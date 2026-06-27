package com.mouhin.brief.wisdom.common.manage;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息 DTO
 */
@Data
public class MessageDTO implements Serializable {
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
