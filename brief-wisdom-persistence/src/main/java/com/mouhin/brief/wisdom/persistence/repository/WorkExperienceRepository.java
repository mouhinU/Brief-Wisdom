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

    public List<WorkExperience> findVisibleOrderBySortOrderAsc() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .eq(WorkExperience::getIsVisible, 1)
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    public List<WorkExperience> findAllOrderBySortOrderAsc() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    public List<WorkExperience> findByIdsOrderBySortOrderAsc(Collection<Long> ids) {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .in(WorkExperience::getId, ids)
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    public WorkExperience findById(Long id) {
        return workExperienceMapper.selectById(id);
    }

    public void save(WorkExperience experience) {
        workExperienceMapper.insert(experience);
    }

    public void update(WorkExperience experience) {
        workExperienceMapper.updateById(experience);
    }

    public void deleteById(Long id) {
        workExperienceMapper.deleteById(id);
    }
}
