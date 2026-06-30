package com.mouhin.brief.wisdom.common.resume;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目 DTO
 */
/**
 * ProjectDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class ProjectDTO implements Serializable {
    private Long id;
    private Long experienceId;
    private String name;
    private String lifecycle;
    private String background;
    private String duty;
    private Integer sortOrder;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
