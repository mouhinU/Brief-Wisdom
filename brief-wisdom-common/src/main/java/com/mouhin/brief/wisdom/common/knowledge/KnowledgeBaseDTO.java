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

    /**
     * 归属类型: personal-个人, team-团队
     */
    private String ownerType;

    /**
     * 归属ID（用户ID或团队ID）
     */
    private String ownerId;

    /**
     * 创建者用户ID
     */
    private String creatorId;

    /**
     * 关联的团队ID列表（多团队归属，为空表示个人知识库）
     */
    private List<Long> teamIds;

    /**
     * 关联的团队名称列表（与 teamIds 对应，用于展示）
     */
    private List<String> teamNames;

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
