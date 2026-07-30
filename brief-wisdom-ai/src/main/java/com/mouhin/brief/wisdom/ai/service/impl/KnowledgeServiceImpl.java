package com.mouhin.brief.wisdom.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
import com.mouhin.brief.wisdom.ai.service.KnowledgeVectorService;
import com.mouhin.brief.wisdom.ai.service.TeamService;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.persistence.model.KbTeam;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import com.mouhin.brief.wisdom.persistence.model.Team;
import com.mouhin.brief.wisdom.persistence.repository.KbTeamRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeDocumentRepository;
import com.mouhin.brief.wisdom.persistence.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

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
    private final KnowledgeVectorService knowledgeVectorService;
    private final TeamService teamService;
    private final KbTeamRepository kbTeamRepository;
    private final TeamRepository teamRepository;
    @Qualifier("briefWisdomExecutor")
    private final Executor briefWisdomExecutor;

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    @Override
    public List<KnowledgeBaseDTO> listBasesTree() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        return buildTree(allBases, 0L, buildKbTeamInfo(allBases));
    }

    /**
     * 获取用户可见的知识库（树形结构，按权限过滤）
     */
    @Override
    public List<KnowledgeBaseDTO> listBasesTree(String userId) {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        Set<Long> visibleIds = teamService.getVisibleKbIds(userId);
        List<KnowledgeBase> visibleBases = allBases.stream()
                .filter(base -> visibleIds.contains(base.getId()))
                .toList();
        return buildTree(visibleBases, 0L, buildKbTeamInfo(visibleBases));
    }

    /**
     * 获取所有知识库（平铺列表，含文档数量）
     */
    @Override
    public List<KnowledgeBaseDTO> listBases() {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        // 批量查询文档数量、子知识库状态和团队归属，避免 N+1
        Map<Long, Long> docCountMap = buildDocCountMap(allBases);
        Map<Long, Boolean> hasChildrenMap = buildHasChildrenMap(allBases);
        KbTeamInfo teamInfo = buildKbTeamInfo(allBases);
        return allBases.stream().map(base -> toBaseDTO(base, docCountMap, hasChildrenMap, teamInfo)).toList();
    }

    /**
     * 获取用户可见的知识库（平铺列表，按权限过滤，含文档数量）
     */
    @Override
    public List<KnowledgeBaseDTO> listBases(String userId) {
        List<KnowledgeBase> allBases = knowledgeBaseRepository.findAll();
        Set<Long> visibleIds = teamService.getVisibleKbIds(userId);
        List<KnowledgeBase> visibleBases = allBases.stream()
                .filter(base -> visibleIds.contains(base.getId()))
                .toList();
        Map<Long, Long> docCountMap = buildDocCountMap(visibleBases);
        Map<Long, Boolean> hasChildrenMap = buildHasChildrenMap(visibleBases);
        KbTeamInfo teamInfo = buildKbTeamInfo(visibleBases);
        return visibleBases.stream().map(base -> toBaseDTO(base, docCountMap, hasChildrenMap, teamInfo)).toList();
    }

    /**
     * 分页查询顶级知识库
     */
    @Override
    public Page<KnowledgeBaseDTO> listTopBasesPaged(int page, int size) {
        Page<KnowledgeBase> basePage = knowledgeBaseRepository.findTopLevelPaged(page, size);
        Page<KnowledgeBaseDTO> dtoPage = new Page<>(basePage.getCurrent(), basePage.getSize(), basePage.getTotal());
        KbTeamInfo teamInfo = buildKbTeamInfo(basePage.getRecords());
        dtoPage.setRecords(basePage.getRecords().stream()
                .map(base -> toBaseDTO(base, null, null, teamInfo)).toList());
        return dtoPage;
    }

    /**
     * 获取子知识库
     */
    @Override
    public List<KnowledgeBaseDTO> listChildBases(Long parentId) {
        List<KnowledgeBase> bases = knowledgeBaseRepository.findByParentId(parentId);
        KbTeamInfo teamInfo = buildKbTeamInfo(bases);
        return bases.stream().map(base -> toBaseDTO(base, null, null, teamInfo)).toList();
    }

    /**
     * 创建知识库（支持多团队归属）
     */
    @Override
    @Transactional
    public KnowledgeBaseDTO createBase(KnowledgeBaseBO bo) {
        KnowledgeBase base = new KnowledgeBase();
        base.setName(bo.getName());
        base.setDescription(bo.getDescription());
        base.setIcon(bo.getIcon() != null ? bo.getIcon() : "📚");
        base.setParentId(bo.getParentId() != null ? bo.getParentId() : 0L);
        base.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : 0);
        base.setIsPublic(bo.getIsPublic() != null ? bo.getIsPublic() : 0);
        base.setCreatorId(bo.getCreatorId());

        List<Long> teamIds = bo.getTeamIds();
        if (teamIds != null && !teamIds.isEmpty()) {
            // 团队知识库：关联一个或多个团队
            base.setOwnerType("team");
            base.setOwnerId(String.valueOf(teamIds.get(0)));
        } else {
            // 个人知识库（无团队属性）
            base.setOwnerType("personal");
            base.setOwnerId(bo.getOwnerId() != null ? bo.getOwnerId() : bo.getCreatorId());
        }
        knowledgeBaseRepository.save(base);

        // 写入知识库-团队关联表
        if (teamIds != null && !teamIds.isEmpty()) {
            saveKbTeamLinks(base.getId(), teamIds);
        }
        return toBaseDTO(base);
    }

    /**
     * 更新知识库（teamIds 非 null 时重置团队归属）
     */
    @Override
    @Transactional
    public KnowledgeBaseDTO updateBase(Long id, KnowledgeBaseBO bo) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id);
        if (base == null) {
            throw new AIException("知识库不存在: " + id);
        }
        if (bo.getName() != null) {
            base.setName(bo.getName());
        }
        if (bo.getDescription() != null) {
            base.setDescription(bo.getDescription());
        }
        if (bo.getIcon() != null) {
            base.setIcon(bo.getIcon());
        }
        if (bo.getParentId() != null) {
            base.setParentId(bo.getParentId());
        }
        if (bo.getSortOrder() != null) {
            base.setSortOrder(bo.getSortOrder());
        }
        if (bo.getIsPublic() != null) {
            base.setIsPublic(bo.getIsPublic());
        }
        // teamIds 为 null 表示不修改团队归属；非 null 表示重置
        if (bo.getTeamIds() != null) {
            kbTeamRepository.deleteByKbId(id);
            if (bo.getTeamIds().isEmpty()) {
                // 移除所有团队归属，转为个人知识库
                base.setOwnerType("personal");
                base.setOwnerId(base.getCreatorId());
            } else {
                base.setOwnerType("team");
                base.setOwnerId(String.valueOf(bo.getTeamIds().get(0)));
                saveKbTeamLinks(id, bo.getTeamIds());
            }
        }
        knowledgeBaseRepository.update(base);
        return toBaseDTO(base);
    }

    /**
     * 删除知识库（若知识库下还有文档则拒绝删除）
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
        // 检查是否有文档，有文档时拒绝删除
        long docCount = knowledgeDocumentRepository.countByBaseId(id);
        if (docCount > 0) {
            throw new AIException("该知识库下还有 " + docCount + " 个文档，请先删除知识库下的所有文档");
        }
        // 清理团队关联记录
        kbTeamRepository.deleteByKbId(id);
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
        // 同一知识库下的文档共享 baseName，只查询一次
        Map<Long, String> baseNameMap = buildBaseNameMap(List.of(baseId));
        // 转换为 DTO 分页
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        dtoPage.setRecords(docPage.getRecords().stream().map(doc -> toDocDTO(doc, baseNameMap)).toList());
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
        // 异步向量化新文档
        vectorizeDocument(doc);
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
        // 重新向量化更新后的文档
        vectorizeDocument(doc);
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
        // 删除对应的向量索引
        knowledgeVectorService.deleteFromVectorStore(id);
    }

    /**
     * 批量删除文档
     */
    @Override
    @Transactional
    public int batchDeleteDocuments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int deletedCount = 0;
        for (Long id : ids) {
            KnowledgeDocument doc = knowledgeDocumentRepository.findById(id);
            if (doc != null) {
                knowledgeDocumentRepository.deleteById(id);
                // 同步删除向量索引
                knowledgeVectorService.deleteFromVectorStore(id);
                deletedCount++;
            }
        }
        log.info("批量删除文档完成 - 请求: {} 个, 实际删除: {} 个", ids.size(), deletedCount);
        return deletedCount;
    }

    /**
     * 搜索文档
     */
    @Override
    public Page<KnowledgeDocumentDTO> searchDocuments(String keyword, int page, int size) {
        Page<KnowledgeDocument> docPage = knowledgeDocumentRepository.searchByTitle(keyword, page, size);
        // 批量获取涉及的知识库名称
        List<Long> baseIds = docPage.getRecords().stream()
                .map(KnowledgeDocument::getBaseId)
                .distinct()
                .toList();
        Map<Long, String> baseNameMap = buildBaseNameMap(baseIds);
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        dtoPage.setRecords(docPage.getRecords().stream().map(doc -> toDocDTO(doc, baseNameMap)).toList());
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
            // 重新向量化更新后的文档
            vectorizeDocument(existing);
            return false;
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        copyBoToDoc(bo, doc);
        doc.setFileName(sourcePath);
        doc.setViewCount(0);
        knowledgeDocumentRepository.save(doc);
        // 向量化新导入的文档
        vectorizeDocument(doc);
        return true;
    }

    // ==================== 私有方法 ====================

    /**
     * 异步向量化文档（失败不影响主流程）
     */
    private void vectorizeDocument(KnowledgeDocument doc) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                knowledgeVectorService.vectorizeAndStore(doc);
            } catch (Exception e) {
                log.warn("文档向量化失败（不影响主流程）: docId={}, error={}", doc.getId(), e.getMessage());
            }
        }, briefWisdomExecutor);
    }

    /**
     * 构建知识库树形结构
     */
    private List<KnowledgeBaseDTO> buildTree(List<KnowledgeBase> allBases, Long parentId, KbTeamInfo teamInfo) {
        List<KnowledgeBaseDTO> tree = new ArrayList<>();
        for (KnowledgeBase base : allBases) {
            if (parentId.equals(base.getParentId())) {
                KnowledgeBaseDTO dto = toBaseDTO(base, null, null, teamInfo);
                dto.setChildren(buildTree(allBases, base.getId(), teamInfo));
                tree.add(dto);
            }
        }
        return tree;
    }

    private KnowledgeBaseDTO toBaseDTO(KnowledgeBase base) {
        return toBaseDTO(base, null, null, buildKbTeamInfo(List.of(base)));
    }

    private KnowledgeBaseDTO toBaseDTO(KnowledgeBase base, Map<Long, Long> docCountMap,
                                       Map<Long, Boolean> hasChildrenMap, KbTeamInfo teamInfo) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(base.getId());
        dto.setName(base.getName());
        dto.setDescription(base.getDescription());
        dto.setIcon(base.getIcon());
        dto.setParentId(base.getParentId());
        dto.setSortOrder(base.getSortOrder());
        dto.setIsPublic(base.getIsPublic());
        dto.setOwnerType(base.getOwnerType());
        dto.setOwnerId(base.getOwnerId());
        dto.setCreatorId(base.getCreatorId());
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
        // 填充团队归属信息
        List<Long> teamIds = teamInfo.kbTeamIds().getOrDefault(base.getId(), List.of());
        dto.setTeamIds(teamIds);
        dto.setTeamNames(teamIds.stream()
                .map(teamId -> teamInfo.teamNames().getOrDefault(teamId, ""))
                .toList());
        return dto;
    }

    /**
     * 知识库团队归属批量信息（kbId -> 团队ID列表，teamId -> 团队名称）
     */
    private record KbTeamInfo(Map<Long, List<Long>> kbTeamIds, Map<Long, String> teamNames) {
    }

    /**
     * 批量构建知识库团队归属信息（单次查询关联表 + 单次查询团队表，避免 N+1）
     */
    private KbTeamInfo buildKbTeamInfo(List<KnowledgeBase> bases) {
        if (bases.isEmpty()) {
            return new KbTeamInfo(Map.of(), Map.of());
        }
        List<Long> kbIds = bases.stream().map(KnowledgeBase::getId).toList();
        List<KbTeam> links = kbTeamRepository.findByKbIds(kbIds);
        int linkCapacity = (int) (links.size() / 0.75) + 1;
        Map<Long, List<Long>> kbTeamIds = new HashMap<>(linkCapacity);
        for (KbTeam link : links) {
            kbTeamIds.computeIfAbsent(link.getKbId(), k -> new ArrayList<>()).add(link.getTeamId());
        }
        List<Long> teamIds = links.stream().map(KbTeam::getTeamId).distinct().toList();
        Map<Long, String> teamNames;
        if (teamIds.isEmpty()) {
            teamNames = Map.of();
        } else {
            List<Team> teams = teamRepository.findByIds(teamIds);
            int nameCapacity = (int) (teams.size() / 0.75) + 1;
            teamNames = new HashMap<>(nameCapacity);
            for (Team team : teams) {
                teamNames.put(team.getId(), team.getName());
            }
        }
        return new KbTeamInfo(kbTeamIds, teamNames);
    }

    /**
     * 保存知识库与团队的关联记录
     */
    private void saveKbTeamLinks(Long kbId, List<Long> teamIds) {
        for (Long teamId : teamIds) {
            KbTeam link = new KbTeam();
            link.setKbId(kbId);
            link.setTeamId(teamId);
            kbTeamRepository.save(link);
        }
    }

    /**
     * 批量构建知识库文档数量映射（单次 GROUP BY 查询，避免 N+1）
     */
    private Map<Long, Long> buildDocCountMap(List<KnowledgeBase> bases) {
        if (bases.isEmpty()) {
            return Map.of();
        }
        List<Long> baseIds = bases.stream().map(KnowledgeBase::getId).toList();
        return knowledgeDocumentRepository.countByBaseIdGrouped(baseIds);
    }

    /**
     * 批量构建知识库是否有子节点映射（单次 GROUP BY 查询，避免 N+1）
     */
    private Map<Long, Boolean> buildHasChildrenMap(List<KnowledgeBase> bases) {
        if (bases.isEmpty()) {
            return Map.of();
        }
        List<Long> baseIds = bases.stream().map(KnowledgeBase::getId).toList();
        Map<Long, Long> countMap = knowledgeBaseRepository.countByParentIdGrouped(baseIds);
        int initialCapacity = (int) (bases.size() / 0.75) + 1;
        Map<Long, Boolean> result = new HashMap<>(initialCapacity);
        for (KnowledgeBase base : bases) {
            result.put(base.getId(), countMap.getOrDefault(base.getId(), 0L) > 0);
        }
        return result;
    }

    /**
     * 批量构建知识库 ID -> 名称映射（避免 toDocDTO 中逐条查询）
     */
    private Map<Long, String> buildBaseNameMap(List<Long> baseIds) {
        if (baseIds == null || baseIds.isEmpty()) {
            return Map.of();
        }
        List<KnowledgeBase> bases = knowledgeBaseRepository.findByIds(baseIds);
        int initialCapacity = (int) (bases.size() / 0.75) + 1;
        Map<Long, String> map = new HashMap<>(initialCapacity);
        for (KnowledgeBase base : bases) {
            map.put(base.getId(), base.getName());
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
        if (bo.getBaseId() != null) {
            doc.setBaseId(bo.getBaseId());
        }
        if (bo.getTitle() != null) {
            doc.setTitle(bo.getTitle());
        }
        if (bo.getDocType() != null) {
            doc.setDocType(bo.getDocType());
        }
        doc.setContent(bo.getContent());
        doc.setFileUrl(bo.getFileUrl());
        doc.setFileName(bo.getFileName());
        doc.setFileSize(bo.getFileSize());
        doc.setFileType(bo.getFileType());
        doc.setLinkUrl(bo.getLinkUrl());
        doc.setLinkDesc(bo.getLinkDesc());
        doc.setTags(bo.getTags());
        if (bo.getSortOrder() != null) {
            doc.setSortOrder(bo.getSortOrder());
        }
        if (bo.getStatus() != null) {
            doc.setStatus(bo.getStatus());
        }
    }
}
