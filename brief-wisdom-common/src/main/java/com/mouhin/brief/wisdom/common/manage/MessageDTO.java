package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record MessageDTO(
        Long id,
        String sessionId,
        String userId,
        String role,
        String content,
        String model,
        Integer tokens,
        Double cost,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime timestamp,
        String messageType
) implements Serializable {

    public Long getId() { return id; }

    public String getSessionId() { return sessionId; }

    public String getUserId() { return userId; }

    public String getRole() { return role; }

    public String getContent() { return content; }

    public String getModel() { return model; }

    public Integer getTokens() { return tokens; }

    public Double getCost() { return cost; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public String getMessageType() { return messageType; }
}
