package com.mouhin.brief.wisdom.knowledge.req;

import lombok.Data;

/**
 * 知识库文档列表查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
public class DocumentListQueryRequest {

    /** 知识库ID */
    private Long baseId;

    /** 文档类型筛选（可选） */
    private String docType;

    /** 当前页码，默认1 */
    private int page = 1;

    /** 每页大小，默认20 */
    private int size = 20;
}