package com.mouhin.brief.wisdom.exception;

/**
 * 简历模块异常基类
 * <p>
 * 所有简历相关的业务异常应继承此类，便于按模块统一捕获和处理。
 */
public class ResumeException extends BizException {

    private static final long serialVersionUID = 1L;

    public ResumeException(String message) {
        super(message);
    }

}
