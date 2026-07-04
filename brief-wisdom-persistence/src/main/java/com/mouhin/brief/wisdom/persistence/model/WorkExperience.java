package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作经历实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_experience")
public class WorkExperience extends BaseEntity {

    @TableField(value = "title")
    private String title;

    @TableField(value = "job")
    private String job;

    @TableField(value = "description")
    private String description;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 是否在简历页面显示: 1-显示, 0-隐藏
     */
    @TableField(value = "is_visible")
    private Integer isVisible;
}
