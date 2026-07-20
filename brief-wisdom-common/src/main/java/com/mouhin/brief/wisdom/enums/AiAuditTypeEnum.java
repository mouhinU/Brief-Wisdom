package com.mouhin.brief.wisdom.enums;

import lombok.Getter;

/**
 * AI 审计类型枚举
 * <p>
 * 定义 AI 对话过程中的审计日志类型，用于追踪和记录内容安全相关事件。
 */

/**
 * AiAuditTypeEnum 枚举
 *
 * @author Brief-Wisdom
 * @date 2026-07-06
 */
@Getter
public enum AiAuditTypeEnum {

    /**
     * 输入拦截：用户输入被安全过滤器拦截
     */
    INPUT_BLOCKED("INPUT_BLOCKED", "输入拦截"),

    /**
     * 输出过滤：AI 回复中的敏感内容被过滤
     */
    OUTPUT_FILTERED("OUTPUT_FILTERED", "输出过滤"),

    /**
     * 风险检测：检测到潜在的安全风险
     */
    RISK_DETECTED("RISK_DETECTED", "风险检测");

    /**
     * 审计类型代码
     */
    private final String code;

    /**
     * 审计类型描述
     */
    private final String description;

    AiAuditTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 审计类型代码
     * @return 对应的枚举值，未找到返回 null
     */
    public static AiAuditTypeEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AiAuditTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
