package com.mouhin.brief.wisdom.ai.req;

import com.mouhin.brief.wisdom.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息历史分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageHistoryQueryRequest extends PageRequest {
    // 可扩展其他筛选条件
}
