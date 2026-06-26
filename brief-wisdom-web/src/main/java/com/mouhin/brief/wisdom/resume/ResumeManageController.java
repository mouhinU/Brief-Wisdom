package com.mouhin.brief.wisdom.resume;

import com.mouhin.brief.wisdom.persistence.model.*;
import com.mouhin.brief.wisdom.resume.service.ResumeManageService;
import lombok.Data;
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
    public ApiResponse<List<WorkExperience>> listExperiences() {
        try {
            List<WorkExperience> data = resumeManageService.listExperiences();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取工作经历列表失败: ", e);
            return ApiResponse.error("获取工作经历列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/experiences/{id}")
    public ApiResponse<WorkExperience> getExperience(@PathVariable Long id) {
        try {
            WorkExperience data = resumeManageService.getExperience(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.error("工作经历不存在");
        } catch (Exception e) {
            log.error("获取工作经历失败: ", e);
            return ApiResponse.error("获取工作经历失败: " + e.getMessage());
        }
    }

    @PostMapping("/experiences")
    public ApiResponse<WorkExperience> createExperience(@RequestBody WorkExperience experience) {
        try {
            WorkExperience data = resumeManageService.createExperience(experience);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("创建工作经历失败: ", e);
            return ApiResponse.error("创建工作经历失败: " + e.getMessage());
        }
    }

    @PutMapping("/experiences/{id}")
    public ApiResponse<WorkExperience> updateExperience(@PathVariable Long id, @RequestBody WorkExperience experience) {
        try {
            experience.setId(id);
            WorkExperience data = resumeManageService.updateExperience(experience);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("更新工作经历失败: ", e);
            return ApiResponse.error("更新工作经历失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/experiences/{id}")
    public ApiResponse<Void> deleteExperience(@PathVariable Long id) {
        try {
            resumeManageService.deleteExperience(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除工作经历失败: ", e);
            return ApiResponse.error("删除工作经历失败: " + e.getMessage());
        }
    }

    // ========== 项目 ==========

    @GetMapping("/projects")
    public ApiResponse<List<Project>> listProjects(@RequestParam(required = false) Long experienceId) {
        try {
            List<Project> data = experienceId != null
                    ? resumeManageService.listProjectsByExperienceId(experienceId)
                    : resumeManageService.listProjects();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取项目列表失败: ", e);
            return ApiResponse.error("获取项目列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<Project> getProject(@PathVariable Long id) {
        try {
            Project data = resumeManageService.getProject(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.error("项目不存在");
        } catch (Exception e) {
            log.error("获取项目失败: ", e);
            return ApiResponse.error("获取项目失败: " + e.getMessage());
        }
    }

    @PostMapping("/projects")
    public ApiResponse<Project> createProject(@RequestBody Project project) {
        try {
            Project data = resumeManageService.createProject(project);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("创建项目失败: ", e);
            return ApiResponse.error("创建项目失败: " + e.getMessage());
        }
    }

    @PutMapping("/projects/{id}")
    public ApiResponse<Project> updateProject(@PathVariable Long id, @RequestBody Project project) {
        try {
            project.setId(id);
            Project data = resumeManageService.updateProject(project);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("更新项目失败: ", e);
            return ApiResponse.error("更新项目失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        try {
            resumeManageService.deleteProject(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除项目失败: ", e);
            return ApiResponse.error("删除项目失败: " + e.getMessage());
        }
    }

    // ========== 项目成果 ==========

    @GetMapping("/achievements")
    public ApiResponse<List<ProjectAchievement>> listAchievements(@RequestParam(required = false) Long projectId) {
        try {
            List<ProjectAchievement> data = projectId != null
                    ? resumeManageService.listAchievementsByProjectId(projectId)
                    : resumeManageService.listAchievements();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取项目成果列表失败: ", e);
            return ApiResponse.error("获取项目成果列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/{id}")
    public ApiResponse<ProjectAchievement> getAchievement(@PathVariable Long id) {
        try {
            ProjectAchievement data = resumeManageService.getAchievement(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.error("项目成果不存在");
        } catch (Exception e) {
            log.error("获取项目成果失败: ", e);
            return ApiResponse.error("获取项目成果失败: " + e.getMessage());
        }
    }

    @PostMapping("/achievements")
    public ApiResponse<ProjectAchievement> createAchievement(@RequestBody ProjectAchievement achievement) {
        try {
            ProjectAchievement data = resumeManageService.createAchievement(achievement);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("创建项目成果失败: ", e);
            return ApiResponse.error("创建项目成果失败: " + e.getMessage());
        }
    }

    @PutMapping("/achievements/{id}")
    public ApiResponse<ProjectAchievement> updateAchievement(@PathVariable Long id, @RequestBody ProjectAchievement achievement) {
        try {
            achievement.setId(id);
            ProjectAchievement data = resumeManageService.updateAchievement(achievement);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("更新项目成果失败: ", e);
            return ApiResponse.error("更新项目成果失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/achievements/{id}")
    public ApiResponse<Void> deleteAchievement(@PathVariable Long id) {
        try {
            resumeManageService.deleteAchievement(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除项目成果失败: ", e);
            return ApiResponse.error("删除项目成果失败: " + e.getMessage());
        }
    }

    // ========== 技术栈 ==========

    @GetMapping("/stacks")
    public ApiResponse<List<WorkExperienceStack>> listStacks(@RequestParam(required = false) Long experienceId) {
        try {
            List<WorkExperienceStack> data = experienceId != null
                    ? resumeManageService.listStacksByExperienceId(experienceId)
                    : resumeManageService.listStacks();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取技术栈列表失败: ", e);
            return ApiResponse.error("获取技术栈列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/stacks/{id}")
    public ApiResponse<WorkExperienceStack> getStack(@PathVariable Long id) {
        try {
            WorkExperienceStack data = resumeManageService.getStack(id);
            return data != null ? ApiResponse.success(data) : ApiResponse.error("技术栈不存在");
        } catch (Exception e) {
            log.error("获取技术栈失败: ", e);
            return ApiResponse.error("获取技术栈失败: " + e.getMessage());
        }
    }

    @PostMapping("/stacks")
    public ApiResponse<WorkExperienceStack> createStack(@RequestBody WorkExperienceStack stack) {
        try {
            WorkExperienceStack data = resumeManageService.createStack(stack);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("创建技术栈失败: ", e);
            return ApiResponse.error("创建技术栈失败: " + e.getMessage());
        }
    }

    @PutMapping("/stacks/{id}")
    public ApiResponse<WorkExperienceStack> updateStack(@PathVariable Long id, @RequestBody WorkExperienceStack stack) {
        try {
            stack.setId(id);
            WorkExperienceStack data = resumeManageService.updateStack(stack);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("更新技术栈失败: ", e);
            return ApiResponse.error("更新技术栈失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/stacks/{id}")
    public ApiResponse<Void> deleteStack(@PathVariable Long id) {
        try {
            resumeManageService.deleteStack(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除技术栈失败: ", e);
            return ApiResponse.error("删除技术栈失败: " + e.getMessage());
        }
    }

    // ========== 通用响应 ==========

    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String error;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setData(data);
            return response;
        }

        public static <T> ApiResponse<T> error(String error) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setError(error);
            return response;
        }
    }
}
