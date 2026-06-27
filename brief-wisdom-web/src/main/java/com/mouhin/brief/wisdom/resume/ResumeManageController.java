package com.mouhin.brief.wisdom.resume;

import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.resume.*;
import com.mouhin.brief.wisdom.persistence.model.*;
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
public class ResumeManageController {

    private final ResumeManageService resumeManageService;

    // ========== 工作经历 ==========

    @GetMapping("/experiences")
    public ApiResponse<List<WorkExperienceDTO>> listExperiences() {
        try {
            return ApiResponse.success(resumeManageService.listExperiences());
        } catch (Exception e) {
            log.error("获取工作经历列表失败: ", e);
            return ApiResponse.fail("获取工作经历列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/experiences/{id}")
    public ApiResponse<WorkExperienceDTO> getExperience(@PathVariable Long id) {
        try {
            WorkExperienceDTO data = resumeManageService.getExperience(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.fail("工作经历不存在");
        } catch (Exception e) {
            log.error("获取工作经历失败: ", e);
            return ApiResponse.fail("获取工作经历失败: " + e.getMessage());
        }
    }

    @PostMapping("/experiences")
    public ApiResponse<WorkExperienceDTO> createExperience(@RequestBody WorkExperience experience) {
        try {
            return ApiResponse.success(resumeManageService.createExperience(experience));
        } catch (Exception e) {
            log.error("创建工作经历失败: ", e);
            return ApiResponse.fail("创建工作经历失败: " + e.getMessage());
        }
    }

    @PutMapping("/experiences/{id}")
    public ApiResponse<WorkExperienceDTO> updateExperience(@PathVariable Long id, @RequestBody WorkExperience experience) {
        try {
            experience.setId(id);
            return ApiResponse.success(resumeManageService.updateExperience(experience));
        } catch (Exception e) {
            log.error("更新工作经历失败: ", e);
            return ApiResponse.fail("更新工作经历失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/experiences/{id}")
    public ApiResponse<Void> deleteExperience(@PathVariable Long id) {
        try {
            resumeManageService.deleteExperience(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除工作经历失败: ", e);
            return ApiResponse.fail("删除工作经历失败: " + e.getMessage());
        }
    }

    // ========== 项目 ==========

    @GetMapping("/projects")
    public ApiResponse<List<ProjectDTO>> listProjects(@RequestParam(required = false) Long experienceId) {
        try {
            List<ProjectDTO> data = experienceId != null
                    ? resumeManageService.listProjectsByExperienceId(experienceId)
                    : resumeManageService.listProjects();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取项目列表失败: ", e);
            return ApiResponse.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<ProjectDTO> getProject(@PathVariable Long id) {
        try {
            ProjectDTO data = resumeManageService.getProject(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.fail("项目不存在");
        } catch (Exception e) {
            log.error("获取项目失败: ", e);
            return ApiResponse.fail("获取项目失败: " + e.getMessage());
        }
    }

    @PostMapping("/projects")
    public ApiResponse<ProjectDTO> createProject(@RequestBody Project project) {
        try {
            return ApiResponse.success(resumeManageService.createProject(project));
        } catch (Exception e) {
            log.error("创建项目失败: ", e);
            return ApiResponse.fail("创建项目失败: " + e.getMessage());
        }
    }

    @PutMapping("/projects/{id}")
    public ApiResponse<ProjectDTO> updateProject(@PathVariable Long id, @RequestBody Project project) {
        try {
            project.setId(id);
            return ApiResponse.success(resumeManageService.updateProject(project));
        } catch (Exception e) {
            log.error("更新项目失败: ", e);
            return ApiResponse.fail("更新项目失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        try {
            resumeManageService.deleteProject(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除项目失败: ", e);
            return ApiResponse.fail("删除项目失败: " + e.getMessage());
        }
    }

    // ========== 项目成果 ==========

    @GetMapping("/achievements")
    public ApiResponse<List<ProjectAchievementDTO>> listAchievements(@RequestParam(required = false) Long projectId) {
        try {
            List<ProjectAchievementDTO> data = projectId != null
                    ? resumeManageService.listAchievementsByProjectId(projectId)
                    : resumeManageService.listAchievements();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取项目成果列表失败: ", e);
            return ApiResponse.fail("获取项目成果列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/{id}")
    public ApiResponse<ProjectAchievementDTO> getAchievement(@PathVariable Long id) {
        try {
            ProjectAchievementDTO data = resumeManageService.getAchievement(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.fail("项目成果不存在");
        } catch (Exception e) {
            log.error("获取项目成果失败: ", e);
            return ApiResponse.fail("获取项目成果失败: " + e.getMessage());
        }
    }

    @PostMapping("/achievements")
    public ApiResponse<ProjectAchievementDTO> createAchievement(@RequestBody ProjectAchievement achievement) {
        try {
            return ApiResponse.success(resumeManageService.createAchievement(achievement));
        } catch (Exception e) {
            log.error("创建项目成果失败: ", e);
            return ApiResponse.fail("创建项目成果失败: " + e.getMessage());
        }
    }

    @PutMapping("/achievements/{id}")
    public ApiResponse<ProjectAchievementDTO> updateAchievement(@PathVariable Long id, @RequestBody ProjectAchievement achievement) {
        try {
            achievement.setId(id);
            return ApiResponse.success(resumeManageService.updateAchievement(achievement));
        } catch (Exception e) {
            log.error("更新项目成果失败: ", e);
            return ApiResponse.fail("更新项目成果失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/achievements/{id}")
    public ApiResponse<Void> deleteAchievement(@PathVariable Long id) {
        try {
            resumeManageService.deleteAchievement(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除项目成果失败: ", e);
            return ApiResponse.fail("删除项目成果失败: " + e.getMessage());
        }
    }

    // ========== 技术栈 ==========

    @GetMapping("/stacks")
    public ApiResponse<List<WorkExperienceStackDTO>> listStacks(@RequestParam(required = false) Long experienceId) {
        try {
            List<WorkExperienceStackDTO> data = experienceId != null
                    ? resumeManageService.listStacksByExperienceId(experienceId)
                    : resumeManageService.listStacks();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取技术栈列表失败: ", e);
            return ApiResponse.fail("获取技术栈列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/stacks/{id}")
    public ApiResponse<WorkExperienceStackDTO> getStack(@PathVariable Long id) {
        try {
            WorkExperienceStackDTO data = resumeManageService.getStack(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.fail("技术栈不存在");
        } catch (Exception e) {
            log.error("获取技术栈失败: ", e);
            return ApiResponse.fail("获取技术栈失败: " + e.getMessage());
        }
    }

    @PostMapping("/stacks")
    public ApiResponse<WorkExperienceStackDTO> createStack(@RequestBody WorkExperienceStack stack) {
        try {
            return ApiResponse.success(resumeManageService.createStack(stack));
        } catch (Exception e) {
            log.error("创建技术栈失败: ", e);
            return ApiResponse.fail("创建技术栈失败: " + e.getMessage());
        }
    }

    @PutMapping("/stacks/{id}")
    public ApiResponse<WorkExperienceStackDTO> updateStack(@PathVariable Long id, @RequestBody WorkExperienceStack stack) {
        try {
            stack.setId(id);
            return ApiResponse.success(resumeManageService.updateStack(stack));
        } catch (Exception e) {
            log.error("更新技术栈失败: ", e);
            return ApiResponse.fail("更新技术栈失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/stacks/{id}")
    public ApiResponse<Void> deleteStack(@PathVariable Long id) {
        try {
            resumeManageService.deleteStack(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除技术栈失败: ", e);
            return ApiResponse.fail("删除技术栈失败: " + e.getMessage());
        }
    }
}
