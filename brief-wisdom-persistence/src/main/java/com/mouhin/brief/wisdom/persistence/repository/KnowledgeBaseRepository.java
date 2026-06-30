package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.KnowledgeBaseMapper;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库数据访问层
 */
/**
 * KnowledgeBaseRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public List<KnowledgeBase> findAll() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .orderByAsc(KnowledgeBase::getSortOrder)
        );
    }

    public List<KnowledgeBase> findByParentId(Long parentId) {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getParentId, parentId)
                        .orderByAsc(KnowledgeBase::getSortOrder)
        );
    }

    public KnowledgeBase findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public void save(KnowledgeBase base) {
        knowledgeBaseMapper.insert(base);
    }

    public void update(KnowledgeBase base) {
        knowledgeBaseMapper.updateById(base);
    }

    public void deleteById(Long id) {
        knowledgeBaseMapper.deleteById(id);
    }

    public long countByParentId(Long parentId) {
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getParentId, parentId);
        return knowledgeBaseMapper.selectCount(qw);
    }

    /**
     * 分页查询顶级知识库
     */
    public Page<KnowledgeBase> findTopLevelPaged(int page, int size) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getParentId, 0L)
                .orderByAsc(KnowledgeBase::getSortOrder)
                .orderByDesc(KnowledgeBase::getCreateTime);
        return knowledgeBaseMapper.selectPage(pageParam, qw);
    }
}
