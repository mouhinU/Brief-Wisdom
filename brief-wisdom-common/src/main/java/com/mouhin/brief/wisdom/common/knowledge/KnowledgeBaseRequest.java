package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

/**
 * 知识库创建/更新请求
 */
@Data
public class KnowledgeBaseRequest {

    private String name;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Integer isPublic;
}
