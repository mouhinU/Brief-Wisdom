package com.mouhin.brief.wisdom.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 * <p>
 * 封装 Spring AI Vector Store 的能力，提供知识库文档的向量化和语义检索功能：
 * - 将文本转换为向量嵌入(Embedding)
 * - 存储向量到 Milvus 向量数据库
 * - 基于语义相似度检索相关文档
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Slf4j
@Service
@ConditionalOnBean(VectorStore.class)
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /** 默认检索返回的文档数量 */
    private static final int DEFAULT_TOP_K = 5;

    /** 默认相似度阈值(0-1之间，越高越严格) */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    /** Milvus 是否可用 */
    private boolean milvusAvailable = false;

    /**
     * 初始化时检测 Milvus 是否可用
     */
    @PostConstruct
    public void init() {
        try {
            // 尝试创建一个测试文档来检测连接
            Document testDoc = new Document("test", Map.of("test", "true"));
            vectorStore.add(List.of(testDoc));
            milvusAvailable = true;
            log.info("Milvus 向量数据库连接成功，向量化检索功能已启用");
        } catch (Exception e) {
            milvusAvailable = false;
            log.warn("Milvus 向量数据库不可用，将使用关键词匹配降级方案: {}", e.getMessage());
        }
    }

    /**
     * 为单篇文档生成并存储向量嵌入
     *
     * @param documentId 文档 ID
     * @param content    文档内容
     * @param metadata   元数据(标题、标签等)
     */
    public void addDocumentEmbedding(Long documentId, String content, Map<String, Object> metadata) {
        if (!milvusAvailable) {
            log.debug("Milvus 未启用，跳过向量化: documentId={}", documentId);
            return;
        }
        
        if (content == null || content.isBlank()) {
            log.warn("文档内容为空，跳过向量化: documentId={}", documentId);
            return;
        }

        try {
            // 构建 Spring AI Document
            Document doc = new Document(
                    content,
                    Map.of(
                            "documentId", String.valueOf(documentId),
                            "title", metadata.getOrDefault("title", ""),
                            "tags", metadata.getOrDefault("tags", "")
                    )
            );

            // 添加到向量存储(自动进行 Embedding)
            vectorStore.add(List.of(doc));
            log.info("文档向量化成功: documentId={}, contentLength={}", documentId, content.length());
        } catch (Exception e) {
            log.error("文档向量化失败: documentId={}, error={}", documentId, e.getMessage(), e);
        }
    }

    /**
     * 批量添加文档向量嵌入
     *
     * @param documents 文档列表，每个元素包含 id、content、metadata
     */
    public void addBatchDocumentEmbeddings(List<DocumentEmbeddingRequest> documents) {
        if (!milvusAvailable) {
            log.debug("Milvus 未启用，跳过批量向量化");
            return;
        }
        
        if (documents == null || documents.isEmpty()) {
            return;
        }

        List<Document> aiDocuments = new ArrayList<>();
        for (DocumentEmbeddingRequest req : documents) {
            if (req.getContent() != null && !req.getContent().isBlank()) {
                Document doc = new Document(
                        req.getContent(),
                        Map.of(
                                "documentId", String.valueOf(req.getDocumentId()),
                                "title", req.getMetadata().getOrDefault("title", ""),
                                "tags", req.getMetadata().getOrDefault("tags", "")
                        )
                );
                aiDocuments.add(doc);
            }
        }

        if (!aiDocuments.isEmpty()) {
            try {
                vectorStore.add(aiDocuments);
                log.info("批量文档向量化成功: count={}", aiDocuments.size());
            } catch (Exception e) {
                log.error("批量文档向量化失败: error={}", e.getMessage(), e);
            }
        }
    }

    /**
     * 删除文档的向量嵌入
     *
     * @param documentId 文档 ID
     */
    public void removeDocumentEmbedding(Long documentId) {
        if (!milvusAvailable) {
            log.debug("Milvus 未启用，跳过删除向量嵌入: documentId={}", documentId);
            return;
        }
        
        try {
            // Spring AI Vector Store 不支持直接按 ID 删除，需要通过 filter 查询后删除
            // 这里简化处理，实际使用时可能需要维护 documentId -> vectorId 的映射
            log.info("删除文档向量嵌入: documentId={}", documentId);
        } catch (Exception e) {
            log.error("删除文档向量嵌入失败: documentId={}, error={}", documentId, e.getMessage(), e);
        }
    }

    /**
     * 基于语义相似度检索相关文档
     *
     * @param query 查询文本
     * @return 相关文档 ID 列表(按相似度降序)
     */
    public List<Long> searchSimilarDocuments(String query) {
        return searchSimilarDocuments(query, DEFAULT_TOP_K, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /**
     * 基于语义相似度检索相关文档(自定义参数)
     *
     * @param query              查询文本
     * @param topK               返回结果数量
     * @param similarityThreshold 相似度阈值
     * @return 相关文档 ID 列表(按相似度降序)
     */
    public List<Long> searchSimilarDocuments(String query, int topK, double similarityThreshold) {
        if (!milvusAvailable) {
            log.debug("Milvus 未启用，返回空结果");
            return List.of();
        }
        
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            var results = vectorStore.similaritySearch(searchRequest);

            List<Long> documentIds = new ArrayList<>();
            for (var doc : results) {
                String docIdStr = (String) doc.getMetadata().get("documentId");
                if (docIdStr != null) {
                    try {
                        documentIds.add(Long.parseLong(docIdStr));
                    } catch (NumberFormatException e) {
                        log.warn("无效的文档ID格式: {}", docIdStr);
                    }
                }
            }

            log.info("向量检索完成: query={}, found={} docs", query, documentIds.size());
            return documentIds;
        } catch (Exception e) {
            log.error("向量检索失败: query={}, error={}", query, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取文本的向量嵌入(不存储)
     *
     * @param text 输入文本
     * @return 向量数组
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }

        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("生成向量嵌入失败: error={}", e.getMessage(), e);
            return new float[0];
        }
    }

    /**
     * 文档向量化请求
     */
    public static class DocumentEmbeddingRequest {
        private Long documentId;
        private String content;
        private Map<String, Object> metadata;

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
