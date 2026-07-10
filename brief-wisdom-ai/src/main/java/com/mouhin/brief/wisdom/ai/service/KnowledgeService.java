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
     *
     * @return 知识库树形 DTO 列表
     */
    List<KnowledgeBaseDTO> listBasesTree();

    /**
     * 获取所有知识库（平铺列表，含文档数量）
     *
     * @return 知识库 DTO 列表
     */
    List<KnowledgeBaseDTO> listBases();

    /**
     * 分页查询顶级知识库
     *
     * @param page 页码（从 1 开始）
     * @param size 每页大小
     * @return 分页结果
     */
    Page<KnowledgeBaseDTO> listTopBasesPaged(int page, int size);

    /**
     * 获取子知识库
     *
     * @param parentId 父知识库 ID
     * @return 子知识库 DTO 列表
     */
    List<KnowledgeBaseDTO> listChildBases(Long parentId);

    /**
     * 创建知识库
     *
     * @param bo 知识库业务对象
     * @return 创建后的知识库 DTO
     */
    KnowledgeBaseDTO createBase(KnowledgeBaseBO bo);

    /**
     * 更新知识库
     *
     * @param id 知识库 ID
     * @param bo 知识库业务对象
     * @return 更新后的知识库 DTO
     */
    KnowledgeBaseDTO updateBase(Long id, KnowledgeBaseBO bo);

    /**
     * 删除知识库（若知识库下还有文档则拒绝删除）
     *
     * @param id 知识库 ID
     */
    void deleteBase(Long id);

    // ==================== 文档 ====================

    /**
     * 获取知识库下的文档列表（分页）
     *
     * @param baseId  知识库 ID
     * @param docType 文档类型（可选）
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    Page<KnowledgeDocumentDTO> listDocuments(Long baseId, String docType, int page, int size);

    /**
     * 获取文档详情
     *
     * @param id 文档 ID
     * @return 文档 DTO
     */
    KnowledgeDocumentDTO getDocument(Long id);

    /**
     * 创建文档
     *
     * @param bo 文档业务对象
     * @return 创建后的文档 DTO
     */
    KnowledgeDocumentDTO createDocument(KnowledgeDocumentBO bo);

    /**
     * 更新文档
     *
     * @param id 文档 ID
     * @param bo 文档业务对象
     * @return 更新后的文档 DTO
     */
    KnowledgeDocumentDTO updateDocument(Long id, KnowledgeDocumentBO bo);

    /**
     * 删除文档
     *
     * @param id 文档 ID
     */
    void deleteDocument(Long id);

    /**
     * 批量删除文档
     *
     * @param ids 文档 ID 列表
     * @return 实际删除的文档数量
     */
    int batchDeleteDocuments(List<Long> ids);

    /**
     * 搜索文档（按标题模糊查询）
     *
     * @param keyword 搜索关键词
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    Page<KnowledgeDocumentDTO> searchDocuments(String keyword, int page, int size);

    /**
     * 按 Markdown 导入源路径 upsert 文档：已导入则更新，未导入则新增
     *
     * @param bo         文档内容
     * @param sourcePath 相对项目根目录的源文件路径（存入 fileName 用于去重）
     * @return true 表示新增，false 表示更新
     */
    boolean upsertImportedMarkdown(KnowledgeDocumentBO bo, String sourcePath);
}
