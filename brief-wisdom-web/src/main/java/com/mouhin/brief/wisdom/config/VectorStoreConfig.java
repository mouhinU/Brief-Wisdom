package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.ai.config.AiProviderProperties;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * VectorStore 手动配置（支持 RediSearch 不可用时优雅降级）
 * <p>
 * Spring AI 的 RedisVectorStore 依赖 RediSearch 模块（FT.* 命令）。
 * 当 Redis 实例未安装 RediSearch 模块时，VectorStore 操作变为 no-op，
 * KnowledgeVectorService 会自动感知并跳过向量操作。
 * <p>
 * 要启用向量检索，请安装 Redis Stack（包含 RediSearch 模块）：
 * https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/
 *
 * @author Brief-Wisdom
 * @date 2026-07-12
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.redis.index-name:brief-wisdom-knowledge}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:bw:doc:}")
    private String prefix;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * 创建 EmbeddingModel（对接 DashScope text-embedding-v3）。
     * <p>
     * 使用默认提供商的 OpenAI 兼容协议配置，模型固定为 text-embedding-v3。
     */
    @Bean
    public EmbeddingModel embeddingModel(AiProviderProperties aiProperties) {
        AiProviderProperties.ProviderConfig defaultProvider =
                aiProperties.getProviders().get(aiProperties.getDefaultProvider());
        if (defaultProvider == null) {
            log.warn("[VectorStore] 未找到默认提供商 {} 的配置，EmbeddingModel 不可用", aiProperties.getDefaultProvider());
            return new NoOpEmbeddingModel();
        }
        String apiKey = defaultProvider.getApiKey();
        String baseUrl = defaultProvider.getBaseUrl();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[VectorStore] 默认提供商 {} 未配置 api-key，EmbeddingModel 不可用", aiProperties.getDefaultProvider());
            return new NoOpEmbeddingModel();
        }

        ClientOptions clientOptions = ClientOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .build();
        OpenAIClient client = new OpenAIClientImpl(clientOptions);

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-v3")
                .dimensions(1024)
                .build();

        log.info("[VectorStore] EmbeddingModel 初始化完成（provider: {}, base-url: {}）",
                aiProperties.getDefaultProvider(), baseUrl);
        return new OpenAiEmbeddingModel(client, MetadataMode.EMBED, options);
    }

    /**
     * 创建惰性 VectorStore。
     * <p>
     * 实际的 RedisVectorStore 在首次使用（add/delete/search）时才初始化。
     * 如果 RediSearch 模块不可用，所有操作变为 no-op，search 返回空列表。
     * 这样应用可以在没有 RediSearch 的 Redis 环境下正常启动。
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        AtomicReference<VectorStore> delegate = new AtomicReference<>();
        AtomicReference<Boolean> initAttempted = new AtomicReference<>(false);

        return new LazyVectorStore(delegate, initAttempted, () -> {
            try {
                RedisClient redisClient = createRedisClient();
                RedisVectorStore store = RedisVectorStore.builder(redisClient, embeddingModel)
                        .indexName(indexName)
                        .prefix(prefix)
                        .initializeSchema(true)
                        .build();
                store.afterPropertiesSet();
                log.info("[VectorStore] Redis Vector Store 初始化成功（索引: {}）", indexName);
                return store;
            } catch (Exception e) {
                log.warn("[VectorStore] RediSearch 模块不可用，向量检索已禁用。"
                        + "原因: {}。如需启用，请安装 Redis Stack。", e.getMessage());
                return null;
            }
        });
    }

    private RedisClient createRedisClient() {
        if (password != null && !password.isEmpty()) {
            return RedisClient.builder()
                    .hostAndPort(host, port)
                    .clientConfig(DefaultJedisClientConfig.builder()
                            .password(password)
                            .build())
                    .build();
        } else {
            return RedisClient.create(host, port);
        }
    }
}
