package com.mouhin.brief.wisdom.common.resume;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目成果 DTO
 */

/**
 * ProjectAchievementDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class ProjectAchievementDTO implements Serializable {
    private Long id;
    private Long projectId;
    private String content;
    private Integer sortOrder;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
