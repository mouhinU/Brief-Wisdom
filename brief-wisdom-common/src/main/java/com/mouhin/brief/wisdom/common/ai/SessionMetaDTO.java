package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话元数据
 */

/**
 * SessionMetaDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class SessionMetaDTO implements Serializable {
    private String sessionId;
    private String userId;
    private String title;
    private String description;
    private String pageContext;  // 页面上下文
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
