package com.mouhin.brief.wisdom.exception;

/**
 * AI助手模块异常基类
 * <p>
 * 所有AI助手相关的业务异常应继承此类，便于按模块统一捕获和处理。
 */
public class AIException extends BizException {

    private static final long serialVersionUID = 1L;

    public AIException(String message) {
        super(message);
    }

}
