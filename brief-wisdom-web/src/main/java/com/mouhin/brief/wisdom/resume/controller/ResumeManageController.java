package com.mouhin.brief.wisdom.resume.controller;

import com.mouhin.brief.wisdom.common.resume.ProjectAchievementDTO;
import com.mouhin.brief.wisdom.common.resume.ProjectDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceStackDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.Project;
import com.mouhin.brief.wisdom.persistence.model.ProjectAchievement;
import com.mouhin.brief.wisdom.persistence.model.WorkExperience;
import com.mouhin.brief.wisdom.persistence.model.WorkExperienceStack;
import com.mouhin.brief.wisdom.resume.req.ProjectAchievementSaveRequest;
import com.mouhin.brief.wisdom.resume.req.ProjectSaveRequest;
import com.mouhin.brief.wisdom.resume.req.WorkExperienceSaveRequest;
import com.mouhin.brief.wisdom.resume.req.WorkExperienceStackSaveRequest;
import com.mouhin.brief.wisdom.resume.service.ResumeManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历数据管理 REST 接口
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/resume/manage")
@RequiredArgsConstructor
@Slf4j
@RequiresPermission("resume:manage")
@Tag(name = "简历管理", description = "简历数据 CRUD 管理接口")
public class ResumeManageController {

    private final ResumeManageService resumeManageService;

    // ========== 工作经历 ==========

    @Operation(summary = "获取工作经历列表")
    @GetMapping("/experiences")
    public List<WorkExperienceDTO> listExperiences() {
        return resumeManageService.listExperiences();
    }

    @Operation(summary = "获取工作经历详情")
    @GetMapping("/experiences/{id}")
    public WorkExperienceDTO getExperience(@PathVariable Long id) {
        return resumeManageService.getExperience(id);
    }

    @Operation(summary = "新增工作经历")
    @PostMapping("/experiences")
    public WorkExperienceDTO createExperience(@RequestBody WorkExperienceSaveRequest request) {
        return resumeManageService.createExperience(toWorkExperience(null, request));
    }

    @Operation(summary = "更新工作经历")
    @PutMapping("/experiences/{id}")
    public WorkExperienceDTO updateExperience(@PathVariable Long id,
                                              @RequestBody WorkExperienceSaveRequest request) {
        return resumeManageService.updateExperience(toWorkExperience(id, request));
    }

    @Operation(summary = "删除工作经历")
    @DeleteMapping("/experiences/{id}")
    public Boolean deleteExperience(@PathVariable Long id) {
        resumeManageService.deleteExperience(id);
        return true;
    }

    // ========== 项目 ==========

    @Operation(summary = "获取项目列表")
    @GetMapping("/projects")
    public List<ProjectDTO> listProjects(@RequestParam(required = false) Long experienceId) {
        return experienceId != null
                ? resumeManageService.listProjectsByExperienceId(experienceId)
                : resumeManageService.listProjects();
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/projects/{id}")
    public ProjectDTO getProject(@PathVariable Long id) {
        return resumeManageService.getProject(id);
    }

    @Operation(summary = "新增项目")
    @PostMapping("/projects")
    public ProjectDTO createProject(@RequestBody ProjectSaveRequest request) {
        return resumeManageService.createProject(toProject(null, request));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/projects/{id}")
    public ProjectDTO updateProject(@PathVariable Long id, @RequestBody ProjectSaveRequest request) {
        return resumeManageService.updateProject(toProject(id, request));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/projects/{id}")
    public Boolean deleteProject(@PathVariable Long id) {
        resumeManageService.deleteProject(id);
        return true;
    }

    // ========== 项目成果 ==========

    @Operation(summary = "获取项目成果列表")
    @GetMapping("/achievements")
    public List<ProjectAchievementDTO> listAchievements(@RequestParam(required = false) Long projectId) {
        return projectId != null
                ? resumeManageService.listAchievementsByProjectId(projectId)
                : resumeManageService.listAchievements();
    }

    @Operation(summary = "获取项目成果详情")
    @GetMapping("/achievements/{id}")
    public ProjectAchievementDTO getAchievement(@PathVariable Long id) {
        return resumeManageService.getAchievement(id);
    }

    @Operation(summary = "新增项目成果")
    @PostMapping("/achievements")
    public ProjectAchievementDTO createAchievement(@RequestBody ProjectAchievementSaveRequest request) {
        return resumeManageService.createAchievement(toAchievement(null, request));
    }

    @Operation(summary = "更新项目成果")
    @PutMapping("/achievements/{id}")
    public ProjectAchievementDTO updateAchievement(@PathVariable Long id,
                                                   @RequestBody ProjectAchievementSaveRequest request) {
        return resumeManageService.updateAchievement(toAchievement(id, request));
    }

    @Operation(summary = "删除项目成果")
    @DeleteMapping("/achievements/{id}")
    public Boolean deleteAchievement(@PathVariable Long id) {
        resumeManageService.deleteAchievement(id);
        return true;
    }

    // ========== 技术栈 ==========

    @Operation(summary = "获取技术栈列表")
    @GetMapping("/stacks")
    public List<WorkExperienceStackDTO> listStacks(@RequestParam(required = false) Long experienceId) {
        return experienceId != null
                ? resumeManageService.listStacksByExperienceId(experienceId)
                : resumeManageService.listStacks();
    }

    @Operation(summary = "获取技术栈详情")
    @GetMapping("/stacks/{id}")
    public WorkExperienceStackDTO getStack(@PathVariable Long id) {
        return resumeManageService.getStack(id);
    }

    @Operation(summary = "新增技术栈")
    @PostMapping("/stacks")
    public WorkExperienceStackDTO createStack(@RequestBody WorkExperienceStackSaveRequest request) {
        return resumeManageService.createStack(toStack(null, request));
    }

    @Operation(summary = "更新技术栈")
    @PutMapping("/stacks/{id}")
    public WorkExperienceStackDTO updateStack(@PathVariable Long id,
                                              @RequestBody WorkExperienceStackSaveRequest request) {
        return resumeManageService.updateStack(toStack(id, request));
    }

    @Operation(summary = "删除技术栈")
    @DeleteMapping("/stacks/{id}")
    public Boolean deleteStack(@PathVariable Long id) {
        resumeManageService.deleteStack(id);
        return true;
    }

    private WorkExperience toWorkExperience(Long id, WorkExperienceSaveRequest request) {
        WorkExperience experience = new WorkExperience();
        experience.setId(id);
        experience.setTitle(request.getTitle());
        experience.setJob(request.getJob());
        experience.setDescription(request.getDescription());
        experience.setSortOrder(request.getSortOrder());
        experience.setIsVisible(request.getIsVisible());
        return experience;
    }

    private Project toProject(Long id, ProjectSaveRequest request) {
        Project project = new Project();
        project.setId(id);
        project.setExperienceId(request.getExperienceId());
        project.setName(request.getName());
        project.setLifecycle(request.getLifecycle());
        project.setBackground(request.getBackground());
        project.setDuty(request.getDuty());
        project.setSortOrder(request.getSortOrder());
        return project;
    }

    private ProjectAchievement toAchievement(Long id, ProjectAchievementSaveRequest request) {
        ProjectAchievement achievement = new ProjectAchievement();
        achievement.setId(id);
        achievement.setProjectId(request.getProjectId());
        achievement.setContent(request.getContent());
        achievement.setSortOrder(request.getSortOrder());
        return achievement;
    }

    private WorkExperienceStack toStack(Long id, WorkExperienceStackSaveRequest request) {
        WorkExperienceStack stack = new WorkExperienceStack();
        stack.setId(id);
        stack.setExperienceId(request.getExperienceId());
        stack.setTechName(request.getTechName());
        stack.setSortOrder(request.getSortOrder());
        return stack;
    }
}
