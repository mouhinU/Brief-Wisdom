package com.mouhin.brief.wisdom.ai.service.tools;

import com.mouhin.brief.wisdom.ai.service.UrlFetchService;
import com.mouhin.brief.wisdom.common.knowledge.UrlMetadataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 网页内容抓取工具
 * <p>
 * 抓取指定 URL 的网页元数据（标题、描述），供 AI 助手分析网页内容。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebFetchTool {

    private final UrlFetchService urlFetchService;

    /**
     * 抓取指定 URL 的网页内容摘要
     *
     * @param url 要抓取的网页 URL
     * @return 网页标题、描述等元数据
     */
    @Tool(description = "抓取指定 URL 的网页内容摘要。当用户提供一个链接并希望分析其内容、总结网页信息时调用。")
    public String fetchWebPage(
            @ToolParam(description = "要抓取的网页 URL，必须是完整的 http/https 地址") String url) {

        log.info("[Tool] fetchWebPage 被调用: url={}", url);

        if (url == null || url.isBlank()) {
            return "URL 不能为空。";
        }

        // 简单的 URL 格式校验
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            UrlMetadataDTO metadata = urlFetchService.fetchUrlMetadata(url);
            if (metadata == null) {
                return "无法获取 URL 内容: " + url;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("网页信息：\n");
            sb.append("URL: ").append(metadata.getUrl()).append("\n");
            if (metadata.getTitle() != null && !metadata.getTitle().isBlank()) {
                sb.append("标题: ").append(metadata.getTitle()).append("\n");
            }
            if (metadata.getDescription() != null && !metadata.getDescription().isBlank()) {
                sb.append("描述: ").append(metadata.getDescription()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] fetchWebPage 执行失败: url={}, error={}", url, e.getMessage(), e);
            return "抓取网页失败: " + e.getMessage();
        }
    }
}
