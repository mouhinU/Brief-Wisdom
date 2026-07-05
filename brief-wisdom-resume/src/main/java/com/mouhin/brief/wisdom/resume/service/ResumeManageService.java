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

    /**
     * 获取所有工作经历（含项目、成果、技术栈）
     *
     * @return 工作经历 DTO 列表
     */
    List<WorkExperienceDTO> listExperiences();

    /**
     * 根据 ID 获取工作经历详情
     *
     * @param id 工作经历 ID
     * @return 工作经历 DTO
     */
    WorkExperienceDTO getExperience(Long id);

    /**
     * 创建工作经历
     *
     * @param experience 工作经历实体
     * @return 创建后的工作经历 DTO
     */
    WorkExperienceDTO createExperience(WorkExperience experience);

    /**
     * 更新工作经历
     *
     * @param experience 工作经历实体
     * @return 更新后的工作经历 DTO
     */
    WorkExperienceDTO updateExperience(WorkExperience experience);

    /**
     * 删除工作经历
     *
     * @param id 工作经历 ID
     */
    void deleteExperience(Long id);

    // ========== 项目 ==========

    /**
     * 获取所有项目
     *
     * @return 项目 DTO 列表
     */
    List<ProjectDTO> listProjects();

    /**
     * 获取指定工作经历下的项目列表
     *
     * @param experienceId 工作经历 ID
     * @return 项目 DTO 列表
     */
    List<ProjectDTO> listProjectsByExperienceId(Long experienceId);

    /**
     * 根据 ID 获取项目详情
     *
     * @param id 项目 ID
     * @return 项目 DTO
     */
    ProjectDTO getProject(Long id);

    /**
     * 创建项目
     *
     * @param project 项目实体
     * @return 创建后的项目 DTO
     */
    ProjectDTO createProject(Project project);

    /**
     * 更新项目
     *
     * @param project 项目实体
     * @return 更新后的项目 DTO
     */
    ProjectDTO updateProject(Project project);

    /**
     * 删除项目
     *
     * @param id 项目 ID
     */
    void deleteProject(Long id);

    // ========== 项目成果 ==========

    /**
     * 获取所有项目成果
     *
     * @return 项目成果 DTO 列表
     */
    List<ProjectAchievementDTO> listAchievements();

    /**
     * 获取指定项目下的成果列表
     *
     * @param projectId 项目 ID
     * @return 项目成果 DTO 列表
     */
    List<ProjectAchievementDTO> listAchievementsByProjectId(Long projectId);

    /**
     * 根据 ID 获取项目成果详情
     *
     * @param id 项目成果 ID
     * @return 项目成果 DTO
     */
    ProjectAchievementDTO getAchievement(Long id);

    /**
     * 创建项目成果
     *
     * @param achievement 项目成果实体
     * @return 创建后的项目成果 DTO
     */
    ProjectAchievementDTO createAchievement(ProjectAchievement achievement);

    /**
     * 更新项目成果
     *
     * @param achievement 项目成果实体
     * @return 更新后的项目成果 DTO
     */
    ProjectAchievementDTO updateAchievement(ProjectAchievement achievement);

    /**
     * 删除项目成果
     *
     * @param id 项目成果 ID
     */
    void deleteAchievement(Long id);

    // ========== 技术栈 ==========

    /**
     * 获取所有技术栈
     *
     * @return 技术栈 DTO 列表
     */
    List<WorkExperienceStackDTO> listStacks();

    /**
     * 获取指定工作经历下的技术栈列表
     *
     * @param experienceId 工作经历 ID
     * @return 技术栈 DTO 列表
     */
    List<WorkExperienceStackDTO> listStacksByExperienceId(Long experienceId);

    /**
     * 根据 ID 获取技术栈详情
     *
     * @param id 技术栈 ID
     * @return 技术栈 DTO
     */
    WorkExperienceStackDTO getStack(Long id);

    /**
     * 创建技术栈
     *
     * @param stack 技术栈实体
     * @return 创建后的技术栈 DTO
     */
    WorkExperienceStackDTO createStack(WorkExperienceStack stack);

    /**
     * 更新技术栈
     *
     * @param stack 技术栈实体
     * @return 更新后的技术栈 DTO
     */
    WorkExperienceStackDTO updateStack(WorkExperienceStack stack);

    /**
     * 删除技术栈
     *
     * @param id 技术栈 ID
     */
    void deleteStack(Long id);
}
