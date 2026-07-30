package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识库业务数据传输对象（Service 层使用）
 *
 * @author Brief-Wisdom
 * @date 2026-07-04
 */
@Data
public class KnowledgeBaseBO {

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
     * 关联的团队ID列表（支持多团队归属，为空表示个人知识库）
     */
    private List<Long> teamIds;
}
