package com.mouhin.brief.wisdom.common.resume;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目 DTO
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
