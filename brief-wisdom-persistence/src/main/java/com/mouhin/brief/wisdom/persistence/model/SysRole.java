package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableField(value = "role_name")
    private String roleName;

    @TableField(value = "role_key")
    private String roleKey;

    @TableField(value = "description")
    private String description;

    @TableField(value = "status")
    private Integer status;
}
