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

    /**
     * 查询所有技术栈（按排序字段升序）
     *
     * @return 技术栈列表
     */
    public List<WorkExperienceStack> findAllOrderBySortOrderAsc() {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    /**
     * 查询指定工作经历下的技术栈（按排序字段升序）
     *
     * @param experienceId 工作经历 ID
     * @return 技术栈列表
     */
    public List<WorkExperienceStack> findByExperienceIdOrderBySortOrderAsc(Long experienceId) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .eq(WorkExperienceStack::getExperienceId, experienceId)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    /**
     * 批量查询多个工作经历下的技术栈（按排序字段升序）
     *
     * @param experienceIds 工作经历 ID 集合
     * @return 技术栈列表
     */
    public List<WorkExperienceStack> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .in(WorkExperienceStack::getExperienceId, experienceIds)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询技术栈
     *
     * @param id 技术栈 ID
     * @return 匹配的技术栈，不存在返回 null
     */
    public WorkExperienceStack findById(Long id) {
        return workExperienceStackMapper.selectById(id);
    }

    /**
     * 保存新技术栈
     *
     * @param stack 技术栈实体
     */
    public void save(WorkExperienceStack stack) {
        workExperienceStackMapper.insert(stack);
    }

    /**
     * 更新技术栈
     *
     * @param stack 技术栈实体
     */
    public void update(WorkExperienceStack stack) {
        workExperienceStackMapper.updateById(stack);
    }

    /**
     * 根据 ID 删除技术栈
     *
     * @param id 技术栈 ID
     */
    public void deleteById(Long id) {
        workExperienceStackMapper.deleteById(id);
    }
}
