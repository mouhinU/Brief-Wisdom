package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.common.tool.ResumeDataProvider;
import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 简历分析工具
 * <p>
 * 分析用户的简历内容，包括工作经历、项目经验、技术栈等，给出优化建议。
 * 通过 ResumeDataProvider 接口获取简历数据，不直接依赖 resume 模块。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisTool {

    private final ResumeDataProvider resumeDataProvider;
    private final ToolContextProvider toolContextProvider;

    /**
     * 分析简历整体内容
     *
     * @param dimension 分析维度
     * @return 分析结果
     */
    @Tool(description = "分析用户简历内容，包括工作经历、项目经验、技术栈等，给出优化建议。当用户请求简历诊断、简历优化、简历建议时调用。")
    public String analyzeResume(
            @ToolParam(description = "分析维度: overall(整体分析)/work(工作经历)/project(项目经验)/skills(技术栈)") String dimension) {

        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] analyzeResume 被调用: userId={}, dimension={}", userId, dimension);

        try {
            List<Map<String, Object>> experiences = resumeDataProvider.listAllExperiences();
            if (experiences.isEmpty()) {
                return "当前简历中没有工作经历数据。请先在简历管理页面添加工作经历。";
            }

            String effectiveDimension = (dimension != null) ? dimension.toLowerCase() : "overall";

            return switch (effectiveDimension) {
                case "work" -> analyzeWorkExperiences(experiences);
                case "project" -> analyzeProjects(experiences);
                case "skills" -> analyzeSkills(experiences);
                default -> analyzeOverall(experiences);
            };
        } catch (Exception e) {
            log.error("[Tool] analyzeResume 执行失败: {}", e.getMessage(), e);
            return "简历分析失败: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String analyzeOverall(List<Map<String, Object>> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 简历整体分析\n\n");

        sb.append("### 基本统计\n");
        sb.append("- 工作经历数量: ").append(experiences.size()).append(" 段\n");

        int totalProjects = experiences.stream()
                .mapToInt(e -> ((List<?>) e.getOrDefault("projects", List.of())).size())
                .sum();
        sb.append("- 项目经验数量: ").append(totalProjects).append(" 个\n");

        int totalSkills = experiences.stream()
                .mapToInt(e -> ((List<?>) e.getOrDefault("stacks", List.of())).size())
                .sum();
        sb.append("- 技术栈条目: ").append(totalSkills).append(" 项\n");

        sb.append("\n### 内容完整度检查\n");
        for (Map<String, Object> exp : experiences) {
            String title = (String) exp.getOrDefault("title", "未命名");
            String description = (String) exp.get("description");
            List<?> projects = (List<?>) exp.getOrDefault("projects", List.of());
            List<?> stacks = (List<?>) exp.getOrDefault("stacks", List.of());

            sb.append("- **").append(title).append("**\n");
            if (description == null || description.isBlank()) {
                sb.append("  - ⚠️ 工作描述为空，建议补充工作职责和成就\n");
            } else if (description.length() < 50) {
                sb.append("  - ⚠️ 工作描述过短（").append(description.length())
                        .append("字），建议扩充到 100 字以上\n");
            }
            if (projects.isEmpty()) {
                sb.append("  - ⚠️ 没有关联项目经验，建议补充代表性项目\n");
            }
            if (stacks.isEmpty()) {
                sb.append("  - ⚠️ 没有列出技术栈，建议补充使用的技术\n");
            }
        }

        return sb.toString();
    }

    private String analyzeWorkExperiences(List<Map<String, Object>> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 工作经历分析\n\n");

        for (Map<String, Object> exp : experiences) {
            String title = (String) exp.getOrDefault("title", "未命名");
            String job = (String) exp.get("job");
            String description = (String) exp.get("description");

            sb.append("### ").append(title).append("\n");
            if (job != null) {
                sb.append("职位: ").append(job).append("\n");
            }
            if (description != null) {
                sb.append("描述长度: ").append(description.length()).append(" 字\n");
                if (description.length() < 100) {
                    sb.append("建议: 工作描述偏短，建议使用 STAR 法则（情境-任务-行动-结果）扩充描述\n");
                }
            } else {
                sb.append("建议: 工作描述为空，请补充工作职责和成就\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String analyzeProjects(List<Map<String, Object>> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 项目经验分析\n\n");

        int projectCount = 0;
        for (Map<String, Object> exp : experiences) {
            List<Map<String, Object>> projects =
                    (List<Map<String, Object>>) exp.getOrDefault("projects", List.of());
            for (Map<String, Object> project : projects) {
                projectCount++;
                String name = (String) project.getOrDefault("name", "未命名项目");
                String background = (String) project.get("background");
                String duty = (String) project.get("duty");

                sb.append("### ").append(name).append("\n");
                if (background != null) {
                    sb.append("背景描述长度: ").append(background.length()).append(" 字\n");
                }
                if (duty != null) {
                    sb.append("职责描述长度: ").append(duty.length()).append(" 字\n");
                }
                sb.append("\n");
            }
        }

        if (projectCount == 0) {
            sb.append("暂无项目经验数据。\n");
        } else {
            sb.append("共 ").append(projectCount).append(" 个项目。\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String analyzeSkills(List<Map<String, Object>> experiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 技术栈分析\n\n");
        sb.append("### 已列出的技术栈\n");

        for (Map<String, Object> exp : experiences) {
            String title = (String) exp.getOrDefault("title", "未命名");
            List<String> stacks = (List<String>) exp.getOrDefault("stacks", List.of());
            if (!stacks.isEmpty()) {
                sb.append("- **").append(title).append("**: ");
                sb.append(String.join(", ", stacks)).append("\n");
            }
        }

        return sb.toString();
    }
}
