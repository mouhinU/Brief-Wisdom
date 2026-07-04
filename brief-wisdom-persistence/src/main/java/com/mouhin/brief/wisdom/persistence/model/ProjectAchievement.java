package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成果实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_achievement")
public class ProjectAchievement extends BaseEntity {

    @TableField(value = "project_id")
    private Long projectId;

    @TableField(value = "content")
    private String content;

    @TableField(value = "sort_order")
    private Integer sortOrder;
}
