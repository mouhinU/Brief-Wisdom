package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库 RAG（检索增强生成）服务
 * <p>
 * 基于关键词匹配从知识库中检索相关文档，将内容注入 AI 对话的系统提示词中，
 * 实现基础版 RAG。后续可升级为向量检索（Embedding + VectorStore）。
 * <p>
 * 检索策略：
 * 1. 从用户消息中提取关键词
 * 2. 在知识库文档的标题、标签、内容中进行模糊匹配
 * 3. 按相关度排序，取 Top-N 结果
 * 4. 将文档内容截断后注入系统提示词
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRagService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final ProjectCodeIndexService projectCodeIndexService;

    /** 注入上下文的最大字符数（避免超出 token 限制） */
    private static final int MAX_CONTEXT_LENGTH = 3000;

    /** 最多注入的文档数量 */
    private static final int MAX_DOCUMENTS = 3;

    /** 中文分词的最小长度 */
    private static final int MIN_KEYWORD_LENGTH = 2;

    /** 中文关键词提取正则（预编译） */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");

    /** 英文关键词提取正则（预编译） */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]{3,}");

    /**
     * 根据用户消息检索相关知识文档
     *
     * @param userMessage 用户消息
     * @return 相关文档列表（按相关度排序）
     */
    public List<KnowledgeDocument> retrieveRelevantDocuments(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        // 提取关键词
        List<String> keywords = extractKeywords(userMessage);
        if (keywords.isEmpty()) {
            return List.of();
        }

        log.debug("RAG 检索关键词: {}", keywords);

        // 对每个关键词搜索匹配的文档
        List<ScoredDocument> scoredDocs = new ArrayList<>();
        for (String keyword : keywords) {
            var results = knowledgeDocumentRepository.searchByTitle(keyword, 1, 10);
            for (KnowledgeDocument doc : results.getRecords()) {
                addOrUpdateScore(scoredDocs, doc, keyword, 1.0);
            }

            // 也搜索标签匹配
            var tagResults = knowledgeDocumentRepository.searchByTitle(keyword, 1, 5);
            for (KnowledgeDocument doc : tagResults.getRecords()) {
                if (doc.getTags() != null && doc.getTags().contains(keyword)) {
                    addOrUpdateScore(scoredDocs, doc, keyword, 1.5);
                }
            }
        }

        // 按分数降序排序，取 Top-N
        scoredDocs.sort((a, b) -> Double.compare(b.score, a.score));

        List<KnowledgeDocument> result = new ArrayList<>();
        for (int i = 0; i < Math.min(scoredDocs.size(), MAX_DOCUMENTS); i++) {
            result.add(scoredDocs.get(i).document);
        }

        log.info("RAG 检索到 {} 篇相关文档（关键词: {}）", result.size(), keywords);
        return result;
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
                if (count >= 5) break; // 最多展示5个相关文件
                
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
     * 从用户消息中提取关键词
     * <p>
     * 简单实现：提取连续的中文字符串和英文单词作为关键词。
     * 后续可升级为 jieba 分词或 TF-IDF。
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();

        // 提取中文关键词（2个及以上汉字）
        Matcher matcher = CHINESE_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= MIN_KEYWORD_LENGTH && !isStopWord(word)) {
                keywords.add(word);
            }
        }

        // 提取英文关键词（3个及以上字母）
        matcher = ENGLISH_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (!isEnglishStopWord(word)) {
                keywords.add(word);
            }
        }

        // 去重
        return keywords.stream().distinct().toList();
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

    private void addOrUpdateScore(List<ScoredDocument> scoredDocs, KnowledgeDocument doc,
                                  String keyword, double weight) {
        for (ScoredDocument sd : scoredDocs) {
            if (sd.document.getId().equals(doc.getId())) {
                sd.score += weight;
                return;
            }
        }
        scoredDocs.add(new ScoredDocument(doc, weight));
    }

    /** 中文停用词 */
    private boolean isStopWord(String word) {
        return word.matches("^(的|了|在|是|我|有|和|就|不|人|都|一|一个|上|也|很|到|说|要|去|你|会|着|没有|看|好|自己|这|他|她|它|们|那|些|什么|怎么|如何|为什么|可以|已经|还是|或者|但是|因为|所以|如果|虽然|然后|因此|而|且|或|对|从|向|把|被|让|给|用|做|想|能|会|该|应|需要|可能|应该)$");
    }

    /** 英文停用词 */
    private boolean isEnglishStopWord(String word) {
        return word.matches("^(the|a|an|is|are|was|were|be|been|being|have|has|had|do|does|did|will|would|could|should|may|might|can|shall|this|that|these|those|i|you|he|she|it|we|they|me|him|her|us|them|my|your|his|its|our|their|what|which|who|whom|when|where|why|how|all|each|every|both|few|more|most|other|some|such|no|not|only|same|so|than|too|very|just|because|but|and|or|if|while|with|for|about|against|between|through|during|before|after|above|below|from|into|out|off|over|under|again|further|then|once)$");
    }

    /**
     * 带评分的文档包装
     */
    private static class ScoredDocument {
        final KnowledgeDocument document;
        double score;

        ScoredDocument(KnowledgeDocument document, double score) {
            this.document = document;
            this.score = score;
        }
    }
}
