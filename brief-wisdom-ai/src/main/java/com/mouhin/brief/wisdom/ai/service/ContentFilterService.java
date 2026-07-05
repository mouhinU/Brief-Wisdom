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

    private final com.mouhin.brief.wisdom.ai.service.AiAuditService auditService;

    public ContentFilterService(com.mouhin.brief.wisdom.ai.service.AiAuditService auditService) {
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
