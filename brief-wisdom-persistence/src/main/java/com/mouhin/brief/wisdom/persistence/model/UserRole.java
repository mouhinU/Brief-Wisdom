package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联实体类
 */
/**
 * UserRole
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("sys_user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "role_id")
    private Long roleId;
}
