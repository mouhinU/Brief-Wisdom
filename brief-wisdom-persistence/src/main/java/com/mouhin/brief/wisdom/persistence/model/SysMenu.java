package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统菜单实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

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
}
