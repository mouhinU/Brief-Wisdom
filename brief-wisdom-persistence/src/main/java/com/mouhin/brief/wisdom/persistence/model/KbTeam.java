package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库-团队关联实体
 * <p>
 * 支持一个知识库属于多个团队（多对多关系）。
 *
 * @author Brief-Wisdom
 * @date 2026-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_team")
public class KbTeam extends BaseEntity {

    /**
     * 知识库ID
     */
    @TableField(value = "kb_id")
    private Long kbId;

    /**
     * 团队ID
     */
    @TableField(value = "team_id")
    private Long teamId;
}
