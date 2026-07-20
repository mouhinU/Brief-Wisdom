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

    /**
     * 查询所有知识库（按排序字段升序）
     *
     * @return 知识库列表
     */
    public List<KnowledgeBase> findAll() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .orderByAsc(KnowledgeBase::getSortOrder)
        );
    }

    /**
     * 查询指定父知识库下的子知识库
     *
     * @param parentId 父知识库 ID
     * @return 子知识库列表
     */
    public List<KnowledgeBase> findByParentId(Long parentId) {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getParentId, parentId)
                        .orderByAsc(KnowledgeBase::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询知识库
     *
     * @param id 知识库 ID
     * @return 匹配的知识库，不存在返回 null
     */
    public KnowledgeBase findById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    /**
     * 保存新知识库
     *
     * @param base 知识库实体
     */
    public void save(KnowledgeBase base) {
        knowledgeBaseMapper.insert(base);
    }

    /**
     * 更新知识库
     *
     * @param base 知识库实体
     */
    public void update(KnowledgeBase base) {
        knowledgeBaseMapper.updateById(base);
    }

    /**
     * 根据 ID 删除知识库
     *
     * @param id 知识库 ID
     */
    public void deleteById(Long id) {
        knowledgeBaseMapper.deleteById(id);
    }

    /**
     * 统计指定父知识库下的子知识库数量
     *
     * @param parentId 父知识库 ID
     * @return 子知识库数量
     */
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
