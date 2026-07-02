package com.mouhin.brief.wisdom.resume.service;

import com.mouhin.brief.wisdom.common.resume.ProjectAchievementDTO;
import com.mouhin.brief.wisdom.common.resume.ProjectDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceStackDTO;
import com.mouhin.brief.wisdom.persistence.model.Project;
import com.mouhin.brief.wisdom.persistence.model.ProjectAchievement;
import com.mouhin.brief.wisdom.persistence.model.WorkExperience;
import com.mouhin.brief.wisdom.persistence.model.WorkExperienceStack;

import java.util.List;

/**
 * 简历数据管理服务接口（CRUD）
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface ResumeManageService {

    // ========== 工作经历 ==========

    List<WorkExperienceDTO> listExperiences();

    WorkExperienceDTO getExperience(Long id);

    WorkExperienceDTO createExperience(WorkExperience experience);

    WorkExperienceDTO updateExperience(WorkExperience experience);

    void deleteExperience(Long id);

    // ========== 项目 ==========

    List<ProjectDTO> listProjects();

    List<ProjectDTO> listProjectsByExperienceId(Long experienceId);

    ProjectDTO getProject(Long id);

    ProjectDTO createProject(Project project);

    ProjectDTO updateProject(Project project);

    void deleteProject(Long id);

    // ========== 项目成果 ==========

    List<ProjectAchievementDTO> listAchievements();

    List<ProjectAchievementDTO> listAchievementsByProjectId(Long projectId);

    ProjectAchievementDTO getAchievement(Long id);

    ProjectAchievementDTO createAchievement(ProjectAchievement achievement);

    ProjectAchievementDTO updateAchievement(ProjectAchievement achievement);

    void deleteAchievement(Long id);

    // ========== 技术栈 ==========

    List<WorkExperienceStackDTO> listStacks();

    List<WorkExperienceStackDTO> listStacksByExperienceId(Long experienceId);

    WorkExperienceStackDTO getStack(Long id);

    WorkExperienceStackDTO createStack(WorkExperienceStack stack);

    WorkExperienceStackDTO updateStack(WorkExperienceStack stack);

    void deleteStack(Long id);
}
