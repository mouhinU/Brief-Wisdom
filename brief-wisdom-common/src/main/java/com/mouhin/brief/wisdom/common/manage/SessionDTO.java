package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话信息传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record SessionDTO(
        String sessionId,
        String userId,
        String title,
        String description,
        Integer messageCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updateTime
) implements Serializable {

    public String getSessionId() { return sessionId; }

    public String getUserId() { return userId; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public Integer getMessageCount() { return messageCount; }

    public LocalDateTime getCreateTime() { return createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
}
