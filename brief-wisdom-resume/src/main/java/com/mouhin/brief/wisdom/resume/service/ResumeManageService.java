package com.mouhin.brief.wisdom.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.common.resume.ProjectAchievementDTO;
import com.mouhin.brief.wisdom.common.resume.ProjectDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceStackDTO;
import com.mouhin.brief.wisdom.persistence.mapper.*;
import com.mouhin.brief.wisdom.persistence.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 简历数据管理服务（CRUD）
 */
@Service
@RequiredArgsConstructor
public class ResumeManageService {

    private final WorkExperienceMapper workExperienceMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAchievementMapper projectAchievementMapper;
    private final WorkExperienceStackMapper workExperienceStackMapper;

    // ========== 工作经历 ==========

    public List<WorkExperienceDTO> listExperiences() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .orderByAsc(WorkExperience::getSortOrder)
        ).stream().map(this::toExpDTO).toList();
    }

    public WorkExperienceDTO getExperience(Long id) {
        WorkExperience e = workExperienceMapper.selectById(id);
        return e != null ? toExpDTO(e) : null;
    }

    @Transactional
    public WorkExperienceDTO createExperience(WorkExperience experience) {
        workExperienceMapper.insert(experience);
        return toExpDTO(experience);
    }

    @Transactional
    public WorkExperienceDTO updateExperience(WorkExperience experience) {
        workExperienceMapper.updateById(experience);
        return toExpDTO(workExperienceMapper.selectById(experience.getId()));
    }

    @Transactional
    public void deleteExperience(Long id) {
        workExperienceMapper.deleteById(id);
    }

    // ========== 项目 ==========

    public List<ProjectDTO> listProjects() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .orderByAsc(Project::getSortOrder)
        ).stream().map(this::toProjDTO).toList();
    }

    public List<ProjectDTO> listProjectsByExperienceId(Long experienceId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getExperienceId, experienceId)
                        .orderByAsc(Project::getSortOrder)
        ).stream().map(this::toProjDTO).toList();
    }

    public ProjectDTO getProject(Long id) {
        Project p = projectMapper.selectById(id);
        return p != null ? toProjDTO(p) : null;
    }

    @Transactional
    public ProjectDTO createProject(Project project) {
        projectMapper.insert(project);
        return toProjDTO(project);
    }

    @Transactional
    public ProjectDTO updateProject(Project project) {
        projectMapper.updateById(project);
        return toProjDTO(projectMapper.selectById(project.getId()));
    }

    @Transactional
    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
    }

    // ========== 项目成果 ==========

    public List<ProjectAchievementDTO> listAchievements() {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .orderByAsc(ProjectAchievement::getSortOrder)
        ).stream().map(this::toAchDTO).toList();
    }

    public List<ProjectAchievementDTO> listAchievementsByProjectId(Long projectId) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .eq(ProjectAchievement::getProjectId, projectId)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        ).stream().map(this::toAchDTO).toList();
    }

    public ProjectAchievementDTO getAchievement(Long id) {
        ProjectAchievement a = projectAchievementMapper.selectById(id);
        return a != null ? toAchDTO(a) : null;
    }

    @Transactional
    public ProjectAchievementDTO createAchievement(ProjectAchievement achievement) {
        projectAchievementMapper.insert(achievement);
        return toAchDTO(achievement);
    }

    @Transactional
    public ProjectAchievementDTO updateAchievement(ProjectAchievement achievement) {
        projectAchievementMapper.updateById(achievement);
        return toAchDTO(projectAchievementMapper.selectById(achievement.getId()));
    }

    @Transactional
    public void deleteAchievement(Long id) {
        projectAchievementMapper.deleteById(id);
    }

    // ========== 技术栈 ==========

    public List<WorkExperienceStackDTO> listStacks() {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        ).stream().map(this::toStackDTO).toList();
    }

    public List<WorkExperienceStackDTO> listStacksByExperienceId(Long experienceId) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .in(WorkExperienceStack::getExperienceId, experienceId)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        ).stream().map(this::toStackDTO).toList();
    }

    public WorkExperienceStackDTO getStack(Long id) {
        WorkExperienceStack s = workExperienceStackMapper.selectById(id);
        return s != null ? toStackDTO(s) : null;
    }

    @Transactional
    public WorkExperienceStackDTO createStack(WorkExperienceStack stack) {
        workExperienceStackMapper.insert(stack);
        return toStackDTO(stack);
    }

    @Transactional
    public WorkExperienceStackDTO updateStack(WorkExperienceStack stack) {
        workExperienceStackMapper.updateById(stack);
        return toStackDTO(workExperienceStackMapper.selectById(stack.getId()));
    }

    @Transactional
    public void deleteStack(Long id) {
        workExperienceStackMapper.deleteById(id);
    }

    // ========== 转换方法 ==========

    private WorkExperienceDTO toExpDTO(WorkExperience e) {
        WorkExperienceDTO dto = new WorkExperienceDTO();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setJob(e.getJob());
        dto.setDescription(e.getDescription());
        dto.setSortOrder(e.getSortOrder());
        dto.setIsVisible(e.getIsVisible());
        dto.setCreateTime(e.getCreateTime());
        dto.setUpdateTime(e.getUpdateTime());
        return dto;
    }

    private ProjectDTO toProjDTO(Project p) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(p.getId());
        dto.setExperienceId(p.getExperienceId());
        dto.setName(p.getName());
        dto.setLifecycle(p.getLifecycle());
        dto.setBackground(p.getBackground());
        dto.setDuty(p.getDuty());
        dto.setSortOrder(p.getSortOrder());
        dto.setCreateTime(p.getCreateTime());
        dto.setUpdateTime(p.getUpdateTime());
        return dto;
    }

    private ProjectAchievementDTO toAchDTO(ProjectAchievement a) {
        ProjectAchievementDTO dto = new ProjectAchievementDTO();
        dto.setId(a.getId());
        dto.setProjectId(a.getProjectId());
        dto.setContent(a.getContent());
        dto.setSortOrder(a.getSortOrder());
        dto.setCreateTime(a.getCreateTime());
        dto.setUpdateTime(a.getUpdateTime());
        return dto;
    }

    private WorkExperienceStackDTO toStackDTO(WorkExperienceStack s) {
        WorkExperienceStackDTO dto = new WorkExperienceStackDTO();
        dto.setId(s.getId());
        dto.setExperienceId(s.getExperienceId());
        dto.setTechName(s.getTechName());
        dto.setSortOrder(s.getSortOrder());
        dto.setCreateTime(s.getCreateTime());
        dto.setUpdateTime(s.getUpdateTime());
        return dto;
    }
}
