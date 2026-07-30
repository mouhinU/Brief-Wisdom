package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.KnowledgeBaseMapper;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 根据名称查询知识库
     *
     * @param name 知识库名称
     * @return 匹配的知识库，不存在返回 null
     */
    public KnowledgeBase findByName(String name) {
        return knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getName, name)
                        .last("LIMIT 1")
        );
    }

    /**
     * 按归属类型和归属ID查询知识库列表
     *
     * @param ownerType 归属类型（personal/team）
     * @param ownerId   归属ID
     * @return 匹配的知识库列表
     */
    public List<KnowledgeBase> findByOwner(String ownerType, String ownerId) {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerType, ownerType)
                        .eq(KnowledgeBase::getOwnerId, ownerId)
                        .orderByAsc(KnowledgeBase::getSortOrder)
        );
    }

    /**
     * 批量统计多个知识库的子知识库数量（单次查询，避免 N+1）
     *
     * @param parentIds 父知识库 ID 列表
     * @return parentId -> 子知识库数量 的映射
     */
    public Map<Long, Long> countByParentIdGrouped(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<KnowledgeBase> qw = new QueryWrapper<>();
        qw.select("parent_id, COUNT(*) AS cnt");
        qw.in("parent_id", parentIds);
        qw.groupBy("parent_id");
        List<Map<String, Object>> maps = knowledgeBaseMapper.selectMaps(qw);
        int initialCapacity = (int) (maps.size() / 0.75) + 1;
        Map<Long, Long> result = new HashMap<>(initialCapacity);
        for (Map<String, Object> map : maps) {
            Long parentId = ((Number) map.get("parent_id")).longValue();
            Long cnt = ((Number) map.get("cnt")).longValue();
            result.put(parentId, cnt);
        }
        return result;
    }

    /**
     * 查询所有公开知识库的 ID 列表
     *
     * @return 公开知识库 ID 列表
     */
    public List<Long> findIdsByIsPublic(Integer isPublic) {
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getIsPublic, isPublic);
        qw.select(KnowledgeBase::getId);
        return knowledgeBaseMapper.selectList(qw).stream()
                .map(KnowledgeBase::getId)
                .toList();
    }

    /**
     * 根据 ID 列表批量查询知识库
     *
     * @param ids 知识库 ID 列表
     * @return 匹配的知识库列表
     */
    public List<KnowledgeBase> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return knowledgeBaseMapper.selectBatchIds(ids);
    }
}
