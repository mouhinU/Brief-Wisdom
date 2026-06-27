package com.mouhin.brief.wisdom.common.manage;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话信息 DTO
 */
@Data
public class SessionDTO implements Serializable {
    private String sessionId;
    private String userId;
    private String title;
    private String description;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
