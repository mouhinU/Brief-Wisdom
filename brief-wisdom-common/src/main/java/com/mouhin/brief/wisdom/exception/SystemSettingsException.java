package com.mouhin.brief.wisdom.exception;

import com.mouhin.brief.wisdom.enums.BizExceptionEnums;

import java.io.Serial;

/**
 * 系统设置模块异常基类
 * <p>
 * 所有系统设置相关的业务异常应继承此类，便于按模块统一捕获和处理。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public class SystemSettingsException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用错误消息创建系统设置模块异常
     *
     * @param message 错误消息
     */
    public SystemSettingsException(String message) {
        super(message);
    }

    /**
     * 使用异常枚举创建系统设置模块异常
     *
     * @param exceptionEnum 业务异常枚举
     */
    public SystemSettingsException(BizExceptionEnums exceptionEnum) {
        super(exceptionEnum);
    }

    /**
     * 使用异常枚举和自定义消息创建系统设置模块异常
     *
     * @param exceptionEnum 业务异常枚举
     * @param message       自定义错误消息
     */
    public SystemSettingsException(BizExceptionEnums exceptionEnum, String message) {
        super(exceptionEnum, message);
    }
}
