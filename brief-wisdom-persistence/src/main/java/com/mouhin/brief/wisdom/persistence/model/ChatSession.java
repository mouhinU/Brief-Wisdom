package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话实体类 - 关联用户
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

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
}
