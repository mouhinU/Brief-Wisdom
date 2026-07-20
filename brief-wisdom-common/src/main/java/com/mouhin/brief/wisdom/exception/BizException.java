package com.mouhin.brief.wisdom.exception;


import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import lombok.Getter;

/**
 * BizException
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private String code;

    /**
     * 使用错误消息创建异常
     *
     * @param message 错误消息
     */
    public BizException(String message) {
        super(message);
    }

    /**
     * 使用错误码和错误消息创建异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用错误码、错误消息和原因异常创建异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 使用异常枚举创建异常（使用枚举默认消息）
     */
    public BizException(BizExceptionEnums exceptionEnum) {
        super(exceptionEnum.getMessage());
        this.code = exceptionEnum.getCode();
    }

    /**
     * 使用异常枚举创建异常（自定义消息覆盖默认消息）
     */
    public BizException(BizExceptionEnums exceptionEnum, String message) {
        super(message);
        this.code = exceptionEnum.getCode();
    }

}
