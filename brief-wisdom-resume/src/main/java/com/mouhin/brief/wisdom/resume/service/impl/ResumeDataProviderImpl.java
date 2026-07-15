package com.mouhin.brief.wisdom.resume.service.impl;

import com.mouhin.brief.wisdom.common.tool.ResumeDataProvider;
import com.mouhin.brief.wisdom.resume.dto.ProjectVO;
import com.mouhin.brief.wisdom.resume.dto.WorkExperienceVO;
import com.mouhin.brief.wisdom.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 简历数据提供者实现
 * <p>
 * 基于 ResumeService 实现 ResumeDataProvider 接口，
 * 为 AI 工具模块提供简历数据访问能力。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Component
@RequiredArgsConstructor
public class ResumeDataProviderImpl implements ResumeDataProvider {

    private final ResumeService resumeService;

    @Override
    public List<Map<String, Object>> listAllExperiences() {
        List<WorkExperienceVO> experiences = resumeService.listAllExperiences();
        List<Map<String, Object>> result = new ArrayList<>(experiences.size());

        for (WorkExperienceVO exp : experiences) {
            Map<String, Object> map = new HashMap<>(8);
            map.put("title", exp.getTitle());
            map.put("job", exp.getJob());
            map.put("description", exp.getDescription());
            map.put("stacks", exp.getStacks() != null ? exp.getStacks() : List.of());

            // 转换项目列表
            if (exp.getProjects() != null) {
                List<Map<String, Object>> projects = new ArrayList<>(exp.getProjects().size());
                for (ProjectVO project : exp.getProjects()) {
                    Map<String, Object> projectMap = new HashMap<>(6);
                    projectMap.put("name", project.getName());
                    projectMap.put("lifecycle", project.getLifecycle());
                    projectMap.put("background", project.getBackground());
                    projectMap.put("duty", project.getDuty());
                    projectMap.put("achievements", project.getAchievements() != null ? project.getAchievements() : List.of());
                    projects.add(projectMap);
                }
                map.put("projects", projects);
            } else {
                map.put("projects", List.of());
            }

            result.add(map);
        }

        return result;
    }
}
