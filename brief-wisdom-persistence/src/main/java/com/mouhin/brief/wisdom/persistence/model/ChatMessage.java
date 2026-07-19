package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类 - 存储完整的问答历史
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

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

    @TableField(value = "feedback_score")
    private Integer feedbackScore;  // 用户反馈评分: 1-5（1=很差, 5=很好）

    @TableField(value = "feedback_comment")
    private String feedbackComment;  // 用户反馈备注

    @TableField(value = "feedback_time")
    private LocalDateTime feedbackTime;  // 反馈时间
}
