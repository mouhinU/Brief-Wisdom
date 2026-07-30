package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天附件实体类 - 存储多模态文件元信息（图片/文档/音频）
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_attachment")
public class ChatAttachment extends BaseEntity {

    @TableField(value = "user_id")
    private String userId;  // 上传者用户ID

    @TableField(value = "session_id")
    private String sessionId;  // 所属会话ID（发送消息时回填）

    @TableField(value = "message_id")
    private Long messageId;  // 所属消息ID（发送消息时回填）

    @TableField(value = "file_name")
    private String fileName;  // 原始文件名

    @TableField(value = "file_key")
    private String fileKey;  // 存储 key

    @TableField(value = "file_url")
    private String fileUrl;  // 可访问 URL

    @TableField(value = "media_type")
    private String mediaType;  // MIME 类型

    @TableField(value = "file_size")
    private Long fileSize;  // 文件字节数

    @TableField(value = "category")
    private String category;  // 附件分类: image-图片, document-文档, audio-音频
}
