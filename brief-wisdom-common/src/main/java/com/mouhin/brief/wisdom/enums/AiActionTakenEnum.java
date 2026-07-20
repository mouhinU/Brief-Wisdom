package com.mouhin.brief.wisdom.enums;

import lombok.Getter;

/**
 * AI 审计采取的动作枚举
 * <p>
 * 定义 AI 内容安全检测后采取的处理动作，用于记录和追踪安全事件的处理结果。
 */

/**
 * AiActionTakenEnum 枚举
 *
 * @author Brief-Wisdom
 * @date 2026-07-06
 */
@Getter
public enum AiActionTakenEnum {

    /**
     * 拦截：完全阻止请求或响应
     */
    BLOCKED("BLOCKED", "拦截"),

    /**
     * 过滤：替换或移除敏感内容后放行
     */
    FILTERED("FILTERED", "过滤"),

    /**
     * 警告：允许通过但记录警告信息
     */
    WARNED("WARNED", "警告"),

    /**
     * 放行：正常允许通过
     */
    ALLOWED("ALLOWED", "放行");

    /**
     * 动作代码
     */
    private final String code;

    /**
     * 动作描述
     */
    private final String description;

    AiActionTakenEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 动作代码
     * @return 对应的枚举值，未找到返回 null
     */
    public static AiActionTakenEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AiActionTakenEnum action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
