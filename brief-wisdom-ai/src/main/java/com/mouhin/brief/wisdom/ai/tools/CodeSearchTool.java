package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.ai.service.ProjectCodeIndexService;
import com.mouhin.brief.wisdom.ai.service.ProjectCodeIndexService.CodeFileIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目代码搜索工具
 * <p>
 * 搜索项目源代码文件，获取代码结构、类名、包名等信息。
 * 替代原有的每次对话都注入代码上下文的方式，改为按需调用。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeSearchTool {

    private final ProjectCodeIndexService projectCodeIndexService;

    /**
     * 搜索项目源代码文件
     *
     * @param keyword 搜索关键词（类名、方法名、文件名等）
     * @return 匹配的代码文件列表
     */
    @Tool(description = "搜索项目源代码文件，获取代码结构、类名、包名等信息。当用户询问项目代码实现、文件位置、类结构、模块关系时调用。")
    public String searchProjectCode(
            @ToolParam(description = "搜索关键词，可以是类名、方法名、文件名、包名") String keyword) {

        log.info("[Tool] searchProjectCode 被调用: keyword={}", keyword);

        try {
            List<CodeFileIndex> files = projectCodeIndexService.searchCodeFiles(keyword);
            if (files.isEmpty()) {
                return "未在项目代码中找到与「" + keyword + "」相关的文件。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("在项目代码中搜索到 ").append(files.size()).append(" 个相关文件：\n\n");

            for (CodeFileIndex file : files) {
                sb.append("📄 ").append(file.getFileName()).append("\n");
                sb.append("   路径: ").append(file.getFilePath()).append("\n");
                if (file.getPackageName() != null) {
                    sb.append("   包名: ").append(file.getPackageName()).append("\n");
                }
                if (file.getClassName() != null) {
                    sb.append("   类名: ").append(file.getClassName()).append("\n");
                }
                if (file.getFileType() != null) {
                    sb.append("   类型: ").append(file.getFileType()).append("\n");
                }
                if (file.getSummary() != null && !file.getSummary().isBlank()) {
                    String summary = file.getSummary().replaceAll("\\s+", " ").trim();
                    if (summary.length() > 200) {
                        summary = summary.substring(0, 200) + "...";
                    }
                    sb.append("   说明: ").append(summary).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] searchProjectCode 执行失败: {}", e.getMessage(), e);
            return "代码搜索失败: " + e.getMessage();
        }
    }

    /**
     * 获取项目结构概览
     *
     * @return 项目模块结构信息
     */
    @Tool(description = "获取项目整体结构概览，包括各模块名称、职责和文件数量。当用户询问项目有哪些模块、整体架构时调用。")
    public String getProjectOverview() {
        log.info("[Tool] getProjectOverview 被调用");
        try {
            return projectCodeIndexService.getProjectStructureOverview();
        } catch (Exception e) {
            log.error("[Tool] getProjectOverview 执行失败: {}", e.getMessage(), e);
            return "获取项目结构失败: " + e.getMessage();
        }
    }
}
