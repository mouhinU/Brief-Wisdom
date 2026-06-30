package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库分类实体类
 */
/**
 * KnowledgeBase
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
