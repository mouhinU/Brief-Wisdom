package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话元数据
 */
@Data
public class SessionMetaDTO implements Serializable {
    private String sessionId;
    private String userId;
    private String title;
    private String description;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
