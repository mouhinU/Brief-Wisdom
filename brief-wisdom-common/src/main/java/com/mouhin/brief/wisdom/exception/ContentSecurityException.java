package com.mouhin.brief.wisdom.exception;

/**
 * 内容安全异常（输入违规时抛出）
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public class ContentSecurityException extends AIException {

    private static final long serialVersionUID = 1L;

    public ContentSecurityException(String message) {
        super(message);
    }
}
