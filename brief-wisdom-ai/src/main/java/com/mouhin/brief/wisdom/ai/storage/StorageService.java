package com.mouhin.brief.wisdom.ai.storage;

import java.io.InputStream;

/**
 * 对象存储抽象接口
 * <p>
 * 统一文件上传、读取、删除与 URL 解析能力，屏蔽底层存储差异。
 * 通过 {@code app.storage.type} 在本地磁盘（local）与 MinIO/S3 兼容存储（minio）间切换，
 * 与项目实时同步通道（sse/websocket）的可插拔设计保持一致。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param input       文件输入流
     * @param fileName    原始文件名（用于推断扩展名）
     * @param contentType MIME 类型
     * @param size        文件字节数
     * @return 存储对象元信息（含存储 key 与可访问 URL）
     */
    StoredObject upload(InputStream input, String fileName, String contentType, long size);

    /**
     * 按存储 key 读取文件字节（用于构造多模态消息发给模型）
     *
     * @param key 存储 key
     * @return 文件字节数组
     */
    byte[] readBytes(String key);

    /**
     * 解析存储 key 对应的可访问 URL
     *
     * @param key 存储 key
     * @return 可访问 URL（本地为静态资源路径，对象存储为预签名或公开 URL）
     */
    String resolveUrl(String key);

    /**
     * 按存储 key 删除文件
     *
     * @param key 存储 key
     */
    void delete(String key);
}
