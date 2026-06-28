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
@Slf4j
@Service
public class ContentFilterService {

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
     * 检查输入是否包含违规关键词
     *
     * @param message 用户输入
     * @return true 表示违规，应拦截
     */
    public boolean isInputBlocked(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lowerMsg = message.toLowerCase();
        for (String keyword : BLOCKED_KEYWORDS) {
            if (lowerMsg.contains(keyword.toLowerCase())) {
                log.warn("[内容安全] 输入命中敏感关键词，已拦截: keyword={}", keyword);
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤输出内容中的敏感信息
     *
     * @param content AI 回复内容
     * @return 过滤后的内容（若命中严重敏感内容则返回完整替换提示）
     */
    public String filterOutput(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String filtered = content;
        int matchCount = 0;

        for (Pattern pattern : OUTPUT_SENSITIVE_PATTERNS) {
            if (pattern.matcher(filtered).find()) {
                matchCount++;
                filtered = pattern.matcher(filtered).replaceAll(SENSITIVE_REPLACEMENT);
            }
        }

        if (matchCount > 0) {
            log.info("[内容安全] 输出过滤命中 {} 处敏感信息，已替换", matchCount);
        }

        return filtered;
    }

    /**
     * 获取输入被拦截时的提示消息
     */
    public String getBlockedMessage() {
        return "抱歉，您的消息包含不当内容，无法处理。请遵守相关法律法规和社区规范。";
    }
}
