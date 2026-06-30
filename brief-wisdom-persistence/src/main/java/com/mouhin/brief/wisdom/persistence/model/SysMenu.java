package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统菜单实体类
 */
/**
 * SysMenu
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "parent_id")
    private Long parentId;

    @TableField(value = "name")
    private String name;

    @TableField(value = "url")
    private String url;

    @TableField(value = "icon")
    private String icon;

    @TableField(value = "target")
    private String target;

    @TableField(value = "type")
    private Integer type;

    @TableField(value = "permission")
    private String permission;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    @TableField(value = "is_visible")
    private Integer isVisible;

    @TableField(value = "require_login")
    private Integer requireLogin;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
