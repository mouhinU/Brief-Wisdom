package com.mouhin.brief.wisdom.common.ai;

import java.io.Serializable;

/**
 * 聊天附件传输对象
 * <p>
 * 用于文件上传响应，以及历史消息中附件信息的回填渲染。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public record AttachmentDTO(
        Long id,
        String fileName,
        String fileUrl,
        String mediaType,
        Long fileSize,
        String category
) implements Serializable {

    public Long getId() { return id; }

    public String getFileName() { return fileName; }

    public String getFileUrl() { return fileUrl; }

    public String getMediaType() { return mediaType; }

    public Long getFileSize() { return fileSize; }

    public String getCategory() { return category; }
}
