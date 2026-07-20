package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识文档实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @TableField(value = "base_id")
    private Long baseId;

    @TableField(value = "title")
    private String title;

    /**
     * 文档类型: INTERNAL-内部文档, FILE-文件, LINK-外部链接
     */
    @TableField(value = "doc_type")
    private String docType;

    /**
     * 文档内容（INTERNAL类型使用，富文本HTML）
     */
    @TableField(value = "content")
    private String content;

    /**
     * 文件URL（FILE类型使用）
     */
    @TableField(value = "file_url")
    private String fileUrl;

    @TableField(value = "file_name")
    private String fileName;

    @TableField(value = "file_size")
    private Long fileSize;

    @TableField(value = "file_type")
    private String fileType;

    /**
     * 外部链接URL（LINK类型使用）
     */
    @TableField(value = "link_url")
    private String linkUrl;

    @TableField(value = "link_desc")
    private String linkDesc;

    @TableField(value = "tags")
    private String tags;

    @TableField(value = "view_count")
    private Integer viewCount;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 状态: 0-草稿, 1-已发布, 2-已归档
     */
    @TableField(value = "status")
    private Integer status;
}
