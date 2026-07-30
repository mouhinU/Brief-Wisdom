package com.mouhin.brief.wisdom.ai.storage;

import java.io.Serializable;

/**
 * 存储对象元信息
 * <p>
 * 文件上传成功后返回，记录存储 key 与可访问 URL，供附件登记与后续读取使用。
 *
 * @param key         存储 key（唯一标识，用于读取/删除）
 * @param url         可访问 URL
 * @param size        文件字节数
 * @param contentType MIME 类型
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public record StoredObject(
        String key,
        String url,
        long size,
        String contentType
) implements Serializable {

    public String getKey() { return key; }

    public String getUrl() { return url; }

    public long getSize() { return size; }

    public String getContentType() { return contentType; }
}
