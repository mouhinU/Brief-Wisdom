package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.WorkExperienceStackMapper;
import com.mouhin.brief.wisdom.persistence.model.WorkExperienceStack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工作经历技术栈数据访问层
 */
/**
 * WorkExperienceStackRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class WorkExperienceStackRepository {

    private final WorkExperienceStackMapper workExperienceStackMapper;

    public List<WorkExperienceStack> findAllOrderBySortOrderAsc() {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    public List<WorkExperienceStack> findByExperienceIdOrderBySortOrderAsc(Long experienceId) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .eq(WorkExperienceStack::getExperienceId, experienceId)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    public List<WorkExperienceStack> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .in(WorkExperienceStack::getExperienceId, experienceIds)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    public WorkExperienceStack findById(Long id) {
        return workExperienceStackMapper.selectById(id);
    }

    public void save(WorkExperienceStack stack) {
        workExperienceStackMapper.insert(stack);
    }

    public void update(WorkExperienceStack stack) {
        workExperienceStackMapper.updateById(stack);
    }

    public void deleteById(Long id) {
        workExperienceStackMapper.deleteById(id);
    }
}
