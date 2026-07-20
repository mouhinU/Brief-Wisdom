package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.WorkExperienceMapper;
import com.mouhin.brief.wisdom.persistence.model.WorkExperience;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工作经历数据访问层
 */

/**
 * WorkExperienceRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class WorkExperienceRepository {

    private final WorkExperienceMapper workExperienceMapper;

    /**
     * 查询所有可见的工作经历（按排序字段升序）
     *
     * @return 可见的工作经历列表
     */
    public List<WorkExperience> findVisibleOrderBySortOrderAsc() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .eq(WorkExperience::getIsVisible, 1)
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    /**
     * 查询所有工作经历（按排序字段升序，含隐藏）
     *
     * @return 工作经历列表
     */
    public List<WorkExperience> findAllOrderBySortOrderAsc() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    /**
     * 批量查询指定 ID 集合的工作经历（按排序字段升序）
     *
     * @param ids 工作经历 ID 集合
     * @return 工作经历列表
     */
    public List<WorkExperience> findByIdsOrderBySortOrderAsc(Collection<Long> ids) {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .in(WorkExperience::getId, ids)
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询工作经历
     *
     * @param id 工作经历 ID
     * @return 匹配的工作经历，不存在返回 null
     */
    public WorkExperience findById(Long id) {
        return workExperienceMapper.selectById(id);
    }

    /**
     * 保存新工作经历
     *
     * @param experience 工作经历实体
     */
    public void save(WorkExperience experience) {
        workExperienceMapper.insert(experience);
    }

    /**
     * 更新工作经历
     *
     * @param experience 工作经历实体
     */
    public void update(WorkExperience experience) {
        workExperienceMapper.updateById(experience);
    }

    /**
     * 根据 ID 删除工作经历
     *
     * @param id 工作经历 ID
     */
    public void deleteById(Long id) {
        workExperienceMapper.deleteById(id);
    }
}
