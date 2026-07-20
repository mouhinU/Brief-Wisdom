package com.mouhin.brief.wisdom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {"com.mouhin.brief.wisdom"},
        exclude = {
                // OpenAI 全系列自动配置由 ChatModelRegistry / VectorStoreConfig 手动管理
                OpenAiChatAutoConfiguration.class,
                OpenAiEmbeddingAutoConfiguration.class,
                OpenAiAudioSpeechAutoConfiguration.class,
                OpenAiAudioTranscriptionAutoConfiguration.class,
                OpenAiImageAutoConfiguration.class,
                OpenAiModerationAutoConfiguration.class,
                // Anthropic 由 ChatModelRegistry 手动管理
                AnthropicChatAutoConfiguration.class,
                // VectorStore 由 VectorStoreConfig 手动管理（支持降级）
                RedisVectorStoreAutoConfiguration.class
        }
)
/**
 * WebApplication
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@MapperScan("com.mouhin.brief.wisdom.persistence.mapper")
@EnableScheduling
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}
