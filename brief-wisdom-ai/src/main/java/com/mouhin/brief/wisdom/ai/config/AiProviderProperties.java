package com.mouhin.brief.wisdom.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * AI 多提供商配置属性
 * <p>
 * 绑定 {@code app.ai} 下的配置：
 * <pre>
 * app:
 *   ai:
 *     default-provider: dashscope
 *     providers:
 *       dashscope:
 *         type: openai
 *         api-key: sk-xxx
 *         base-url: https://dashscope.aliyuncs.com/compatible-mode
 *         model: qwen-max
 * </pre>
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    /**
     * 默认提供商名称
     */
    private String defaultProvider = "dashscope";

    /**
     * 所有提供商配置
     */
    private Map<String, ProviderConfig> providers;

    /**
     * 单个提供商配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * 协议类型：openai（OpenAI 兼容协议）或 anthropic
         */
        private String type = "openai";

        /**
         * API Key
         */
        private String apiKey;

        /**
         * API 基础 URL
         */
        private String baseUrl;

        /**
         * 默认模型
         */
        private String model;
    }
}
