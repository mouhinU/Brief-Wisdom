package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类 - 存储完整的问答历史
 */
/**
 * ChatMessage
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private String sessionId;  // 会话ID

    @TableField(value = "user_id")
    private String userId;  // 用户ID

    @TableField(value = "role")
    private String role;  // user 或 assistant

    @TableField(value = "content")
    private String content;  // 消息内容

    @TableField(value = "model")
    private String model;  // AI模型名称

    @TableField(value = "tokens")
    private Integer tokens;  // token数量

    @TableField(value = "cost")
    private Double cost;  // 费用

    @TableField(value = "timestamp", fill = FieldFill.INSERT)
    private LocalDateTime timestamp;

    @TableField(value = "message_type")
    private String messageType;  // 消息类型: text, image, code等

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;  // 0-未删除, 1-已删除
}
