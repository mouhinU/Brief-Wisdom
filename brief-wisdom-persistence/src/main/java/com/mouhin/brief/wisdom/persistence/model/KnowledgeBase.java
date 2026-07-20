package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分类实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    @TableField(value = "name")
    private String name;

    @TableField(value = "description")
    private String description;

    @TableField(value = "icon")
    private String icon;

    @TableField(value = "parent_id")
    private Long parentId;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    @TableField(value = "is_public")
    private Integer isPublic;
}
