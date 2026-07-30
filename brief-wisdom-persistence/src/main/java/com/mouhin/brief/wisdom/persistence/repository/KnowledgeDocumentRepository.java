package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.KnowledgeDocumentMapper;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识文档数据访问层
 */

/**
 * KnowledgeDocumentRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentRepository {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 查询指定知识库下的所有文档（按排序升序、更新时间降序）
     *
     * @param baseId 知识库 ID
     * @return 文档列表
     */
    public List<KnowledgeDocument> findByBaseId(Long baseId) {
        return knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getBaseId, baseId)
                        .orderByAsc(KnowledgeDocument::getSortOrder)
                        .orderByDesc(KnowledgeDocument::getUpdateTime)
        );
    }

    /**
     * 分页查询指定知识库下的文档
     *
     * @param baseId 知识库 ID
     * @param page   页码（从 1 开始）
     * @param size   每页大小
     * @return 分页结果
     */
    public Page<KnowledgeDocument> findByBaseIdPaged(Long baseId, int page, int size) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getBaseId, baseId)
                .orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return knowledgeDocumentMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 分页查询指定知识库下指定类型的文档
     *
     * @param baseId  知识库 ID
     * @param docType 文档类型（可选）
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    public Page<KnowledgeDocument> findByBaseIdAndTypePaged(Long baseId, String docType, int page, int size) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getBaseId, baseId);
        if (docType != null && !docType.isBlank()) {
            queryWrapper.eq(KnowledgeDocument::getDocType, docType);
        }
        queryWrapper.orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return knowledgeDocumentMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 根据 ID 查询文档
     *
     * @param id 文档 ID
     * @return 匹配的文档，不存在返回 null
     */
    public KnowledgeDocument findById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    /**
     * 按知识库 ID 与导入源路径查询文档（Markdown 导入去重用）
     *
     * @param baseId   知识库 ID
     * @param fileName 相对项目根目录的源文件路径
     * @return 匹配的文档，不存在返回 null
     */
    public KnowledgeDocument findByBaseIdAndFileName(Long baseId, String fileName) {
        return knowledgeDocumentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getBaseId, baseId)
                        .eq(KnowledgeDocument::getFileName, fileName)
                        .last("LIMIT 1")
        );
    }

    /**
     * 按标题查询历史导入文档（兼容早期未写入 fileName 的记录）
     *
     * @param baseId  知识库 ID
     * @param title   文档标题
     * @param docType 文档类型
     * @return 匹配的文档，不存在返回 null
     */
    public KnowledgeDocument findLegacyImportedByTitle(Long baseId, String title, String docType) {
        return knowledgeDocumentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getBaseId, baseId)
                        .eq(KnowledgeDocument::getTitle, title)
                        .eq(KnowledgeDocument::getDocType, docType)
                        .and(wrapper -> wrapper.isNull(KnowledgeDocument::getFileName)
                                .or()
                                .eq(KnowledgeDocument::getFileName, ""))
                        .orderByDesc(KnowledgeDocument::getUpdateTime)
                        .last("LIMIT 1")
        );
    }

    /**
     * 根据 ID 批量查询文档
     *
     * @param ids 文档 ID 列表
     * @return 匹配的文档列表（不保证顺序）
     */
    public List<KnowledgeDocument> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return knowledgeDocumentMapper.selectBatchIds(ids);
    }

    /**
     * 保存新文档
     *
     * @param document 文档实体
     */
    public void save(KnowledgeDocument document) {
        knowledgeDocumentMapper.insert(document);
    }

    /**
     * 更新文档
     *
     * @param document 文档实体
     */
    public void update(KnowledgeDocument document) {
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 根据 ID 删除文档
     *
     * @param id 文档 ID
     */
    public void deleteById(Long id) {
        knowledgeDocumentMapper.deleteById(id);
    }

    /**
     * 统计指定知识库下的文档数量
     *
     * @param baseId 知识库 ID
     * @return 文档数量
     */
    public long countByBaseId(Long baseId) {
        LambdaQueryWrapper<KnowledgeDocument> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeDocument::getBaseId, baseId);
        return knowledgeDocumentMapper.selectCount(qw);
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount(Long id) {
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(id);
        if (doc != null) {
            doc.setViewCount(doc.getViewCount() + 1);
            knowledgeDocumentMapper.updateById(doc);
        }
    }

    /**
     * 查询所有文档（用于向量索引重建）
     *
     * @return 所有文档列表
     */
    public List<KnowledgeDocument> findAll() {
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 批量统计多个知识库下的文档数量（单次查询，避免 N+1）
     *
     * @param baseIds 知识库 ID 列表
     * @return baseId -> 文档数量 的映射
     */
    public Map<Long, Long> countByBaseIdGrouped(List<Long> baseIds) {
        if (baseIds == null || baseIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<KnowledgeDocument> qw = new QueryWrapper<>();
        qw.select("base_id, COUNT(*) AS cnt");
        qw.in("base_id", baseIds);
        qw.groupBy("base_id");
        List<Map<String, Object>> maps = knowledgeDocumentMapper.selectMaps(qw);
        int initialCapacity = (int) (maps.size() / 0.75) + 1;
        Map<Long, Long> result = new HashMap<>(initialCapacity);
        for (Map<String, Object> map : maps) {
            Long baseId = ((Number) map.get("base_id")).longValue();
            Long cnt = ((Number) map.get("cnt")).longValue();
            result.put(baseId, cnt);
        }
        return result;
    }

    /**
     * 搜索文档（按标题模糊查询）
     */
    public Page<KnowledgeDocument> searchByTitle(String keyword, int page, int size) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(KnowledgeDocument::getTitle, keyword)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return knowledgeDocumentMapper.selectPage(pageParam, queryWrapper);
    }
}
