package com.mouhin.brief.wisdom.common;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果
 */
/**
 * PageResult
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;
    private boolean hasMore;
}
