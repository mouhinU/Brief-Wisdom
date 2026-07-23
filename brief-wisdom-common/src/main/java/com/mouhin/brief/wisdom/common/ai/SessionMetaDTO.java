package com.mouhin.brief.wisdom.common.ai;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话元数据传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record SessionMetaDTO(
        String sessionId,
        String userId,
        String title,
        String description,
        String pageContext,
        Integer messageCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {

    public String getSessionId() { return sessionId; }

    public String getUserId() { return userId; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getPageContext() { return pageContext; }

    public Integer getMessageCount() { return messageCount; }

    public LocalDateTime getCreateTime() { return createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
}
