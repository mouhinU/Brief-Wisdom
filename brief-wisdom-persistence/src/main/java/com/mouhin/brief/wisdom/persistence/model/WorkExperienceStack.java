package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作经历技术栈实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_experience_stack")
public class WorkExperienceStack extends BaseEntity {

    @TableField(value = "experience_id")
    private Long experienceId;

    @TableField(value = "tech_name")
    private String techName;

    @TableField(value = "sort_order")
    private Integer sortOrder;
}
