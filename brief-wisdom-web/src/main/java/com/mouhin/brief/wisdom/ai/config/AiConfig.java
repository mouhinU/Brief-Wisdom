package com.mouhin.brief.wisdom.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 * <p>
 * ChatModel 由 spring-ai-starter-model-openai 自动配置，
 * 只需手动配置 ChatClient Bean。
 */
@Configuration
public class AiConfig {

    /**
     * 配置 ChatClient Bean（基于自动配置的 OpenAiChatModel）
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
