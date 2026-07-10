package com.mouhin.brief.wisdom.knowledge.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除文档请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-09
 */
@Data
public class DocumentBatchDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待删除的文档 ID 列表
     */
    private List<Long> ids;
}
