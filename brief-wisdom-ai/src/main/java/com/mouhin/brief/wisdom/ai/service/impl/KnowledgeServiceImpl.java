package com.mouhin.brief.wisdom.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    @Override
    public List<KnowledgeBaseDTO> listBasesTree() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        return buildTree(allBases, 0L);
    }

    /**
     * 获取所有知识库（平铺列表，含文档数量）
     */
    @Override
    public List<KnowledgeBaseDTO> listBases() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        // 批量查询文档数量和子知识库状态，避免 N+1
        Map<Long, Long> docCountMap = buildDocCountMap(allBases);
        Map<Long, Boolean> hasChildrenMap = buildHasChildrenMap(allBases);
        return allBases.stream().map(base -> toBaseDTO(base, docCountMap, hasChildrenMap)).toList();
    }

    /**
     * 分页查询顶级知识库
     */
    @Override
    public Page<KnowledgeBaseDTO> listTopBasesPaged(int page, int size) {
        Page<KnowledgeBase> basePage = knowledgeBaseRepository.findTopLevelPaged(page, size);
        Page<KnowledgeBaseDTO> dtoPage = new Page<>(basePage.getCurrent(), basePage.getSize(), basePage.getTotal());
        dtoPage.setRecords(basePage.getRecords().stream().map(this::toBaseDTO).toList());
        return dtoPage;
    }

    /**
     * 获取子知识库
     */
    @Override
    public List<KnowledgeBaseDTO> listChildBases(Long parentId) {
        List<KnowledgeBase> bases = knowledgeBaseRepository.findByParentId(parentId);
        return bases.stream().map(this::toBaseDTO).toList();
    }

    /**
     * 创建知识库
     */
    @Override
    public KnowledgeBaseDTO createBase(KnowledgeBaseBO bo) {
        KnowledgeBase base = new KnowledgeBase();
        base.setName(bo.getName());
        base.setDescription(bo.getDescription());
        base.setIcon(bo.getIcon() != null ? bo.getIcon() : "📚");
        base.setParentId(bo.getParentId() != null ? bo.getParentId() : 0L);
        base.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : 0);
        base.setIsPublic(bo.getIsPublic() != null ? bo.getIsPublic() : 0);
        knowledgeBaseRepository.save(base);
        return toBaseDTO(base);
    }

    /**
     * 更新知识库
     */
    @Override
    public KnowledgeBaseDTO updateBase(Long id, KnowledgeBaseBO bo) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id);
        if (base == null) {
            throw new AIException("知识库不存在: " + id);
        }
        if (bo.getName() != null) { base.setName(bo.getName()); }
        if (bo.getDescription() != null) { base.setDescription(bo.getDescription()); }
        if (bo.getIcon() != null) { base.setIcon(bo.getIcon()); }
        if (bo.getParentId() != null) { base.setParentId(bo.getParentId()); }
        if (bo.getSortOrder() != null) { base.setSortOrder(bo.getSortOrder()); }
        if (bo.getIsPublic() != null) { base.setIsPublic(bo.getIsPublic()); }
        knowledgeBaseRepository.update(base);
        return toBaseDTO(base);
    }

    /**
     * 删除知识库（同时删除其下所有文档）
     */
    @Override
    @Transactional
    public void deleteBase(Long id) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id);
        if (base == null) {
            throw new AIException("知识库不存在: " + id);
        }
        // 检查是否有子知识库
        long childCount = knowledgeBaseRepository.countByParentId(id);
        if (childCount > 0) {
            throw new AIException("该知识库下还有子知识库，请先删除子知识库");
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
    @Override
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
    @Override
    public KnowledgeDocumentDTO getDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new AIException("文档不存在: " + id);
        }
        // 增加浏览次数
        knowledgeDocumentRepository.incrementViewCount(id);
        return toDocDTO(doc);
    }

    /**
     * 创建文档
     */
    @Override
    public KnowledgeDocumentDTO createDocument(KnowledgeDocumentBO bo) {
        KnowledgeDocument doc = new KnowledgeDocument();
        copyBoToDoc(bo, doc);
        doc.setViewCount(0);
        knowledgeDocumentRepository.save(doc);
        return toDocDTO(doc);
    }

    /**
     * 更新文档
     */
    @Override
    public KnowledgeDocumentDTO updateDocument(Long id, KnowledgeDocumentBO bo) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new AIException("文档不存在: " + id);
        }
        copyBoToDoc(bo, doc);
        knowledgeDocumentRepository.update(doc);
        return toDocDTO(doc);
    }

    /**
     * 删除文档
     */
    @Override
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
        if (doc == null) {
            throw new AIException("文档不存在: " + id);
        }
        knowledgeDocumentRepository.deleteById(id);
    }

    /**
     * 搜索文档
     */
    @Override
    public Page<KnowledgeDocumentDTO> searchDocuments(String keyword, int page, int size) {
        Page<KnowledgeDocument> docPage = knowledgeDocumentRepository.searchByTitle(keyword, page, size);
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        dtoPage.setRecords(docPage.getRecords().stream().map(this::toDocDTO).toList());
        return dtoPage;
    }

    /**
     * 按 Markdown 导入源路径 upsert：已存在则更新内容与元数据，不存在则新增
     */
    @Override
    public boolean upsertImportedMarkdown(KnowledgeDocumentBO bo, String sourcePath) {
        KnowledgeDocument existing = knowledgeDocumentRepository.findByBaseIdAndFileName(bo.getBaseId(), sourcePath);
        if (existing == null && bo.getTitle() != null) {
            existing = knowledgeDocumentRepository.findLegacyImportedByTitle(
                    bo.getBaseId(), bo.getTitle(), bo.getDocType());
        }

        if (existing != null) {
            copyBoToDoc(bo, existing);
            existing.setFileName(sourcePath);
            knowledgeDocumentRepository.update(existing);
            return false;
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        copyBoToDoc(bo, doc);
        doc.setFileName(sourcePath);
        doc.setViewCount(0);
        knowledgeDocumentRepository.save(doc);
        return true;
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
        return toBaseDTO(base, null, null);
    }

    private KnowledgeBaseDTO toBaseDTO(KnowledgeBase base, Map<Long, Long> docCountMap, Map<Long, Boolean> hasChildrenMap) {
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
        // 使用批量查询结果或单独查询
        if (docCountMap != null) {
            dto.setDocumentCount(docCountMap.getOrDefault(base.getId(), 0L));
        } else {
            dto.setDocumentCount(knowledgeDocumentRepository.countByBaseId(base.getId()));
        }
        if (hasChildrenMap != null) {
            dto.setHasChildren(hasChildrenMap.getOrDefault(base.getId(), false));
        } else {
            dto.setHasChildren(knowledgeBaseRepository.countByParentId(base.getId()) > 0);
        }
        return dto;
    }

    /**
     * 批量构建知识库文档数量映射（指定初始容量避免扩容）
     */
    private Map<Long, Long> buildDocCountMap(List<KnowledgeBase> bases) {
        // 初始容量 = (元素个数 / 0.75) + 1，遵循 AGENTS.md 规范
        int initialCapacity = (int) (bases.size() / 0.75) + 1;
        Map<Long, Long> map = new HashMap<>(initialCapacity);
        for (KnowledgeBase base : bases) {
            map.put(base.getId(), knowledgeDocumentRepository.countByBaseId(base.getId()));
        }
        return map;
    }

    /**
     * 批量构建知识库是否有子节点映射（指定初始容量避免扩容）
     */
    private Map<Long, Boolean> buildHasChildrenMap(List<KnowledgeBase> bases) {
        // 初始容量 = (元素个数 / 0.75) + 1，遵循 AGENTS.md 规范
        int initialCapacity = (int) (bases.size() / 0.75) + 1;
        Map<Long, Boolean> map = new HashMap<>(initialCapacity);
        for (KnowledgeBase base : bases) {
            map.put(base.getId(), knowledgeBaseRepository.countByParentId(base.getId()) > 0);
        }
        return map;
    }

    private KnowledgeDocumentDTO toDocDTO(KnowledgeDocument doc) {
        return toDocDTO(doc, null);
    }

    private KnowledgeDocumentDTO toDocDTO(KnowledgeDocument doc, Map<Long, String> baseNameMap) {
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
        // 查询所属知识库名称（使用缓存或单独查询）
        if (baseNameMap != null && baseNameMap.containsKey(doc.getBaseId())) {
            dto.setBaseName(baseNameMap.get(doc.getBaseId()));
        } else if (baseNameMap == null) {
            KnowledgeBase base = knowledgeBaseRepository.findById(doc.getBaseId());
            if (base != null) {
                dto.setBaseName(base.getName());
            }
        }
        return dto;
    }

    private void copyBoToDoc(KnowledgeDocumentBO bo, KnowledgeDocument doc) {
        if (bo.getBaseId() != null) { doc.setBaseId(bo.getBaseId()); }
        if (bo.getTitle() != null) { doc.setTitle(bo.getTitle()); }
        if (bo.getDocType() != null) { doc.setDocType(bo.getDocType()); }
        doc.setContent(bo.getContent());
        doc.setFileUrl(bo.getFileUrl());
        doc.setFileName(bo.getFileName());
        doc.setFileSize(bo.getFileSize());
        doc.setFileType(bo.getFileType());
        doc.setLinkUrl(bo.getLinkUrl());
        doc.setLinkDesc(bo.getLinkDesc());
        doc.setTags(bo.getTags());
        if (bo.getSortOrder() != null) { doc.setSortOrder(bo.getSortOrder()); }
        if (bo.getStatus() != null) { doc.setStatus(bo.getStatus()); }
    }
}
