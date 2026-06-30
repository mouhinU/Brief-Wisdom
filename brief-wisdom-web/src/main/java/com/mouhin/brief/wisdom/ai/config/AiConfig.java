package com.mouhin.brief.wisdom.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 * <p>
 * AiAgentService 已通过 {@link com.mouhin.brief.wisdom.ai.service.ChatModelRegistry}
 * 实现多提供商路由，不再依赖单一 ChatClient Bean。
 * <p>
 * ChatModel 实例由 ChatModelRegistry 根据 provider 字段动态创建和缓存，
 * 支持 OpenAI 兼容协议（DashScope、OpenAI、DeepSeek）和 Anthropic（Claude）。
 */
/**
 * AiConfig
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiConfig {
    // ChatClient 已移除，AiAgentService 改用 ChatModelRegistry 直接路由
    // 未来可在此添加 ChatMemory、Advisor 等 Bean
}
