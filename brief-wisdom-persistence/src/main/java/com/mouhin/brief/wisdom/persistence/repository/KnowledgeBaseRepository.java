package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.KnowledgeBaseMapper;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库数据访问层
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
}
