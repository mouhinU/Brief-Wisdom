package com.mouhin.brief.wisdom.common.resume;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 技术栈 DTO
 */
@Data
public class WorkExperienceStackDTO implements Serializable {
    private Long id;
    private Long experienceId;
    private String techName;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
