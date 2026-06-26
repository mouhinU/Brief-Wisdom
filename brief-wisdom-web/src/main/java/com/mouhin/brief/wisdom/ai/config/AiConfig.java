package com.mouhin.brief.wisdom.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 配置 DashScope API
     */
    @Bean
    public DashScopeApi dashScopeApi() {
        return new DashScopeApi(apiKey);
    }

    /**
     * 配置 DashScope ChatModel
     */
    @Bean
    public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {
        return new DashScopeChatModel(dashScopeApi);
    }

    /**
     * 配置 ChatClient Bean
     */
    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.create(dashScopeChatModel);
    }
}
