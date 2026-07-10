package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.knowledge.UrlMetadataDTO;

/**
 * URL 元数据抓取服务接口
 * <p>
 * 用于从外部 URL 抓取网页元数据（标题、描述等），
 * 支持知识库外部链接文档的自动描述生成。
 *
 * @author Brief-Wisdom
 * @date 2026-07-09
 */
public interface UrlFetchService {

    /**
     * 抓取指定 URL 的网页元数据
     *
     * @param url 目标网页 URL
     * @return URL 元数据（标题、描述）
     */
    UrlMetadataDTO fetchUrlMetadata(String url);
}
