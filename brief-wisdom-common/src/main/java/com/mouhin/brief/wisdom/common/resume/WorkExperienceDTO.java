package com.mouhin.brief.wisdom.common.resume;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作经历 DTO
 */
/**
 * WorkExperienceDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class WorkExperienceDTO implements Serializable {
    private Long id;
    private String title;
    private String job;
    private String description;
    private Integer sortOrder;
    private Integer isVisible;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
