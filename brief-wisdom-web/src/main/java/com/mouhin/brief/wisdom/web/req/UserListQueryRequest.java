package com.mouhin.brief.wisdom.web.req;

import com.mouhin.brief.wisdom.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户列表分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQueryRequest extends PageRequest {

    /** 用户级别筛选（可选） */
    private String level;

    /** 关键词搜索（可选） */
    private String keyword;
}