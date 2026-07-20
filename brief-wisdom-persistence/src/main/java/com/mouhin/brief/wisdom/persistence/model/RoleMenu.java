package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-菜单关联实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("sys_role_menu")
public class RoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "role_id")
    private Long roleId;

    @TableField(value = "menu_id")
    private Long menuId;
}
