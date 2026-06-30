package com.mouhin.brief.wisdom.exception;


import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private String code;

    private static final long serialVersionUID = 1L;

    public BizException(String message) {
        super(message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

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
