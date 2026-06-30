package com.mouhin.brief.wisdom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.mouhin.brief.wisdom"},
        exclude = {
                OpenAiChatAutoConfiguration.class,      // OpenAI 由 ChatModelRegistry 手动管理
                AnthropicChatAutoConfiguration.class     // Anthropic 由 ChatModelRegistry 手动管理
        }
)
/**
 * WebApplication
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@MapperScan("com.mouhin.brief.wisdom.persistence.mapper")
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}
