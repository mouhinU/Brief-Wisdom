package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话信息 DTO
 */
/**
 * SessionDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class SessionDTO implements Serializable {
    private String sessionId;
    private String userId;
    private String title;
    private String description;
    private Integer messageCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
