package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库 DTO
 */
/**
 * KnowledgeBaseDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class KnowledgeBaseDTO {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Integer isPublic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 文档数量（非数据库字段，用于展示）
     */
    private Long documentCount;

    /**
     * 是否有子知识库（用于前端懒加载展开）
     */
    private Boolean hasChildren;

    /**
     * 子知识库列表（树形结构用）
     */
    private List<KnowledgeBaseDTO> children;
}
