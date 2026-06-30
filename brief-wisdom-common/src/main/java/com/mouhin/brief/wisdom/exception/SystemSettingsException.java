package com.mouhin.brief.wisdom.exception;

import java.io.Serial;

/**
 * 系统设置模块异常基类
 * <p>
 * 所有系统设置相关的业务异常应继承此类，便于按模块统一捕获和处理。
 */
public class SystemSettingsException extends BizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SystemSettingsException(String message) {
        super(message);
    }

}
