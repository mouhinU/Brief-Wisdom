package com.mouhin.brief.wisdom.exception;

import com.mouhin.brief.wisdom.enums.BizExceptionEnums;

/**
 * AI助手模块异常基类
 * <p>
 * 所有AI助手相关的业务异常应继承此类，便于按模块统一捕获和处理。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public sealed class AIException extends BizException permits ContentSecurityException, RateLimitException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用错误消息创建 AI 模块异常
     *
     * @param message 错误消息
     */
    public AIException(String message) {
        super(message);
    }

    /**
     * 使用错误码和错误消息创建 AI 模块异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public AIException(String code, String message) {
        super(code, message);
    }

    /**
     * 使用异常枚举创建 AI 模块异常
     *
     * @param exceptionEnum 业务异常枚举
     */
    public AIException(BizExceptionEnums exceptionEnum) {
        super(exceptionEnum);
    }

    /**
     * 使用异常枚举和自定义消息创建 AI 模块异常
     *
     * @param exceptionEnum 业务异常枚举
     * @param message       自定义错误消息
     */
    public AIException(BizExceptionEnums exceptionEnum, String message) {
        super(exceptionEnum, message);
    }
}
