package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作经历实体类
 */
@Data
@TableName("work_experience")
public class WorkExperience implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "title")
    private String title;

    @TableField(value = "job")
    private String job;

    @TableField(value = "description")
    private String description;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    /** 是否在简历页面显示: 1-显示, 0-隐藏 */
    @TableField(value = "is_visible")
    private Integer isVisible;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
