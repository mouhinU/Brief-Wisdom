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
@Repository
@RequiredArgsConstructor
public class ProjectAchievementRepository {

    private final ProjectAchievementMapper projectAchievementMapper;

    public List<ProjectAchievement> findAllOrderBySortOrderAsc() {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    public List<ProjectAchievement> findByProjectIdOrderBySortOrderAsc(Long projectId) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .eq(ProjectAchievement::getProjectId, projectId)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    public List<ProjectAchievement> findByProjectIdInOrderBySortOrderAsc(Collection<Long> projectIds) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .in(ProjectAchievement::getProjectId, projectIds)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    public ProjectAchievement findById(Long id) {
        return projectAchievementMapper.selectById(id);
    }

    public void save(ProjectAchievement achievement) {
        projectAchievementMapper.insert(achievement);
    }

    public void update(ProjectAchievement achievement) {
        projectAchievementMapper.updateById(achievement);
    }

    public void deleteById(Long id) {
        projectAchievementMapper.deleteById(id);
    }
}
