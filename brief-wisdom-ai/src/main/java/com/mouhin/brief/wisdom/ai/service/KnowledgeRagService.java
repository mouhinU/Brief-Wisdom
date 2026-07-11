package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 RAG（检索增强生成）服务
 * <p>
 * 基于 Redis Vector Store 的语义向量检索，从知识库中检索相关文档，
 * 将内容注入 AI 对话的系统提示词中，实现 RAG。
 * <p>
 * 检索策略：
 * 1. 使用 Embedding 模型将用户消息转为向量
 * 2. 在 Redis Vector Store 中进行相似度搜索
 * 3. 取 Top-N 结果，截断内容后注入系统提示词
 * 4. 附加项目代码上下文（如有）
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRagService {

    private final KnowledgeVectorService knowledgeVectorService;
    private final ProjectCodeIndexService projectCodeIndexService;

    /** 注入上下文的最大字符数（避免超出 token 限制） */
    private static final int MAX_CONTEXT_LENGTH = 3000;

    /** 最多注入的文档数量 */
    private static final int MAX_DOCUMENTS = 5;

    /**
     * 根据用户消息检索相关知识文档（基于向量相似度）
     *
     * @param userMessage 用户消息
     * @return 相关文档列表（按相似度排序）
     */
    public List<KnowledgeDocument> retrieveRelevantDocuments(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        log.debug("RAG 向量检索开始: messageLength={}", userMessage.length());

        // 使用向量检索替代关键词匹配
        List<KnowledgeDocument> results = knowledgeVectorService.searchBySimilarity(
                userMessage, MAX_DOCUMENTS);

        log.info("RAG 检索到 {} 篇相关文档", results.size());
        return results;
    }

    /**
     * 将检索到的文档内容构建为 AI 上下文
     *
     * @param documents 相关文档列表
     * @return 格式化的上下文字符串，可注入系统提示词
     */
    public String buildContextFromDocuments(List<KnowledgeDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            // 即使没有知识库文档，也尝试提供项目代码上下文
            return buildProjectCodeContext("");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n--- 知识库参考信息 ---\n");
        sb.append("以下是从知识库中检索到的相关信息，请参考这些信息回答用户问题：\n\n");

        int totalLength = 0;
        for (KnowledgeDocument doc : documents) {
            String content = extractTextContent(doc);
            if (content.isBlank()) {
                continue;
            }

            // 截断过长的内容
            if (totalLength + content.length() > MAX_CONTEXT_LENGTH) {
                int remaining = MAX_CONTEXT_LENGTH - totalLength;
                if (remaining > 100) {
                    content = content.substring(0, remaining) + "...（内容已截断）";
                } else {
                    break;
                }
            }

            sb.append("【").append(doc.getTitle()).append("】\n");
            sb.append(content).append("\n\n");
            totalLength += content.length();
        }

        sb.append("--- 参考信息结束 ---\n");

        // 附加项目代码上下文
        String projectContext = buildProjectCodeContext(String.join(", ",
                documents.stream().map(KnowledgeDocument::getTitle).toList()));
        sb.append(projectContext);

        return sb.toString();
    }

    /**
     * 构建项目代码上下文
     *
     * @param queryKeywords 查询关键词
     * @return 项目代码上下文信息
     */
    private String buildProjectCodeContext(String queryKeywords) {
        if (queryKeywords == null || queryKeywords.isBlank()) {
            return "";
        }

        try {
            var codeFiles = projectCodeIndexService.searchCodeFiles(queryKeywords);
            if (codeFiles.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n--- 项目代码参考 ---\n");
            sb.append("以下是项目中相关的代码文件，供你理解项目结构时参考：\n\n");

            int count = 0;
            for (var file : codeFiles) {
                if (count >= 5) {
                    break;
                }

                sb.append("**").append(file.getFileName()).append("**\n");
                sb.append("- 路径: `").append(file.getFilePath()).append("`\n");
                if (file.getFileType() != null) {
                    sb.append("- 类型: ").append(file.getFileType()).append("\n");
                }
                if (file.getClassName() != null) {
                    sb.append("- 类名: ").append(file.getClassName()).append("\n");
                }
                if (file.getPackageName() != null) {
                    sb.append("- 包名: ").append(file.getPackageName()).append("\n");
                }
                if (file.getSummary() != null && !file.getSummary().isBlank()) {
                    sb.append("- 说明: ").append(file.getSummary().replaceAll("\\s+", " ")).append("\n");
                }
                sb.append("\n");
                count++;
            }

            sb.append("--- 项目代码参考结束 ---\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建项目代码上下文失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从文档中提取纯文本内容
     */
    private String extractTextContent(KnowledgeDocument doc) {
        if ("INTERNAL".equals(doc.getDocType()) && doc.getContent() != null) {
            // 去除 HTML 标签，保留纯文本
            return doc.getContent().replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
        } else if ("LINK".equals(doc.getDocType()) && doc.getLinkDesc() != null) {
            return doc.getLinkDesc();
        }
        return "";
    }
}
