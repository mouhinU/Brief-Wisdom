package com.mouhin.brief.wisdom.common.resume;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作经历 DTO
 */
@Data
public class WorkExperienceDTO implements Serializable {
    private Long id;
    private String title;
    private String job;
    private String description;
    private Integer sortOrder;
    private Integer isVisible;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
