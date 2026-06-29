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
import com.mouhin.brief.wisdom.resume.service.ResumeManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历数据管理 REST 接口
 */
@RestController
@RequestMapping("/api/resume/manage")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@RequiresPermission("resume:manage")
public class ResumeManageController {

    private final ResumeManageService resumeManageService;

    // ========== 工作经历 ==========

    @GetMapping("/experiences")
    public List<WorkExperienceDTO> listExperiences() {
        return resumeManageService.listExperiences();
    }

    @GetMapping("/experiences/{id}")
    public WorkExperienceDTO getExperience(@PathVariable Long id) {
        return resumeManageService.getExperience(id);
    }

    @PostMapping("/experiences")
    public WorkExperienceDTO createExperience(@RequestBody WorkExperience experience) {
        return resumeManageService.createExperience(experience);
    }

    @PutMapping("/experiences/{id}")
    public WorkExperienceDTO updateExperience(@PathVariable Long id, @RequestBody WorkExperience experience) {
        experience.setId(id);
        return resumeManageService.updateExperience(experience);
    }

    @DeleteMapping("/experiences/{id}")
    public Boolean deleteExperience(@PathVariable Long id) {
        resumeManageService.deleteExperience(id);
        return true;
    }

    // ========== 项目 ==========

    @GetMapping("/projects")
    public List<ProjectDTO> listProjects(@RequestParam(required = false) Long experienceId) {
        return experienceId != null
                ? resumeManageService.listProjectsByExperienceId(experienceId)
                : resumeManageService.listProjects();
    }

    @GetMapping("/projects/{id}")
    public ProjectDTO getProject(@PathVariable Long id) {
        return resumeManageService.getProject(id);
    }

    @PostMapping("/projects")
    public ProjectDTO createProject(@RequestBody Project project) {
        return resumeManageService.createProject(project);
    }

    @PutMapping("/projects/{id}")
    public ProjectDTO updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        return resumeManageService.updateProject(project);
    }

    @DeleteMapping("/projects/{id}")
    public Boolean deleteProject(@PathVariable Long id) {
        resumeManageService.deleteProject(id);
        return true;
    }

    // ========== 项目成果 ==========

    @GetMapping("/achievements")
    public List<ProjectAchievementDTO> listAchievements(@RequestParam(required = false) Long projectId) {
        return projectId != null
                ? resumeManageService.listAchievementsByProjectId(projectId)
                : resumeManageService.listAchievements();
    }

    @GetMapping("/achievements/{id}")
    public ProjectAchievementDTO getAchievement(@PathVariable Long id) {
        return resumeManageService.getAchievement(id);
    }

    @PostMapping("/achievements")
    public ProjectAchievementDTO createAchievement(@RequestBody ProjectAchievement achievement) {
        return resumeManageService.createAchievement(achievement);
    }

    @PutMapping("/achievements/{id}")
    public ProjectAchievementDTO updateAchievement(@PathVariable Long id, @RequestBody ProjectAchievement achievement) {
        achievement.setId(id);
        return resumeManageService.updateAchievement(achievement);
    }

    @DeleteMapping("/achievements/{id}")
    public Boolean deleteAchievement(@PathVariable Long id) {
        resumeManageService.deleteAchievement(id);
        return true;
    }

    // ========== 技术栈 ==========

    @GetMapping("/stacks")
    public List<WorkExperienceStackDTO> listStacks(@RequestParam(required = false) Long experienceId) {
        return experienceId != null
                ? resumeManageService.listStacksByExperienceId(experienceId)
                : resumeManageService.listStacks();
    }

    @GetMapping("/stacks/{id}")
    public WorkExperienceStackDTO getStack(@PathVariable Long id) {
        return resumeManageService.getStack(id);
    }

    @PostMapping("/stacks")
    public WorkExperienceStackDTO createStack(@RequestBody WorkExperienceStack stack) {
        return resumeManageService.createStack(stack);
    }

    @PutMapping("/stacks/{id}")
    public WorkExperienceStackDTO updateStack(@PathVariable Long id, @RequestBody WorkExperienceStack stack) {
        stack.setId(id);
        return resumeManageService.updateStack(stack);
    }

    @DeleteMapping("/stacks/{id}")
    public Boolean deleteStack(@PathVariable Long id) {
        resumeManageService.deleteStack(id);
        return true;
    }
}
