package com.mouhin.brief.wisdom.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public List<WorkExperience> listExperiences() {
        return workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .orderByAsc(WorkExperience::getSortOrder)
        );
    }

    public WorkExperience getExperience(Long id) {
        return workExperienceMapper.selectById(id);
    }

    @Transactional
    public WorkExperience createExperience(WorkExperience experience) {
        workExperienceMapper.insert(experience);
        return experience;
    }

    @Transactional
    public WorkExperience updateExperience(WorkExperience experience) {
        workExperienceMapper.updateById(experience);
        return experience;
    }

    @Transactional
    public void deleteExperience(Long id) {
        workExperienceMapper.deleteById(id);
    }

    // ========== 项目 ==========

    public List<Project> listProjects() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .orderByAsc(Project::getSortOrder)
        );
    }

    public List<Project> listProjectsByExperienceId(Long experienceId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getExperienceId, experienceId)
                        .orderByAsc(Project::getSortOrder)
        );
    }

    public Project getProject(Long id) {
        return projectMapper.selectById(id);
    }

    @Transactional
    public Project createProject(Project project) {
        projectMapper.insert(project);
        return project;
    }

    @Transactional
    public Project updateProject(Project project) {
        projectMapper.updateById(project);
        return project;
    }

    @Transactional
    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
    }

    // ========== 项目成果 ==========

    public List<ProjectAchievement> listAchievements() {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    public List<ProjectAchievement> listAchievementsByProjectId(Long projectId) {
        return projectAchievementMapper.selectList(
                new LambdaQueryWrapper<ProjectAchievement>()
                        .eq(ProjectAchievement::getProjectId, projectId)
                        .orderByAsc(ProjectAchievement::getSortOrder)
        );
    }

    public ProjectAchievement getAchievement(Long id) {
        return projectAchievementMapper.selectById(id);
    }

    @Transactional
    public ProjectAchievement createAchievement(ProjectAchievement achievement) {
        projectAchievementMapper.insert(achievement);
        return achievement;
    }

    @Transactional
    public ProjectAchievement updateAchievement(ProjectAchievement achievement) {
        projectAchievementMapper.updateById(achievement);
        return achievement;
    }

    @Transactional
    public void deleteAchievement(Long id) {
        projectAchievementMapper.deleteById(id);
    }

    // ========== 技术栈 ==========

    public List<WorkExperienceStack> listStacks() {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    public List<WorkExperienceStack> listStacksByExperienceId(Long experienceId) {
        return workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .in(WorkExperienceStack::getExperienceId, experienceId)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );
    }

    public WorkExperienceStack getStack(Long id) {
        return workExperienceStackMapper.selectById(id);
    }

    @Transactional
    public WorkExperienceStack createStack(WorkExperienceStack stack) {
        workExperienceStackMapper.insert(stack);
        return stack;
    }

    @Transactional
    public WorkExperienceStack updateStack(WorkExperienceStack stack) {
        workExperienceStackMapper.updateById(stack);
        return stack;
    }

    @Transactional
    public void deleteStack(Long id) {
        workExperienceStackMapper.deleteById(id);
    }
}
