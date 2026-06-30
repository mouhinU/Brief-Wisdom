package com.mouhin.brief.wisdom.exception;

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

    public ResumeManageException(String message) {
        super(message);
    }

}
