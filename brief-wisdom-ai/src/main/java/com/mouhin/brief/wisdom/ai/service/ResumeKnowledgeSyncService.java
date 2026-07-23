package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.event.ResumeDataChangedEvent;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import com.mouhin.brief.wisdom.persistence.model.Project;
import com.mouhin.brief.wisdom.persistence.model.ProjectAchievement;
import com.mouhin.brief.wisdom.persistence.model.WorkExperience;
import com.mouhin.brief.wisdom.persistence.model.WorkExperienceStack;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.ProjectAchievementRepository;
import com.mouhin.brief.wisdom.persistence.repository.ProjectRepository;
import com.mouhin.brief.wisdom.persistence.repository.WorkExperienceRepository;
import com.mouhin.brief.wisdom.persistence.repository.WorkExperienceStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简历知识库同步服务
 * <p>
 * 监听简历数据变更事件，将简历内容序列化为结构化 Markdown，
 * 写入「我的简历」知识库并自动向量化，使简历数据参与 RAG 检索。
 *
 * @author Brief-Wisdom
 * @date 2026-07-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeKnowledgeSyncService {

    /**
     * 简历知识库名称
     */
    private static final String RESUME_KB_NAME = "我的简历";

    /**
     * 简历文档去重键（整份简历作为一个文档）
     */
    private static final String RESUME_SOURCE_PATH = "resume://full";

    /**
     * 文档类型：内部文档
     */
    private static final String DOC_TYPE_INTERNAL = "INTERNAL";

    /**
     * 文件类型标识
     */
    private static final String FILE_TYPE_RESUME = "resume";

    /**
     * 文档状态：已发布
     */
    private static final int STATUS_PUBLISHED = 1;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeService knowledgeService;
    private final WorkExperienceRepository workExperienceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAchievementRepository projectAchievementRepository;
    private final WorkExperienceStackRepository workExperienceStackRepository;

    /**
     * 监听简历数据变更事件，异步执行知识库同步
     *
     * @param event 简历数据变更事件
     */
    @Async("briefWisdomExecutor")
    @EventListener
    public void onResumeDataChanged(ResumeDataChangedEvent event) {
        log.info("[简历同步] 收到简历变更事件: {}", event.getChangeDescription());
        try {
            syncResumeToKnowledge();
        } catch (Exception e) {
            log.warn("[简历同步] 同步失败（不影响简历操作）: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行简历数据到知识库的全量同步
     * <p>
     * 读取所有简历数据，序列化为 Markdown，通过 upsert 写入知识库。
     */
    public void syncResumeToKnowledge() {
        // 1. 查找简历知识库
        KnowledgeBase resumeKb = knowledgeBaseRepository.findByName(RESUME_KB_NAME);
        if (resumeKb == null) {
            log.warn("[简历同步] 未找到「{}」知识库，跳过同步", RESUME_KB_NAME);
            return;
        }

        // 2. 读取所有简历数据并序列化为 Markdown
        String markdownContent = buildResumeMarkdown();
        if (markdownContent == null || markdownContent.isBlank()) {
            log.info("[简历同步] 简历数据为空，跳过同步");
            return;
        }

        // 3. 构建文档 BO 并 upsert
        KnowledgeDocumentBO docBO = new KnowledgeDocumentBO();
        docBO.setBaseId(resumeKb.getId());
        docBO.setTitle("个人简历");
        docBO.setDocType(DOC_TYPE_INTERNAL);
        docBO.setContent(markdownContent);
        docBO.setFileType(FILE_TYPE_RESUME);
        docBO.setFileSize((long) markdownContent.getBytes(StandardCharsets.UTF_8).length);
        docBO.setStatus(STATUS_PUBLISHED);
        docBO.setSortOrder(0);
        docBO.setTags("简历,工作经历,项目经验,技术栈");

        boolean created = knowledgeService.upsertImportedMarkdown(docBO, RESUME_SOURCE_PATH);
        log.info("[简历同步] 同步完成: action={}, contentLength={}", created ? "新增" : "更新", markdownContent.length());
    }

    /**
     * 将简历数据序列化为结构化 Markdown 文本
     *
     * @return Markdown 格式的简历内容，无数据时返回 null
     */
    private String buildResumeMarkdown() {
        List<WorkExperience> experiences = workExperienceRepository.findVisibleOrderBySortOrderAsc();
        if (experiences.isEmpty()) {
            return null;
        }

        List<Long> expIds = experiences.stream().map(WorkExperience::getId).collect(Collectors.toList());

        // 批量查询关联数据
        List<Project> allProjects = projectRepository.findByExperienceIdInOrderBySortOrderAsc(expIds);
        List<WorkExperienceStack> allStacks = workExperienceStackRepository.findByExperienceIdInOrderBySortOrderAsc(expIds);

        List<Long> projectIds = allProjects.stream().map(Project::getId).collect(Collectors.toList());
        List<ProjectAchievement> allAchievements = projectIds.isEmpty()
                ? Collections.emptyList()
                : projectAchievementRepository.findByProjectIdInOrderBySortOrderAsc(projectIds);

        // 按 ID 分组
        Map<Long, List<Project>> projectsByExpId = allProjects.stream()
                .collect(Collectors.groupingBy(Project::getExperienceId));
        Map<Long, List<WorkExperienceStack>> stacksByExpId = allStacks.stream()
                .collect(Collectors.groupingBy(WorkExperienceStack::getExperienceId));
        Map<Long, List<ProjectAchievement>> achievementsByProjectId = allAchievements.stream()
                .collect(Collectors.groupingBy(ProjectAchievement::getProjectId));

        // 构建 Markdown
        StringBuilder md = new StringBuilder();
        md.append("# 个人简历\n\n");

        for (WorkExperience exp : experiences) {
            md.append("## 工作经历：").append(exp.getTitle());
            if (exp.getJob() != null && !exp.getJob().isBlank()) {
                md.append(" - ").append(exp.getJob());
            }
            md.append("\n\n");

            // 工作描述
            if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                md.append("### 工作描述\n").append(exp.getDescription()).append("\n\n");
            }

            // 技术栈
            List<WorkExperienceStack> stacks = stacksByExpId.getOrDefault(exp.getId(), Collections.emptyList());
            if (!stacks.isEmpty()) {
                String stackNames = stacks.stream()
                        .map(WorkExperienceStack::getTechName)
                        .collect(Collectors.joining(", "));
                md.append("### 技术栈\n").append(stackNames).append("\n\n");
            }

            // 项目
            List<Project> projects = projectsByExpId.getOrDefault(exp.getId(), Collections.emptyList());
            for (Project project : projects) {
                md.append("### 项目：").append(project.getName()).append("\n");
                if (project.getLifecycle() != null && !project.getLifecycle().isBlank()) {
                    md.append("- **周期**：").append(project.getLifecycle()).append("\n");
                }
                if (project.getBackground() != null && !project.getBackground().isBlank()) {
                    md.append("- **背景**：").append(project.getBackground()).append("\n");
                }
                if (project.getDuty() != null && !project.getDuty().isBlank()) {
                    md.append("- **职责**：").append(project.getDuty()).append("\n");
                }

                // 项目成果
                List<ProjectAchievement> achievements = achievementsByProjectId.getOrDefault(project.getId(), Collections.emptyList());
                if (!achievements.isEmpty()) {
                    md.append("- **成果**：\n");
                    for (ProjectAchievement ach : achievements) {
                        md.append("  - ").append(ach.getContent()).append("\n");
                    }
                }
                md.append("\n");
            }
        }

        return md.toString().trim();
    }
}
