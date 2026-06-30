package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体类 - 关联用户
 */
/**
 * ChatSession
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;  // 自增主键

    @TableField(value = "session_id")
    private String sessionId;  // 会话ID (UUID)

    @TableField(value = "user_id")
    private String userId;  // 关联用户ID

    @TableField(value = "title")
    private String title;

    @TableField(value = "description")
    private String description;  // 会话描述

    @TableField(value = "page_context")
    private String pageContext;  // 页面上下文（如 /about.html, /resume-manage.html）

    @TableField(value = "message_count")
    private Integer messageCount = 0;  // 消息数量

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;  // 0-未删除, 1-已删除
}
