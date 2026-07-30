package com.mouhin.brief.wisdom.ai.storage;

/**
 * 存储操作异常
 * <p>
 * 文件上传、读取、删除等存储操作失败时抛出，携带明确的业务错误信息。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
