package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.ai.config.AiProviderProperties;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientImpl;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.OpenAIClientAsyncImpl;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
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
     * 根据提供商和模型名称获取 ChatModel（支持思考模式）
     *
     * @param provider     提供商标识
     * @param modelName    模型名称
     * @param thinkingMode 思考模式: normal-普通模式, thinking-思考模式
     * @return ChatModel 实例
     */
    public ChatModel getChatModel(String provider, String modelName, String thinkingMode) {
        // 思考模式下使用不同的缓存 key
        String cacheKey = buildCacheKey(provider, modelName, thinkingMode);
        
        ChatModel chatModel = modelCache.get(cacheKey);
        if (chatModel != null) {
            return chatModel;
        }
        
        // 创建带思考模式配置的 ChatModel
        chatModel = createChatModelWithThinkingMode(provider, modelName, thinkingMode);
        if (chatModel != null) {
            modelCache.put(cacheKey, chatModel);
            log.info("[AI] 创建带思考模式的 ChatModel: provider={}, model={}, mode={}", 
                    provider, modelName, thinkingMode);
        }
        
        return chatModel;
    }

    /**
     * 构建缓存 key
     */
    private String buildCacheKey(String provider, String modelName, String thinkingMode) {
        if ("thinking".equals(thinkingMode)) {
            return provider + ":" + modelName + ":thinking";
        }
        return provider + ":" + modelName;
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
        ClientOptions clientOptions = ClientOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .build();
        OpenAIClient client = new OpenAIClientImpl(clientOptions);
        OpenAIClientAsync asyncClient = new OpenAIClientAsyncImpl(clientOptions);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model != null ? model : "gpt-4o")
                .build();
        return OpenAiChatModel.builder()
                .openAiClient(client)
                .openAiClientAsync(asyncClient)
                .options(options)
                .build();
    }

    /**
     * 创建 Anthropic (Claude) 的 ChatModel
     */
    private AnthropicChatModel createAnthropicChatModel(String baseUrl, String apiKey, String model) {
        com.anthropic.core.ClientOptions.Builder clientOptionsBuilder = com.anthropic.core.ClientOptions.builder()
                .putHeader("x-api-key", apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            clientOptionsBuilder.baseUrl(baseUrl);
        }
        com.anthropic.core.ClientOptions clientOptions = clientOptionsBuilder.build();
        AnthropicClient client = new AnthropicClientImpl(clientOptions);

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model != null ? model : "claude-sonnet-4-20250514")
                .build();
        return AnthropicChatModel.builder()
                .anthropicClient(client)
                .options(options)
                .build();
    }

    /**
     * 创建带思考模式配置的 ChatModel
     *
     * @param provider     提供商标识
     * @param modelName    模型名称
     * @param thinkingMode 思考模式: normal-普通模式, thinking-思考模式
     * @return ChatModel 实例
     */
    private ChatModel createChatModelWithThinkingMode(String provider, String modelName, String thinkingMode) {
        AiProviderProperties.ProviderConfig props = properties.getProviders().get(provider);
        if (props == null) {
            log.warn("[AI] 未找到提供商 {} 的配置", provider);
            return null;
        }

        String apiKey = props.getApiKey();
        String baseUrl = props.getBaseUrl();
        
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI] 提供商 {} 未配置 api-key", provider);
            return null;
        }

        // 根据提供商类型创建不同的 ChatModel
        String type = props.getType() != null ? props.getType() : "openai";
        
        if ("anthropic".equals(type)) {
            return createAnthropicChatModelWithThinking(baseUrl, apiKey, modelName, thinkingMode);
        } else {
            return createOpenAiCompatibleChatModelWithThinking(baseUrl, apiKey, modelName, thinkingMode);
        }
    }

    /**
     * 创建 OpenAI 兼容协议的 ChatModel（支持思考模式）
     */
    private OpenAiChatModel createOpenAiCompatibleChatModelWithThinking(
            String baseUrl, String apiKey, String model, String thinkingMode) {
        ClientOptions clientOptions = ClientOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .build();
        OpenAIClient client = new OpenAIClientImpl(clientOptions);
        OpenAIClientAsync asyncClient = new OpenAIClientAsyncImpl(clientOptions);
        
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(model != null ? model : "gpt-4o");
        
        // 思考模式下启用 reasoning_effort
        if ("thinking".equals(thinkingMode)) {
            optionsBuilder.reasoningEffort("high");
            log.debug("[AI] OpenAI 兼容协议启用思考模式: reasoning_effort=high");
        }
        
        return OpenAiChatModel.builder()
                .openAiClient(client)
                .openAiClientAsync(asyncClient)
                .options(optionsBuilder.build())
                .build();
    }

    /**
     * 创建 Anthropic ChatModel（支持思考模式）
     */
    private AnthropicChatModel createAnthropicChatModelWithThinking(
            String baseUrl, String apiKey, String model, String thinkingMode) {
        com.anthropic.core.ClientOptions.Builder clientOptionsBuilder = com.anthropic.core.ClientOptions.builder()
                .putHeader("x-api-key", apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            clientOptionsBuilder.baseUrl(baseUrl);
        }
        com.anthropic.core.ClientOptions clientOptions = clientOptionsBuilder.build();
        AnthropicClient client = new AnthropicClientImpl(clientOptions);

        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
                .model(model != null ? model : "claude-sonnet-4-20250514");
        
        // 思考模式下启用 extended thinking
        // 注意：Spring AI 2.0 的 AnthropicChatOptions 可能不支持直接的 thinking 配置
        // 如果 Claude 3.7+ 需要思考模式，建议升级到更高版本的 Spring AI
        if ("thinking".equals(thinkingMode)) {
            log.warn("[AI] Anthropic 思考模式在当前 Spring AI 版本中暂不支持，使用默认配置");
            // TODO: 升级到 Spring AI 2.1.0+ 后启用以下代码
            // optionsBuilder.thinkingBudgetTokens(4096);
        }
        
        return AnthropicChatModel.builder()
                .anthropicClient(client)
                .options(optionsBuilder.build())
                .build();
    }
}
