package com.mouhin.brief.wisdom.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.*;
import com.mouhin.brief.wisdom.persistence.model.*;
import com.mouhin.brief.wisdom.resume.dto.ProjectVO;
import com.mouhin.brief.wisdom.resume.dto.WorkExperienceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简历数据服务
 * <p>
 * 从数据库查询工作经历、项目、成果、技术栈，组装为前端所需的 VO 结构。
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final WorkExperienceMapper workExperienceMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAchievementMapper projectAchievementMapper;
    private final WorkExperienceStackMapper workExperienceStackMapper;

    /**
     * 获取所有工作经历（含项目、成果、技术栈）
     *
     * @return 工作经历列表，按 sort_order 排序
     */
    public List<WorkExperienceVO> listAllExperiences() {
        // 1. 查询所有可见的工作经历
        List<WorkExperience> experiences = workExperienceMapper.selectList(
                new LambdaQueryWrapper<WorkExperience>()
                        .eq(WorkExperience::getIsVisible, 1)
                        .orderByAsc(WorkExperience::getSortOrder)
        );

        if (experiences.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> expIds = experiences.stream()
                .map(WorkExperience::getId)
                .collect(Collectors.toList());

        // 2. 批量查询所有关联的项目
        List<Project> allProjects = projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getExperienceId, expIds)
                        .orderByAsc(Project::getSortOrder)
        );

        // 3. 批量查询所有技术栈
        List<WorkExperienceStack> allStacks = workExperienceStackMapper.selectList(
                new LambdaQueryWrapper<WorkExperienceStack>()
                        .in(WorkExperienceStack::getExperienceId, expIds)
                        .orderByAsc(WorkExperienceStack::getSortOrder)
        );

        // 4. 批量查询所有项目成果
        List<Long> projectIds = allProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        List<ProjectAchievement> allAchievements = projectIds.isEmpty()
                ? Collections.emptyList()
                : projectAchievementMapper.selectList(
                        new LambdaQueryWrapper<ProjectAchievement>()
                                .in(ProjectAchievement::getProjectId, projectIds)
                                .orderByAsc(ProjectAchievement::getSortOrder)
                );

        // 5. 按 ID 分组
        Map<Long, List<Project>> projectsByExpId = allProjects.stream()
                .collect(Collectors.groupingBy(Project::getExperienceId));

        Map<Long, List<WorkExperienceStack>> stacksByExpId = allStacks.stream()
                .collect(Collectors.groupingBy(WorkExperienceStack::getExperienceId));

        Map<Long, List<ProjectAchievement>> achievementsByProjectId = allAchievements.stream()
                .collect(Collectors.groupingBy(ProjectAchievement::getProjectId));

        // 6. 组装 VO
        return experiences.stream().map(exp -> {
            WorkExperienceVO vo = new WorkExperienceVO();
            vo.setTitle(exp.getTitle());
            vo.setJob(exp.getJob());
            vo.setDescription(exp.getDescription());

            // 项目列表
            List<Project> projects = projectsByExpId.getOrDefault(exp.getId(), Collections.emptyList());
            vo.setProjects(projects.stream().map(p -> {
                ProjectVO pvo = new ProjectVO();
                pvo.setName(p.getName());
                pvo.setLifecycle(p.getLifecycle());
                pvo.setBackground(p.getBackground());
                pvo.setDuty(p.getDuty());

                List<ProjectAchievement> achs = achievementsByProjectId.getOrDefault(p.getId(), Collections.emptyList());
                pvo.setAchievements(achs.stream()
                        .map(ProjectAchievement::getContent)
                        .collect(Collectors.toList()));

                return pvo;
            }).collect(Collectors.toList()));

            // 技术栈
            List<WorkExperienceStack> stacks = stacksByExpId.getOrDefault(exp.getId(), Collections.emptyList());
            vo.setStacks(stacks.stream()
                    .map(WorkExperienceStack::getTechName)
                    .collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }
}
