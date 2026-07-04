package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

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
}
