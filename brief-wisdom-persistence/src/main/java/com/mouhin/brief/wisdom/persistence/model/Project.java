package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目实体类
 */
/**
 * Project
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("project")
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
