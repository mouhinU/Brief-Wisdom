package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.ProjectMapper;
import com.mouhin.brief.wisdom.persistence.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 项目数据访问层
 */
@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper projectMapper;

    public List<Project> findAllOrderBySortOrderAsc() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .orderByAsc(Project::getSortOrder)
        );
    }

    public List<Project> findByExperienceIdOrderBySortOrderAsc(Long experienceId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getExperienceId, experienceId)
                        .orderByAsc(Project::getSortOrder)
        );
    }

    public List<Project> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getExperienceId, experienceIds)
                        .orderByAsc(Project::getSortOrder)
        );
    }

    public Project findById(Long id) {
        return projectMapper.selectById(id);
    }

    public void save(Project project) {
        projectMapper.insert(project);
    }

    public void update(Project project) {
        projectMapper.updateById(project);
    }

    public void deleteById(Long id) {
        projectMapper.deleteById(id);
    }
}
