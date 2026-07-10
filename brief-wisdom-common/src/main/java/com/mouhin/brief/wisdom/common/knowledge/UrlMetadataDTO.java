package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.io.Serializable;

/**
 * URL 元数据传输对象
 * <p>
 * 用于返回从 URL 抓取的网页元数据（标题、描述等）
 *
 * @author Brief-Wisdom
 * @date 2026-07-09
 */
@Data
public class UrlMetadataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 网页标题
     */
    private String title;

    /**
     * 网页描述（摘要）
     */
    private String description;

    /**
     * 原始 URL
     */
    private String url;
}
