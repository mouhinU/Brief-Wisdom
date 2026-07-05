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
/**
 * ProjectRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class ProjectRepository {

    private final ProjectMapper projectMapper;

    /**
     * 查询所有项目（按排序字段升序）
     *
     * @return 项目列表
     */
    public List<Project> findAllOrderBySortOrderAsc() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .orderByAsc(Project::getSortOrder)
        );
    }

    /**
     * 查询指定工作经历下的项目（按排序字段升序）
     *
     * @param experienceId 工作经历 ID
     * @return 项目列表
     */
    public List<Project> findByExperienceIdOrderBySortOrderAsc(Long experienceId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getExperienceId, experienceId)
                        .orderByAsc(Project::getSortOrder)
        );
    }

    /**
     * 批量查询多个工作经历下的项目（按排序字段升序）
     *
     * @param experienceIds 工作经历 ID 集合
     * @return 项目列表
     */
    public List<Project> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getExperienceId, experienceIds)
                        .orderByAsc(Project::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询项目
     *
     * @param id 项目 ID
     * @return 匹配的项目，不存在返回 null
     */
    public Project findById(Long id) {
        return projectMapper.selectById(id);
    }

    /**
     * 保存新项目
     *
     * @param project 项目实体
     */
    public void save(Project project) {
        projectMapper.insert(project);
    }

    /**
     * 更新项目
     *
     * @param project 项目实体
     */
    public void update(Project project) {
        projectMapper.updateById(project);
    }

    /**
     * 根据 ID 删除项目
     *
     * @param id 项目 ID
     */
    public void deleteById(Long id) {
        projectMapper.deleteById(id);
    }
}
