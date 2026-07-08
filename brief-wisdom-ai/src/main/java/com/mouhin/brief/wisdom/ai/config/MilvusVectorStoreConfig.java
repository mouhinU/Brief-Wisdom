package com.mouhin.brief.wisdom.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * <p>
 * 用于知识库文档的向量化存储和语义检索
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
public class MilvusVectorStoreConfig {

    /** Milvus 服务地址 */
    private String host = "localhost";

    /** Milvus 服务端口 */
    private int port = 19530;

    /** Milvus 用户名(可选) */
    private String username;

    /** Milvus 密码(可选) */
    private String password;

    /** 集合名称 */
    private String collectionName = "knowledge_embeddings";

    /** 向量维度(与 Embedding 模型匹配，默认 OpenAI/DashScope 为 1536) */
    private int dimension = 1536;

    /**
     * 创建 Milvus 客户端连接
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        try {
            ConnectParam.Builder builder = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port);

            if (username != null && !username.isBlank()) {
                builder.withAuthorization(username, password != null ? password : "");
            }

            MilvusServiceClient client = new MilvusServiceClient(builder.build());
            log.info("Milvus 客户端初始化成功: host={}, port={}", host, port);
            return client;
        } catch (Exception e) {
            log.error("Milvus 客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法连接到 Milvus 服务，请检查 Milvus 是否正常运行", e);
        }
    }
}
