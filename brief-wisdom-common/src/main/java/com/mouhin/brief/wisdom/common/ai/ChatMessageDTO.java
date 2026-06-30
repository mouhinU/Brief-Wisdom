package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息 DTO
 */
/**
 * ChatMessageDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class ChatMessageDTO implements Serializable {
    private Long id;
    private String sessionId;
    private String userId;
    private String role;
    private String content;
    private String model;
    private Integer tokens;
    private Double cost;
    private LocalDateTime timestamp;
    private String messageType;
}
