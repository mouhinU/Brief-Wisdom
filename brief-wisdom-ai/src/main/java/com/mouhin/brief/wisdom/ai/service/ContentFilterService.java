package com.mouhin.brief.wisdom.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容安全过滤服务
 * <p>
 * 对 AI 输入/输出进行敏感内容检测，作为系统提示词之外的第二道防线。
 * <ul>
 *   <li>输入预过滤：拦截明显违规的请求，避免消耗模型 token</li>
 *   <li>输出过滤：检测模型回复中的敏感内容，替换为安全提示</li>
 * </ul>
 */

/**
 * ContentFilterService
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
public class ContentFilterService {

    private final AiAuditService auditService;

    public ContentFilterService(AiAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 输入敏感关键词列表（命中即拦截，不调用模型）
     * <p>
     * 可根据业务需要扩展，建议后续迁移到数据库配置，支持动态更新。
     */
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            // 违法类
            "制作炸弹", "制造毒品", "购买枪支", "黑客攻击教程",
            "破解密码", "盗取账号", "洗钱方法", "制毒配方",
            // 有害类
            "自杀方法", "自残教程", "如何杀人", "下毒方法"
    );

    /**
     * Prompt 注入攻击模式（正则表达式）
     * <p>
     * 用于检测用户试图绕过系统指令的攻击行为。
     */
    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
            // 忽略/覆盖指令
            Pattern.compile("(?i)(ignore|忽略| disregard|忘记)\\s+(all|所有|之前的|前面的)\\s*(instructions|指令|规则|限制|prompt|提示词)"),
            Pattern.compile("(?i)(forget|忘记)\\s+(everything|所有事|之前的事|一切)"),
            Pattern.compile("(?i)(override|覆盖|bypass|绕过)\\s+(system|系统|security|安全)"),

            // 角色扮演越狱
            Pattern.compile("(?i)(act\\s+as|扮演|假装|假装是|你现在是)\\s+(admin|管理员|developer|开发者|root|上帝|神|unrestricted|无限制的)"),
            Pattern.compile("(?i)(you\\s+are\\s+now|你现在是|从现在开始)\\s+(free|自由|unrestricted|不受限制)"),

            // 系统指令泄露
            Pattern.compile("(?i)(show|显示|reveal|揭示|print|打印|output|输出)\\s+(your|你的|the|这个)\\s*(system\\s*prompt|系统提示|initial\\s*instructions|初始指令|rules|规则)"),
            Pattern.compile("(?i)(what\\s*are\\s*your|你的.*是什么|列出.*规则)\\s*(rules|guidelines|instructions|限制|约束)"),

            // 分隔符攻击
            Pattern.compile("[-=*]{10,}"),  // 长分隔符可能用于分割上下文

            // 特殊标记攻击
            Pattern.compile("(?i)<\\|im\\s*end\\|>|<\\|endoftext\\|>"),  // LLM 特殊标记

            // Base64 编码绕过
            Pattern.compile("(?i)(decode|解码|decrypt|解密)\\s+(base64|this|这段)"),

            // 多语言混淆
            Pattern.compile("(?i)(translate|翻译)\\s+(to|为)\\s+(english|英文|chinese|中文).*(and\\s*then|然后).*(execute|执行|follow|遵循)")
    );

    /**
     * 输出敏感词模式（检测 AI 回复中不应出现的内容）
     * <p>
     * 使用正则匹配，命中后替换为安全提示。
     */
    private static final List<Pattern> OUTPUT_SENSITIVE_PATTERNS = List.of(
            // 身份证号模式（18位）
            Pattern.compile("\\d{17}[\\dXx]"),
            // 手机号模式（11位）
            Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"),
            // 银行卡号模式（16-19位数字）
            Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)")
    );

    /**
     * 输出命中时的替换提示
     */
    private static final String SENSITIVE_REPLACEMENT = "[敏感信息已过滤]";

    /**
     * 安全提示消息（当输出被过滤时返回给用户）
     */
    private static final String FILTERED_RESPONSE =
            "抱歉，回复中包含敏感信息，已被安全过滤。请调整您的问题后重试。";

    /**
     * 检查输入是否包含违规关键词（仅检测，不记录日志）
     *
     * @param message 用户输入
     * @return 命中的敏感词，null 表示未命中
     */
    public String checkInputBlocked(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String lowerMsg = message.toLowerCase();
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lowerMsg.contains(keyword.toLowerCase())) {
                log.warn("[内容安全] 输入命中敏感关键词: keyword={}", keyword);
                return keyword;
            }
        }
        return null;
    }

    /**
     * 检测 Prompt 注入攻击
     * <p>
     * 通过正则匹配和启发式规则识别常见的 Prompt 注入攻击模式。
     *
     * @param message 用户输入
     * @return 如果检测到攻击，返回攻击类型描述；否则返回 null
     */
    public String detectPromptInjection(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        // 1. 正则模式匹配
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                String attackType = classifyAttackType(pattern.pattern());
                log.warn("[Prompt注入防护] 检测到攻击模式: type={}, pattern={}", attackType, pattern.pattern());
                return attackType;
            }
        }

        // 2. 启发式规则检测
        String heuristicResult = detectByHeuristics(message);
        if (heuristicResult != null) {
            log.warn("[Prompt注入防护] 启发式检测命中: type={}", heuristicResult);
            return heuristicResult;
        }

        return null;
    }

    /**
     * 根据正则模式分类攻击类型
     */
    private String classifyAttackType(String pattern) {
        if (pattern.contains("ignore") || pattern.contains("忽略") || pattern.contains("forget")) {
            return "指令覆盖攻击";
        } else if (pattern.contains("act as") || pattern.contains("扮演") || pattern.contains("你现在是")) {
            return "角色扮演越狱";
        } else if (pattern.contains("system prompt") || pattern.contains("系统提示")) {
            return "系统指令泄露";
        } else if (pattern.contains("[-=*]")) {
            return "分隔符攻击";
        } else if (pattern.contains("base64")) {
            return "编码绕过攻击";
        }
        return "未知攻击类型";
    }

    /**
     * 启发式规则检测
     * <p>
     * 检测一些难以用正则表达的复杂攻击模式。
     */
    private String detectByHeuristics(String message) {
        String lowerMsg = message.toLowerCase();

        // 检测过长的重复指令（可能用于淹没系统提示）
        if (message.length() > 5000) {
            long repeatCount = message.chars().distinct().count();
            double diversityRatio = (double) repeatCount / message.length();
            if (diversityRatio < 0.1) {  // 字符多样性极低，可能是重复攻击
                return "文本淹没攻击";
            }
        }

        // 检测混合语言指令混淆
        int chineseCount = 0;
        int englishCount = 0;
        for (char c : message.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                englishCount++;
            }
        }
        // 中英文混合且包含敏感动词，可能是混淆攻击
        if (chineseCount > 20 && englishCount > 20) {
            if (lowerMsg.contains("ignore") || lowerMsg.contains("忽略") ||
                    lowerMsg.contains("override") || lowerMsg.contains("覆盖")) {
                return "多语言混淆攻击";
            }
        }

        // 检测嵌套指令结构（如："请忽略之前的指令，然后执行：忽略之前的指令...")
        int ignoreCount = 0;
        if (lowerMsg.contains("ignore") || lowerMsg.contains("忽略")) {
            ignoreCount = countOccurrences(lowerMsg, "ignore") + countOccurrences(lowerMsg, "忽略");
        }
        if (ignoreCount >= 3) {
            return "嵌套指令攻击";
        }

        return null;
    }

    /**
     * 计算子串出现次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * 过滤输出内容中的敏感信息
     *
     * @param content   AI 回复内容
     * @param sessionId 会话ID（用于审计日志）
     * @param userId    用户ID（用于审计日志）
     * @param messageId 消息ID（用于审计日志）
     * @return 过滤后的内容（若命中严重敏感内容则返回完整替换提示）
     */
    public String filterOutput(String content, String sessionId, String userId, Long messageId) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String filtered = content;
        int matchCount = 0;
        String matchedPattern = null;

        for (Pattern pattern : OUTPUT_SENSITIVE_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(filtered);
            if (matcher.find()) {
                matchCount++;
                matchedPattern = pattern.pattern();
                filtered = matcher.replaceAll(SENSITIVE_REPLACEMENT);
            }
        }

        if (matchCount > 0) {
            log.info("[内容安全] 输出过滤命中 {} 处敏感信息，已替换", matchCount);
            // 记录审计日志
            auditService.logOutputFiltered(sessionId, userId, messageId, matchedPattern,
                    maskSensitiveInfo(content), filtered);
        }

        return filtered;
    }

    /**
     * 获取输入被拦截时的提示消息
     */
    public String getBlockedMessage() {
        return "抱歉，您的消息包含不当内容，无法处理。请遵守相关法律法规和社区规范。";
    }

    /**
     * 记录审计日志（供 AiAgentService 调用）
     */
    public void logOutputFiltered(String sessionId, String userId, Long messageId, String triggerPattern,
                                  String originalContent, String filteredContent) {
        auditService.logOutputFiltered(sessionId, userId, messageId, triggerPattern, originalContent, filteredContent);
    }

    /**
     * 记录输入拦截审计日志
     */
    public void logInputBlocked(String sessionId, String userId, String keyword, String originalContent) {
        auditService.logInputBlocked(sessionId, userId, keyword, originalContent);
    }

    /**
     * 对敏感信息进行脱敏处理（用于审计日志存储）
     *
     * @param content 原始内容
     * @return 脱敏后的内容
     */
    private String maskSensitiveInfo(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        // 简单脱敏：只显示前50个字符，其余用省略号代替
        if (content.length() > 50) {
            return content.substring(0, 50) + "...[已脱敏]";
        }
        return content;
    }
}
