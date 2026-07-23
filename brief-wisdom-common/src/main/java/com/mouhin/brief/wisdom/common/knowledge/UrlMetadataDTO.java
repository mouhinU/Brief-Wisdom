package com.mouhin.brief.wisdom.common.knowledge;

import java.io.Serializable;

/**
 * URL 元数据传输对象
 * <p>
 * 用于返回从 URL 抓取的网页元数据（标题、描述等）
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record UrlMetadataDTO(
        String title,
        String description,
        String url
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getUrl() { return url; }
}
