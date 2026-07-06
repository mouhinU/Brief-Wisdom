package com.mouhin.brief.wisdom.enums;

import lombok.Getter;

/**
 * AI 风险等级枚举
 * <p>
 * 定义 AI 内容安全检测的风险等级，用于评估和分级处理安全事件。
 */
/**
 * AiRiskLevelEnum 枚举
 *
 * @author Brief-Wisdom
 * @date 2026-07-06
 */
@Getter
public enum AiRiskLevelEnum {

    /**
     * 低风险：轻微违规，需要关注
     */
    LOW("LOW", "低"),

    /**
     * 中风险：中等违规，需要及时处理
     */
    MEDIUM("MEDIUM", "中"),

    /**
     * 高风险：严重违规，需要立即处理
     */
    HIGH("HIGH", "高"),

    /**
     * 严重风险：极严重违规，需要紧急处理并告警
     */
    CRITICAL("CRITICAL", "严重");

    /**
     * 风险等级代码
     */
    private final String code;

    /**
     * 风险等级描述
     */
    private final String description;

    AiRiskLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 风险等级代码
     * @return 对应的枚举值，未找到返回 null
     */
    public static AiRiskLevelEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AiRiskLevelEnum level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
