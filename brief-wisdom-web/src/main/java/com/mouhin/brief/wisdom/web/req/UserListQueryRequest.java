package com.mouhin.brief.wisdom.web.req;

import lombok.Data;

/**
 * 用户列表分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
public class UserListQueryRequest {

    /** 当前页码，默认1 */
    private int page = 1;

    /** 每页大小，默认20 */
    private int size = 20;

    /** 用户级别筛选（可选） */
    private String level;

    /** 关键词搜索（可选） */
    private String keyword;
}