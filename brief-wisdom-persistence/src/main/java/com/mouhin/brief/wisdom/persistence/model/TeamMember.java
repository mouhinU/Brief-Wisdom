package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 团队成员实体
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team_member")
public class TeamMember extends BaseEntity {

    /**
     * 团队ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 角色: owner-拥有者, admin-管理员, member-成员
     */
    @TableField(value = "role")
    private String role;
}
