package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {

    @TableField(value = "experience_id")
    private Long experienceId;

    @TableField(value = "name")
    private String name;

    @TableField(value = "lifecycle")
    private String lifecycle;

    @TableField(value = "background")
    private String background;

    @TableField(value = "duty")
    private String duty;

    @TableField(value = "sort_order")
    private Integer sortOrder;
}
