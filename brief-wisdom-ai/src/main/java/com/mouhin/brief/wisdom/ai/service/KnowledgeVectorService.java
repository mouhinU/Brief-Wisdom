package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库向量检索服务
 * <p>
 * 基于 Redis Vector Store 实现语义向量检索，替代原有的关键词匹配 RAG。
 * 负责文档的向量化存储、删除和相似度搜索。
 *
 * @author Brief-Wisdom
 * @date 2026-07-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {

    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    /** 向量搜索默认返回的文档数量 */
    private static final int DEFAULT_TOP_K = 5;

    /** 相似度阈值（低于此值的结果将被过滤） */
    private static final double SIMILARITY_THRESHOLD = 0.3;

    /** 单个分块的最大字符数 */
    private static final int MAX_CHUNK_SIZE = 2000;

    /** 分块之间的重叠字符数（保证上下文连续性） */
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 将知识库文档向量化并存入 Redis Vector Store
     * <p>
     * 对长文档按标题/段落智能分块，每块独立向量化，提升检索精度。
     *
     * @param doc 知识库文档
     */
    public void vectorizeAndStore(KnowledgeDocument doc) {
        if (doc == null) {
            return;
        }

        String textContent = extractTextContent(doc);
        if (textContent == null || textContent.isBlank()) {
            log.debug("文档内容为空，跳过向量化: docId={}, title={}", doc.getId(), doc.getTitle());
            return;
        }

        // 智能分块：短文档单块存储，长文档按段落/标题切分
        List<String> chunks = splitIntoChunks(textContent, MAX_CHUNK_SIZE, CHUNK_OVERLAP);

        // 先删除旧向量（避免重复）
        deleteFromVectorStore(doc.getId());

        // 构建 Spring AI Document 列表，附带元数据
        List<Document> aiDocuments = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>(8);
            metadata.put("docId", String.valueOf(doc.getId()));
            metadata.put("baseId", String.valueOf(doc.getBaseId()));
            metadata.put("title", doc.getTitle() != null ? doc.getTitle() : "");
            metadata.put("docType", doc.getDocType() != null ? doc.getDocType() : "");
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            if (doc.getTags() != null) {
                metadata.put("tags", doc.getTags());
            }
            aiDocuments.add(new Document(chunks.get(i), metadata));
        }

        try {
            vectorStore.add(aiDocuments);
            log.info("文档向量化完成: docId={}, title={}, chunks={}, totalLength={}",
                    doc.getId(), doc.getTitle(), chunks.size(), textContent.length());
        } catch (Exception e) {
            log.error("文档向量化失败: docId={}, error={}", doc.getId(), e.getMessage(), e);
        }
    }

    /**
     * 将长文本按标题和段落边界智能分块
     * <p>
     * 优先在 Markdown 标题（#/##/###）处切分，其次在段落边界切分，
     * 最后在任意位置切分以保证块大小不超过上限。相邻块之间保留重叠以维持上下文。
     *
     * @param text      原始文本
     * @param maxChunk  单块最大字符数
     * @param overlap   块间重叠字符数
     * @return 分块列表
     */
    List<String> splitIntoChunks(String text, int maxChunk, int overlap) {
        if (text.length() <= maxChunk) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        // 优先按 Markdown 标题拆分
        String[] sections = text.split("(?=(?:^|\\n)#{1,3}\\s)");

        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) {
                continue;
            }
            if (section.length() <= maxChunk) {
                chunks.add(section);
            } else {
                // 超长 section 按段落进一步拆分
                splitByParagraph(section, maxChunk, chunks);
            }
        }

        // 如果分块结果只有 1 个，说明无法按标题/段落切分，直接截断
        if (chunks.isEmpty()) {
            chunks.add(text.substring(0, Math.min(text.length(), maxChunk)));
            return chunks;
        }

        // 合并过小的块，添加重叠
        return mergeAndOverlapChunks(chunks, maxChunk, overlap);
    }

    /**
     * 按段落边界拆分超长文本
     */
    private void splitByParagraph(String text, int maxChunk, List<String> chunks) {
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            if (current.length() + paragraph.length() + 2 > maxChunk && current.length() > 0) {
                chunks.add(current.toString().trim());
                // 保留尾部作为重叠
                String tail = current.toString();
                if (tail.length() > 200) {
                    current = new StringBuilder(tail.substring(tail.length() - 200));
                    current.append("\n\n");
                } else {
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }

        if (current.length() > 0) {
            String remaining = current.toString().trim();
            if (remaining.length() > maxChunk) {
                // 最终兜底：硬切分
                for (int i = 0; i < remaining.length(); i += maxChunk - 200) {
                    int end = Math.min(i + maxChunk, remaining.length());
                    chunks.add(remaining.substring(i, end));
                    if (end >= remaining.length()) {
                        break;
                    }
                }
            } else {
                chunks.add(remaining);
            }
        }
    }

    /**
     * 合并过小的块并在相邻块间添加重叠
     */
    private List<String> mergeAndOverlapChunks(List<String> rawChunks, int maxChunk, int overlap) {
        if (rawChunks.size() <= 1) {
            return rawChunks;
        }

        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String chunk : rawChunks) {
            if (buffer.length() + chunk.length() + 2 <= maxChunk) {
                if (buffer.length() > 0) {
                    buffer.append("\n\n");
                }
                buffer.append(chunk);
            } else {
                if (buffer.length() > 0) {
                    result.add(buffer.toString().trim());
                }
                buffer = new StringBuilder(chunk);
            }
        }
        if (buffer.length() > 0) {
            result.add(buffer.toString().trim());
        }

        // 如果合并后仍只有 1 块，直接返回
        if (result.size() <= 1) {
            return result;
        }

        // 添加重叠：每块的开头包含上一块尾部的内容
        if (overlap <= 0) {
            return result;
        }
        List<String> overlapped = new ArrayList<>(result.size());
        overlapped.add(result.get(0));
        for (int i = 1; i < result.size(); i++) {
            String prev = result.get(i - 1);
            String tail = prev.length() > overlap ? prev.substring(prev.length() - overlap) : prev;
            overlapped.add(tail + "\n" + result.get(i));
        }
        return overlapped;
    }

    /**
     * 从 Vector Store 中删除指定文档的向量
     *
     * @param documentId 知识库文档 ID
     */
    public void deleteFromVectorStore(Long documentId) {
        if (documentId == null) {
            return;
        }
        try {
            vectorStore.delete(List.of(String.valueOf(documentId)));
            log.debug("删除文档向量: docId={}", documentId);
        } catch (Exception e) {
            log.warn("删除文档向量失败（可忽略）: docId={}, error={}", documentId, e.getMessage());
        }
    }

    /**
     * 基于语义相似度搜索相关知识文档
     *
     * @param query 用户查询文本
     * @return 匹配的知识文档列表（按相似度排序）
     */
    public List<KnowledgeDocument> searchBySimilarity(String query) {
        return searchBySimilarity(query, DEFAULT_TOP_K);
    }

    /**
     * 基于语义相似度搜索相关知识文档
     *
     * @param query 用户查询文本
     * @param topK  返回的最大文档数量
     * @return 匹配的知识文档列表（按相似度排序）
     */
    public List<KnowledgeDocument> searchBySimilarity(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );

            if (results.isEmpty()) {
                log.debug("向量检索无结果: query={}", query);
                return List.of();
            }

            // 从向量结果中提取 docId，批量查询数据库获取完整文档（避免 N+1 查询）
            List<Long> docIds = new java.util.ArrayList<>();
            for (Document result : results) {
                Object docIdObj = result.getMetadata().get("docId");
                if (docIdObj != null) {
                    docIds.add(Long.valueOf(docIdObj.toString()));
                }
            }

            if (docIds.isEmpty()) {
                return List.of();
            }

            // 批量查询所有文档
            List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByIds(docIds);
            // 保持向量搜索的相似度排序
            Map<Long, KnowledgeDocument> docMap = new HashMap<>(allDocs.size());
            for (KnowledgeDocument doc : allDocs) {
                docMap.put(doc.getId(), doc);
            }

            List<KnowledgeDocument> documents = new java.util.ArrayList<>();
            for (Long docId : docIds) {
                KnowledgeDocument doc = docMap.get(docId);
                if (doc != null) {
                    documents.add(doc);
                }
            }

            log.info("向量检索完成: query={}, 命中={}篇", query, documents.size());
            return documents;

        } catch (Exception e) {
            log.warn("向量检索失败，降级为空结果: query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * 重建所有文档的向量索引
     * <p>
     * 用于首次启用向量检索或索引损坏时，全量重建向量数据。
     *
     * @return 成功向量化的文档数量
     */
    public int reindexAll() {
        log.info("开始全量重建向量索引...");
        List<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findAll();
        int successCount = 0;

        for (KnowledgeDocument doc : allDocuments) {
            try {
                String content = extractTextContent(doc);
                if (content != null && !content.isBlank()) {
                    vectorizeAndStore(doc);
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("重建向量索引失败: docId={}, error={}", doc.getId(), e.getMessage());
            }
        }

        log.info("全量重建向量索引完成: 总文档={}, 成功={}", allDocuments.size(), successCount);
        return successCount;
    }

    /**
     * 从知识库文档中提取纯文本内容
     */
    private String extractTextContent(KnowledgeDocument doc) {
        if ("INTERNAL".equals(doc.getDocType()) && doc.getContent() != null) {
            // 去除 HTML 标签，保留纯文本
            return doc.getContent()
                    .replaceAll("<[^>]+>", "")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .trim();
        } else if ("LINK".equals(doc.getDocType()) && doc.getLinkDesc() != null) {
            return doc.getLinkDesc();
        } else if ("FILE".equals(doc.getDocType())) {
            // FILE 类型：使用文件名和文件类型作为可检索内容
            StringBuilder sb = new StringBuilder();
            if (doc.getFileName() != null) {
                sb.append(doc.getFileName());
            }
            if (doc.getFileType() != null) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("(").append(doc.getFileType()).append(")");
            }
            return sb.toString();
        }
        return "";
    }
}
