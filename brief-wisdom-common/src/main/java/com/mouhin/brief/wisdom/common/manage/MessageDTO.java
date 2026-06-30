package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息 DTO
 */
/**
 * MessageDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private String messageType;
}
