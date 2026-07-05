package com.mouhin.brief.wisdom.exception;

import com.mouhin.brief.wisdom.enums.BizExceptionEnums;

/**
 * 简历数据管理模块异常基类
 * <p>
 * 所有简历数据管理相关的业务异常应继承此类，便于按模块统一捕获和处理。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public class ResumeManageException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用错误消息创建简历管理模块异常
     *
     * @param message 错误消息
     */
    public ResumeManageException(String message) {
        super(message);
    }

    /**
     * 使用异常枚举创建简历管理模块异常
     *
     * @param exceptionEnum 业务异常枚举
     */
    public ResumeManageException(BizExceptionEnums exceptionEnum) {
        super(exceptionEnum);
    }

    /**
     * 使用异常枚举和自定义消息创建简历管理模块异常
     *
     * @param exceptionEnum 业务异常枚举
     * @param message       自定义错误消息
     */
    public ResumeManageException(BizExceptionEnums exceptionEnum, String message) {
        super(exceptionEnum, message);
    }
}
