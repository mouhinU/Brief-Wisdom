package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.ai.config.AiProviderProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多提供商 ChatModel 路由注册表
 * <p>
 * 根据 AiModel 的 provider 字段路由到对应的 ChatModel 实例。
 * 支持 OpenAI 兼容协议（DashScope、OpenAI、DeepSeek 等）和 Anthropic（Claude）。
 * <p>
 * 所有提供商统一在 {@code app.ai.providers} 中定义，
 * 默认提供商通过 {@code app.ai.default-provider} 指定。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
public class ChatModelRegistry {

    private final AiProviderProperties properties;

    /**
     * ChatModel 缓存：provider → ChatModel
     */
    private final Map<String, ChatModel> modelCache = new ConcurrentHashMap<>();

    public ChatModelRegistry(AiProviderProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        Map<String, AiProviderProperties.ProviderConfig> providers = properties.getProviders();
        if (providers == null || providers.isEmpty()) {
            log.warn("[AI] 没有配置任何提供商，请检查 app.ai.providers 配置");
            return;
        }
        providers.forEach(this::registerProvider);
        if (modelCache.isEmpty()) {
            log.warn("[AI] 没有成功注册任何提供商，请检查 api-key 配置");
        } else {
            log.info("[AI] ChatModel 注册表初始化完成，已注册 {} 个提供商: {}，默认: {}",
                    modelCache.size(), modelCache.keySet(), properties.getDefaultProvider());
        }
    }

    /**
     * 根据提供商名称获取对应的 ChatModel
     *
     * @param provider 提供商标识（dashscope / openai / anthropic / deepseek 等）
     * @return ChatModel 实例，找不到时返回默认提供商
     */
    public ChatModel getChatModel(String provider) {
        String defaultProvider = properties.getDefaultProvider();
        if (provider == null || provider.isBlank()) {
            provider = defaultProvider;
        }
        ChatModel chatModel = modelCache.get(provider);
        if (chatModel == null) {
            log.warn("[AI] 未找到提供商 {} 的 ChatModel，使用默认提供商 {}", provider, defaultProvider);
            chatModel = modelCache.get(defaultProvider);
        }
        return chatModel;
    }

    /**
     * 获取默认 ChatModel
     */
    public ChatModel getDefaultChatModel() {
        return modelCache.get(properties.getDefaultProvider());
    }

    /**
     * 获取所有已注册的提供商名称
     */
    public Set<String> getRegisteredProviders() {
        return Collections.unmodifiableSet(modelCache.keySet());
    }

    /**
     * 注册提供商
     */
    private void registerProvider(String name, AiProviderProperties.ProviderConfig props) {
        try {
            String type = props.getType() != null ? props.getType() : "openai";
            String apiKey = props.getApiKey();
            String baseUrl = props.getBaseUrl();

            if (apiKey == null || apiKey.isBlank()) {
                log.warn("[AI] 提供商 {} 未配置 api-key，跳过", name);
                return;
            }

            ChatModel chatModel;
            if ("anthropic".equals(type)) {
                chatModel = createAnthropicChatModel(baseUrl, apiKey, props.getModel());
            } else {
                chatModel = createOpenAiCompatibleChatModel(baseUrl, apiKey, props.getModel());
            }

            modelCache.put(name, chatModel);
            log.info("[AI] 注册提供商: {} (type: {}, base-url: {})", name, type, baseUrl);
        } catch (Exception e) {
            log.error("[AI] 注册提供商 {} 失败: {}", name, e.getMessage(), e);
        }
    }

    /**
     * 创建 OpenAI 兼容协议的 ChatModel（适用于 OpenAI、DeepSeek、Ollama 等）
     */
    private OpenAiChatModel createOpenAiCompatibleChatModel(String baseUrl, String apiKey, String model) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model != null ? model : "gpt-4o")
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    /**
     * 创建 Anthropic (Claude) 的 ChatModel
     */
    private AnthropicChatModel createAnthropicChatModel(String baseUrl, String apiKey, String model) {
        AnthropicApi.Builder apiBuilder = AnthropicApi.builder()
                .apiKey(apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            apiBuilder.baseUrl(baseUrl);
        }
        AnthropicApi api = apiBuilder.build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model != null ? model : "claude-sonnet-4-20250514")
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }
}
