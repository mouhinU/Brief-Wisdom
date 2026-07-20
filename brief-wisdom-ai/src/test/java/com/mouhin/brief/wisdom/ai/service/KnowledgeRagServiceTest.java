package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * KnowledgeRagService 知识库 RAG 服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeRagService 知识库 RAG 服务测试")
class KnowledgeRagServiceTest {

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @InjectMocks
    private KnowledgeRagService knowledgeRagService;

    private KnowledgeDocument sampleDoc;

    @BeforeEach
    void setUp() {
        sampleDoc = new KnowledgeDocument();
        sampleDoc.setId(1L);
        sampleDoc.setTitle("Spring Boot 配置指南");
        sampleDoc.setDocType("INTERNAL");
        sampleDoc.setContent("<p>本文介绍 Spring Boot 的常用配置方式</p>");
        sampleDoc.setTags("spring,配置,java");
    }

    @Test
    @DisplayName("空消息应返回空列表")
    void testRetrieveWithEmptyMessage() {
        List<KnowledgeDocument> result = knowledgeRagService.retrieveRelevantDocuments("");
        assertTrue(result.isEmpty());

        result = knowledgeRagService.retrieveRelevantDocuments(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("有匹配文档时应返回结果")
    void testRetrieveWithMatchingDocs() {
        Page<KnowledgeDocument> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleDoc));

        when(knowledgeDocumentRepository.searchByTitle(anyString(), anyInt(), anyInt()))
                .thenReturn(page);

        List<KnowledgeDocument> result = knowledgeRagService.retrieveRelevantDocuments("Spring Boot 配置");

        assertFalse(result.isEmpty());
        assertEquals("Spring Boot 配置指南", result.get(0).getTitle());
    }

    @Test
    @DisplayName("buildContextFromDocuments 应正确格式化文档内容")
    void testBuildContext() {
        String context = knowledgeRagService.buildContextFromDocuments(List.of(sampleDoc));

        assertTrue(context.contains("知识库参考信息"));
        assertTrue(context.contains("Spring Boot 配置指南"));
        assertTrue(context.contains("Spring Boot 的常用配置方式"));
        assertTrue(context.contains("参考信息结束"));
    }

    @Test
    @DisplayName("buildContextFromDocuments 空列表应返回空字符串")
    void testBuildContextEmpty() {
        String context = knowledgeRagService.buildContextFromDocuments(List.of());
        assertEquals("", context);

        context = knowledgeRagService.buildContextFromDocuments(null);
        assertEquals("", context);
    }

    @Test
    @DisplayName("HTML 标签应被正确去除")
    void testHtmlStripping() {
        sampleDoc.setContent("<h1>标题</h1><p>正文内容</p><br/>");

        String context = knowledgeRagService.buildContextFromDocuments(List.of(sampleDoc));

        assertFalse(context.contains("<h1>"));
        assertFalse(context.contains("<p>"));
        assertTrue(context.contains("标题"));
        assertTrue(context.contains("正文内容"));
    }

    @Test
    @DisplayName("LINK 类型文档应使用 linkDesc 作为内容")
    void testLinkDocType() {
        sampleDoc.setDocType("LINK");
        sampleDoc.setContent(null);
        sampleDoc.setLinkDesc("这是一个外部链接的描述");

        String context = knowledgeRagService.buildContextFromDocuments(List.of(sampleDoc));
        assertTrue(context.contains("这是一个外部链接的描述"));
    }
}
