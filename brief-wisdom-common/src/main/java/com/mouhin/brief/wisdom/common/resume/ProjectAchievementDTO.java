package com.mouhin.brief.wisdom.common.resume;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目成果 DTO
 */
@Data
public class ProjectAchievementDTO implements Serializable {
    private Long id;
    private Long projectId;
    private String content;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
