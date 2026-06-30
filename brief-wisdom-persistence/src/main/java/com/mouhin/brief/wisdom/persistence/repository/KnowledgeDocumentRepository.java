package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.KnowledgeDocumentMapper;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    public List<KnowledgeDocument> findByBaseId(Long baseId) {
        return knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getBaseId, baseId)
                        .orderByAsc(KnowledgeDocument::getSortOrder)
                        .orderByDesc(KnowledgeDocument::getUpdateTime)
        );
    }

    public Page<KnowledgeDocument> findByBaseIdPaged(Long baseId, int page, int size) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getBaseId, baseId)
                .orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return knowledgeDocumentMapper.selectPage(pageParam, queryWrapper);
    }

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

    public KnowledgeDocument findById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    public void save(KnowledgeDocument document) {
        knowledgeDocumentMapper.insert(document);
    }

    public void update(KnowledgeDocument document) {
        knowledgeDocumentMapper.updateById(document);
    }

    public void deleteById(Long id) {
        knowledgeDocumentMapper.deleteById(id);
    }

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
