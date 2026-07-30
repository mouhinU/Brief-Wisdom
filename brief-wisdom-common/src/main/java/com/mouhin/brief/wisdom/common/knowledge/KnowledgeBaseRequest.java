package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识库创建/更新请求
 */

/**
 * KnowledgeBaseRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class KnowledgeBaseRequest {

    private String name;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Integer isPublic;

    /**
     * 关联的团队ID列表（支持多团队归属，为空表示个人知识库）
     */
    private List<Long> teamIds;
}
