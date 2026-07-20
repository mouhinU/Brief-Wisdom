package com.mouhin.brief.wisdom.knowledge.req;

import com.mouhin.brief.wisdom.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档搜索分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentSearchQueryRequest extends PageRequest {

    /**
     * 搜索关键词
     */
    private String keyword;
}
