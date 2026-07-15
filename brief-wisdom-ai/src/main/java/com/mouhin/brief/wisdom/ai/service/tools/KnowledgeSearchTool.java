package com.mouhin.brief.wisdom.ai.service.tools;

import com.mouhin.brief.wisdom.ai.service.KnowledgeRagService;
import com.mouhin.brief.wisdom.ai.service.KnowledgeVectorService;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库语义搜索工具
 * <p>
 * 基于 Redis Vector Store 的语义向量检索，从知识库中检索相关文档。
 * 替代原有的每次对话都注入 RAG 上下文的方式，改为按需调用，节省 Token。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool {

    private final KnowledgeVectorService knowledgeVectorService;
    private final KnowledgeRagService knowledgeRagService;

    /**
     * 从知识库中搜索与查询文本相关的文档
     *
     * @param query 搜索查询文本
     * @param topK  返回结果数量，默认 3
     * @return 格式化的相关文档内容
     */
    @Tool(description = "从知识库中搜索与用户问题相关的文档内容。当用户的问题可能涉及项目知识、业务规则、技术文档、公司制度时调用此工具。")
    public String searchKnowledge(
            @ToolParam(description = "搜索查询文本，应包含核心关键词") String query,
            @ToolParam(description = "返回结果数量，默认3，最大10", required = false) Integer topK) {

        log.info("[Tool] searchKnowledge 被调用: query={}, topK={}", query, topK);

        int effectiveTopK = (topK != null && topK > 0) ? Math.min(topK, 10) : 3;

        try {
            List<KnowledgeDocument> docs = knowledgeVectorService.searchBySimilarity(query, effectiveTopK);
            if (docs.isEmpty()) {
                return "未在知识库中找到与「" + query + "」相关的文档。";
            }

            String context = knowledgeRagService.buildContextFromDocuments(docs);
            return "从知识库中检索到 " + docs.size() + " 篇相关文档：\n" + context;
        } catch (Exception e) {
            log.error("[Tool] searchKnowledge 执行失败: {}", e.getMessage(), e);
            return "知识库搜索失败: " + e.getMessage();
        }
    }
}
