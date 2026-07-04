package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;

import java.util.List;

/**
 * 知识库管理服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface KnowledgeService {

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    List<KnowledgeBaseDTO> listBasesTree();

    /**
     * 获取所有知识库（平铺列表，含文档数量）
     */
    List<KnowledgeBaseDTO> listBases();

    /**
     * 分页查询顶级知识库
     */
    Page<KnowledgeBaseDTO> listTopBasesPaged(int page, int size);

    /**
     * 获取子知识库
     */
    List<KnowledgeBaseDTO> listChildBases(Long parentId);

    /**
     * 创建知识库
     */
    KnowledgeBaseDTO createBase(KnowledgeBaseBO bo);

    /**
     * 更新知识库
     */
    KnowledgeBaseDTO updateBase(Long id, KnowledgeBaseBO bo);

    /**
     * 删除知识库（同时删除其下所有文档）
     */
    void deleteBase(Long id);

    // ==================== 文档 ====================

    /**
     * 获取知识库下的文档列表（分页）
     */
    Page<KnowledgeDocumentDTO> listDocuments(Long baseId, String docType, int page, int size);

    /**
     * 获取文档详情
     */
    KnowledgeDocumentDTO getDocument(Long id);

    /**
     * 创建文档
     */
    KnowledgeDocumentDTO createDocument(KnowledgeDocumentBO bo);

    /**
     * 更新文档
     */
    KnowledgeDocumentDTO updateDocument(Long id, KnowledgeDocumentBO bo);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);

    /**
     * 搜索文档
     */
    Page<KnowledgeDocumentDTO> searchDocuments(String keyword, int page, int size);
}
