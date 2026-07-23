package com.mouhin.brief.wisdom.ai.service.impl;

import com.mouhin.brief.wisdom.ai.service.UrlFetchService;
import com.mouhin.brief.wisdom.common.knowledge.UrlMetadataDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL 元数据抓取服务实现
 * <p>
 * 使用 Jsoup 抓取网页内容，提取标题和描述信息。
 * 优先从 meta 标签获取描述，否则从正文提取摘要。
 *
 * @author Brief-Wisdom
 * @date 2026-07-09
 */
@Service
@Slf4j
public class UrlFetchServiceImpl implements UrlFetchService {

    /**
     * 连接超时时间（毫秒）
     */
    private static final int TIMEOUT_MS = 10000;

    /**
     * 描述最大长度
     */
    private static final int MAX_DESC_LENGTH = 500;

    /**
     * 抓取指定 URL 的网页元数据
     */
    @Override
    public UrlMetadataDTO fetchUrlMetadata(String url) {
        if (url == null || url.isBlank()) {
            throw new AIException("URL 不能为空");
        }

        // 校验 URL 格式
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new AIException("URL 格式不正确: " + e.getMessage());
        }

        String title;
        String description;

        try {
            log.info("开始抓取 URL 元数据: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // 提取标题
            title = extractTitle(doc);

            // 提取描述
            description = extractDescription(doc);

            log.info("URL 元数据抓取成功 - title: {}, desc length: {}",
                    title, description != null ? description.length() : 0);

        } catch (IOException e) {
            log.error("抓取 URL 元数据失败: {}", url, e);
            throw new AIException("无法访问该 URL，请检查链接是否正确: " + e.getMessage());
        } catch (Exception e) {
            log.error("抓取 URL 元数据异常: {}", url, e);
            throw new AIException("抓取网页信息时发生异常: " + e.getMessage());
        }

        return new UrlMetadataDTO(title, description, url);
    }

    /**
     * 提取网页标题
     */
    private String extractTitle(Document doc) {
        // 优先从 og:title 获取
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String content = ogTitle.attr("content");
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }

        // 其次从 twitter:title 获取
        Element twitterTitle = doc.selectFirst("meta[name=twitter:title]");
        if (twitterTitle != null) {
            String content = twitterTitle.attr("content");
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }

        // 最后从 title 标签获取
        String title = doc.title();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        return "无标题";
    }

    /**
     * 提取网页描述
     */
    private String extractDescription(Document doc) {
        // 优先从 og:description 获取
        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null) {
            String content = ogDesc.attr("content");
            if (content != null && !content.isBlank()) {
                return truncateDescription(content.trim());
            }
        }

        // 其次从 meta description 获取
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String content = metaDesc.attr("content");
            if (content != null && !content.isBlank()) {
                return truncateDescription(content.trim());
            }
        }

        // 再次从 twitter:description 获取
        Element twitterDesc = doc.selectFirst("meta[name=twitter:description]");
        if (twitterDesc != null) {
            String content = twitterDesc.attr("content");
            if (content != null && !content.isBlank()) {
                return truncateDescription(content.trim());
            }
        }

        // 最后从正文提取摘要
        String bodyText = extractBodyText(doc);
        if (bodyText != null && !bodyText.isBlank()) {
            return truncateDescription(bodyText);
        }

        return "无描述";
    }

    /**
     * 从正文提取文本摘要
     */
    private String extractBodyText(Document doc) {
        // 移除 script 和 style 标签
        doc.select("script, style, nav, header, footer").remove();

        // 优先从 article 或 main 获取
        Element article = doc.selectFirst("article, main");
        if (article != null) {
            String text = article.text();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }

        // 否则从 body 获取
        Element body = doc.body();
        if (body != null) {
            String text = body.text();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }

        return null;
    }

    /**
     * 截断描述文本
     */
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= MAX_DESC_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESC_LENGTH) + "...";
    }
}
