package com.mouhin.brief.wisdom.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
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
