package com.mouhin.brief.wisdom.ai.service.impl;

import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeServiceImpl Markdown upsert 测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeServiceImpl Markdown upsert 测试")
class KnowledgeServiceImplUpsertTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @InjectMocks
    private KnowledgeServiceImpl knowledgeService;

    @Test
    @DisplayName("源路径不存在时新增文档")
    void upsertImportedMarkdownCreatesNewDocument() {
        KnowledgeDocumentBO bo = buildDocumentBO();
        when(knowledgeDocumentRepository.findByBaseIdAndFileName(1L, "docs/readme.md")).thenReturn(null);
        when(knowledgeDocumentRepository.findLegacyImportedByTitle(1L, "readme", "INTERNAL")).thenReturn(null);

        boolean created = knowledgeService.upsertImportedMarkdown(bo, "docs/readme.md");

        assertTrue(created);
        verify(knowledgeDocumentRepository).save(any(KnowledgeDocument.class));
        verify(knowledgeDocumentRepository, never()).update(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("源路径已存在时更新文档")
    void upsertImportedMarkdownUpdatesExistingDocument() {
        KnowledgeDocumentBO bo = buildDocumentBO();
        KnowledgeDocument existing = new KnowledgeDocument();
        existing.setId(10L);
        existing.setBaseId(1L);
        existing.setTitle("readme");
        when(knowledgeDocumentRepository.findByBaseIdAndFileName(1L, "docs/readme.md")).thenReturn(existing);

        boolean created = knowledgeService.upsertImportedMarkdown(bo, "docs/readme.md");

        assertFalse(created);
        verify(knowledgeDocumentRepository).update(existing);
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("兼容历史记录：按标题匹配并更新")
    void upsertImportedMarkdownUpdatesLegacyDocument() {
        KnowledgeDocumentBO bo = buildDocumentBO();
        KnowledgeDocument legacy = new KnowledgeDocument();
        legacy.setId(20L);
        legacy.setBaseId(1L);
        legacy.setTitle("readme");
        when(knowledgeDocumentRepository.findByBaseIdAndFileName(1L, "docs/readme.md")).thenReturn(null);
        when(knowledgeDocumentRepository.findLegacyImportedByTitle(1L, "readme", "INTERNAL")).thenReturn(legacy);

        boolean created = knowledgeService.upsertImportedMarkdown(bo, "docs/readme.md");

        assertFalse(created);
        verify(knowledgeDocumentRepository).update(legacy);
    }

    private KnowledgeDocumentBO buildDocumentBO() {
        KnowledgeDocumentBO bo = new KnowledgeDocumentBO();
        bo.setBaseId(1L);
        bo.setTitle("readme");
        bo.setDocType("INTERNAL");
        bo.setContent("# Readme");
        bo.setStatus(1);
        return bo;
    }
}
