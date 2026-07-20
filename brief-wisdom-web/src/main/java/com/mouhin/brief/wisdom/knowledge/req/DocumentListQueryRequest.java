package com.mouhin.brief.wisdom.knowledge.req;

import com.mouhin.brief.wisdom.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档列表查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentListQueryRequest extends PageRequest {

    /**
     * 知识库ID
     */
    private Long baseId;

    /**
     * 文档类型筛选（可选）
     */
    private String docType;
}