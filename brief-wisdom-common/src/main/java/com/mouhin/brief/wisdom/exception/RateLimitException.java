package com.mouhin.brief.wisdom.exception;

/**
 * 限流异常（请求超频时抛出）
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public class RateLimitException extends AIException {

    private static final long serialVersionUID = 1L;

    public RateLimitException(String message) {
        super(message);
    }
}
