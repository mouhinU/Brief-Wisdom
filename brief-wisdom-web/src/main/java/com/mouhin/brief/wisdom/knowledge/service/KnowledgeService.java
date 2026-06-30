package com.mouhin.brief.wisdom.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseRequest;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentRequest;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库管理业务层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    public List<KnowledgeBaseDTO> listBasesTree() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        return buildTree(allBases, 0L);
    }

    /**
     * 获取所有知识库（平铺列表，含文档数量）
     */
    public List<KnowledgeBaseDTO> listBases() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        return allBases.stream().map(this::toBaseDTO).toList();
    }

    /**
     * 获取子知识库
     */
    public List<KnowledgeBaseDTO> listChildBases(Long parentId) {
        List<KnowledgeBase> bases = knowledgeBaseRepository.findByParentId(parentId);
        return bases.stream().map(this::toBaseDTO).toList();
    }

    /**
     * 创建知识库
     */
    public KnowledgeBaseDTO createBase(KnowledgeBaseRequest request) {
        KnowledgeBase base = new KnowledgeBase();
        base.setName(request.getName());
        base.setDescription(request.getDescription());
        base.setIcon(request.getIcon() != null ? request.getIcon() : "📚");
        base.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        base.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        base.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 0);
        knowledgeBaseRepository.save(base);
        return toBaseDTO(base);
    }

    /**
     * 更新知识库
     */
    public KnowledgeBaseDTO updateBase(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id);
        if (base == null) {
            throw new IllegalArgumentException("知识库不存在: " + id);
        }
        if (request.getName() != null) base.setName(request.getName());
        if (request.getDescription() != null) base.setDescription(request.getDescription());
        if (request.getIcon() != null) base.setIcon(request.getIcon());
        if (request.getParentId() != null) base.setParentId(request.getParentId());
        if (request.getSortOrder() != null) base.setSortOrder(request.getSortOrder());
        if (request.getIsPublic() != null) base.setIsPublic(request.getIsPublic());
        knowledgeBaseRepository.update(base);
        return toBaseDTO(base);
    }

    /**
     * 删除知识库（同时删除其下所有文档）
     */
    @Transactional
    public void deleteBase(Long id) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id);
        if (base == null) {
            throw new IllegalArgumentException("知识库不存在: " + id);
        }
        // 检查是否有子知识库
        long childCount = knowledgeBaseRepository.countByParentId(id);
        if (childCount > 0) {
            throw new IllegalArgumentException("该知识库下还有子知识库，请先删除子知识库");
        }
        // 删除知识库下的所有文档
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.findByBaseId(id);
        for (KnowledgeDocument doc : docs) {
            knowledgeDocumentRepository.deleteById(doc.getId());
        }
        knowledgeBaseRepository.deleteById(id);
    }

    // ==================== 文档 ====================

    /**
     * 获取知识库下的文档列表（分页）
     */
    public Page<KnowledgeDocumentDTO> listDocuments(Long baseId, String docType, int page, int size) {
        Page<KnowledgeDocument> docPage;
        if (docType != null && !docType.isBlank()) {
            docPage = knowledgeDocumentRepository.findByBaseIdAndTypePaged(baseId, docType, page, size);
        } else {
            docPage = knowledgeDocumentRepository.findByBaseIdPaged(baseId, page, size);
        }
        // 转换为 DTO 分页
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        dtoPage.setRecords(docPage.getRecords().stream().map(this::toDocDTO).toList());
        return dtoPage;
    }

    /**
     * 获取文档详情
     */
    public KnowledgeDocumentDTO getDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
        // 增加浏览次数
        knowledgeDocumentRepository.incrementViewCount(id);
        return toDocDTO(doc);
    }

    /**
     * 创建文档
     */
    public KnowledgeDocumentDTO createDocument(KnowledgeDocumentRequest request) {
        KnowledgeDocument doc = new KnowledgeDocument();
        copyRequestToDoc(request, doc);
        doc.setViewCount(0);
        knowledgeDocumentRepository.save(doc);
        return toDocDTO(doc);
    }

    /**
     * 更新文档
     */
    public KnowledgeDocumentDTO updateDocument(Long id, KnowledgeDocumentRequest request) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
        copyRequestToDoc(request, doc);
        knowledgeDocumentRepository.update(doc);
        return toDocDTO(doc);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
        knowledgeDocumentRepository.deleteById(id);
    }

    /**
     * 搜索文档
     */
    public Page<KnowledgeDocumentDTO> searchDocuments(String keyword, int page, int size) {
        Page<KnowledgeDocument> docPage = knowledgeDocumentRepository.searchByTitle(keyword, page, size);
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        dtoPage.setRecords(docPage.getRecords().stream().map(this::toDocDTO).toList());
        return dtoPage;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建知识库树形结构
     */
    private List<KnowledgeBaseDTO> buildTree(List<KnowledgeBase> allBases, Long parentId) {
        List<KnowledgeBaseDTO> tree = new ArrayList<>();
        for (KnowledgeBase base : allBases) {
            if (parentId.equals(base.getParentId())) {
                KnowledgeBaseDTO dto = toBaseDTO(base);
                dto.setChildren(buildTree(allBases, base.getId()));
                tree.add(dto);
            }
        }
        return tree;
    }

    private KnowledgeBaseDTO toBaseDTO(KnowledgeBase base) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(base.getId());
        dto.setName(base.getName());
        dto.setDescription(base.getDescription());
        dto.setIcon(base.getIcon());
        dto.setParentId(base.getParentId());
        dto.setSortOrder(base.getSortOrder());
        dto.setIsPublic(base.getIsPublic());
        dto.setCreateTime(base.getCreateTime());
        dto.setUpdateTime(base.getUpdateTime());
        // 查询文档数量
        dto.setDocumentCount(knowledgeDocumentRepository.countByBaseId(base.getId()));
        return dto;
    }

    private KnowledgeDocumentDTO toDocDTO(KnowledgeDocument doc) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setId(doc.getId());
        dto.setBaseId(doc.getBaseId());
        dto.setTitle(doc.getTitle());
        dto.setDocType(doc.getDocType());
        dto.setContent(doc.getContent());
        dto.setFileUrl(doc.getFileUrl());
        dto.setFileName(doc.getFileName());
        dto.setFileSize(doc.getFileSize());
        dto.setFileType(doc.getFileType());
        dto.setLinkUrl(doc.getLinkUrl());
        dto.setLinkDesc(doc.getLinkDesc());
        dto.setTags(doc.getTags());
        dto.setViewCount(doc.getViewCount());
        dto.setSortOrder(doc.getSortOrder());
        dto.setStatus(doc.getStatus());
        dto.setCreateTime(doc.getCreateTime());
        dto.setUpdateTime(doc.getUpdateTime());
        // 查询所属知识库名称
        KnowledgeBase base = knowledgeBaseRepository.findById(doc.getBaseId());
        if (base != null) {
            dto.setBaseName(base.getName());
        }
        return dto;
    }

    private void copyRequestToDoc(KnowledgeDocumentRequest request, KnowledgeDocument doc) {
        if (request.getBaseId() != null) doc.setBaseId(request.getBaseId());
        if (request.getTitle() != null) doc.setTitle(request.getTitle());
        if (request.getDocType() != null) doc.setDocType(request.getDocType());
        doc.setContent(request.getContent());
        doc.setFileUrl(request.getFileUrl());
        doc.setFileName(request.getFileName());
        doc.setFileSize(request.getFileSize());
        doc.setFileType(request.getFileType());
        doc.setLinkUrl(request.getLinkUrl());
        doc.setLinkDesc(request.getLinkDesc());
        doc.setTags(request.getTags());
        if (request.getSortOrder() != null) doc.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) doc.setStatus(request.getStatus());
    }
}
