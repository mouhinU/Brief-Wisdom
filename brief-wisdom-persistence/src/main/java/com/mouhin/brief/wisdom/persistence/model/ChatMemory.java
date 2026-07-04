package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话记忆实体 —— 存储 AI 对话中提取的用户偏好和关键信息
 * <p>
 * 用于跨会话记忆，让 AI 在后续对话中能记住用户的重要信息。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_memory")
public class ChatMemory extends BaseEntity {

    /** 用户ID */
    @TableField(value = "user_id")
    private String userId;

    /** 记忆分类：preference-偏好, fact-事实, context-上下文 */
    @TableField(value = "category")
    private String category;

    /** 记忆键（如：preferred_language, tech_stack） */
    @TableField(value = "memory_key")
    private String memoryKey;

    /** 记忆值 */
    @TableField(value = "memory_value")
    private String memoryValue;

    /** 来源会话ID */
    @TableField(value = "source_session_id")
    private String sourceSessionId;

    /** 访问次数（用于权重排序） */
    @TableField(value = "access_count")
    private Integer accessCount = 0;
}
