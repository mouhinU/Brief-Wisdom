package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库共享授权实体
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_share")
public class KbShare extends BaseEntity {

    /**
     * 知识库ID
     */
    @TableField(value = "kb_id")
    private Long kbId;

    /**
     * 授权目标类型: team-团队, user-个人
     */
    @TableField(value = "target_type")
    private String targetType;

    /**
     * 授权目标ID（团队ID或用户ID）
     */
    @TableField(value = "target_id")
    private String targetId;

    /**
     * 权限: read-只读
     */
    @TableField(value = "permission")
    private String permission;

    /**
     * 授权人用户ID
     */
    @TableField(value = "shared_by")
    private String sharedBy;
}
