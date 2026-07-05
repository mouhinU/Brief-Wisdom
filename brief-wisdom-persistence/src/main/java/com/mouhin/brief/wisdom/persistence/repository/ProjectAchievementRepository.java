package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.ProjectAchievementMapper;
import com.mouhin.brief.wisdom.persistence.model.ProjectAchievement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 项目成果数据访问层
 */
/**
 * ProjectAchievementRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class ProjectAchievementRepository {

    private final ProjectAchievementMapper projectAchievementMapper;

    /**
     * 查询所有项目成果（按排序字段升序）
     *
     * @return 项目成果列表
     */
    public List<ProjectAchievement> findAllOrderBySortOrderAsc() {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    /**
     * 查询指定项目下的成果（按排序字段升序）
     *
     * @param projectId 项目 ID
     * @return 项目成果列表
     */
    public List<ProjectAchievement> findByProjectIdOrderBySortOrderAsc(Long projectId) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .eq(ProjectAchievement::getProjectId, projectId)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    /**
     * 批量查询多个项目下的成果（按排序字段升序）
     *
     * @param projectIds 项目 ID 集合
     * @return 项目成果列表
     */
    public List<ProjectAchievement> findByProjectIdInOrderBySortOrderAsc(Collection<Long> projectIds) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .in(ProjectAchievement::getProjectId, projectIds)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询项目成果
     *
     * @param id 项目成果 ID
     * @return 匹配的项目成果，不存在返回 null
     */
    public ProjectAchievement findById(Long id) {
        return projectAchievementMapper.selectById(id);
    }

    /**
     * 保存新项目成果
     *
     * @param achievement 项目成果实体
     */
    public void save(ProjectAchievement achievement) {
        projectAchievementMapper.insert(achievement);
    }

    /**
     * 更新项目成果
     *
     * @param achievement 项目成果实体
     */
    public void update(ProjectAchievement achievement) {
        projectAchievementMapper.updateById(achievement);
    }

    /**
     * 根据 ID 删除项目成果
     *
     * @param id 项目成果 ID
     */
    public void deleteById(Long id) {
        projectAchievementMapper.deleteById(id);
    }
}
