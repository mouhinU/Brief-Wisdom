package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 团队实体
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("team")
public class Team extends BaseEntity {

    /**
     * 团队名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 团队描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 团队头像/图标
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 拥有者用户ID
     */
    @TableField(value = "owner_id")
    private String ownerId;
}
