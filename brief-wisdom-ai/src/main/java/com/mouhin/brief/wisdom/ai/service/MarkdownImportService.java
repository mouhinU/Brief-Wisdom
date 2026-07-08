package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.MarkdownImportResult;
import com.mouhin.brief.wisdom.util.SafePathUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Markdown 文档导入服务
 * 用于将项目中的 Markdown 文件批量导入到知识库
 *
 * @author Brief-Wisdom
 * @date 2026-07-06
 */
@Slf4j
@Service
public class MarkdownImportService {

    private static final String DOC_TYPE_INTERNAL = "INTERNAL";
    private static final String FILE_TYPE_MD = "md";
    private static final int STATUS_PUBLISHED = 1;

    @Autowired
    private KnowledgeService knowledgeService;

    /** 导入根目录，默认为 user.dir */
    @Value("${app.knowledge.import-base-dir:}")
    private String importBaseDir;

    /** 允许导入的相对目录，逗号分隔 */
    @Value("${app.knowledge.import-allowed-dirs:docs}")
    private String importAllowedDirs;

    /** 允许导入的根目录相对文件，逗号分隔 */
    @Value("${app.knowledge.import-allowed-files:AGENTS.md}")
    private String importAllowedFiles;

    /**
     * 批量导入指定目录下的所有 Markdown 文件到知识库
     *
     * @param baseId 目标知识库 ID
     * @param sourceDir 源目录路径（相对于项目根目录）
     * @param recursive 是否递归子目录
     * @return 导入结果（新增/更新/失败计数）
     */
    public MarkdownImportResult importMarkdownFiles(Long baseId, String sourceDir, boolean recursive) {
        Path basePath = resolveSafePath(sourceDir);

        if (!Files.exists(basePath)) {
            log.error("源目录不存在: {}", sourceDir);
            throw new IllegalArgumentException("源目录不存在: " + sourceDir);
        }
        if (!Files.isDirectory(basePath)) {
            throw new IllegalArgumentException("路径不是目录: " + sourceDir);
        }

        MarkdownImportResult result = new MarkdownImportResult();

        try (Stream<Path> paths = recursive ? Files.walk(basePath) : Files.list(basePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> upsertMarkdownFile(baseId, path, result));
        } catch (IOException e) {
            log.error("遍历目录失败: {}", sourceDir, e);
            throw new RuntimeException("遍历目录失败", e);
        }

        log.info("Markdown 文件导入完成 - 新增: {}, 更新: {}, 失败: {}",
                result.getCreatedCount(), result.getUpdatedCount(), result.getFailedCount());
        return result;
    }

    /**
     * 导入 docs 目录下的所有 Markdown 文件到指定知识库
     *
     * @param baseId 目标知识库 ID
     * @return 导入结果
     */
    public MarkdownImportResult importDocsDirectory(Long baseId) {
        return importMarkdownFiles(baseId, "docs", true);
    }

    /**
     * 导入 AGENTS.md 文件到指定知识库
     *
     * @param baseId 目标知识库 ID
     * @return 导入结果
     */
    public MarkdownImportResult importAgentsMd(Long baseId) {
        MarkdownImportResult result = new MarkdownImportResult();
        try {
            Path agentsPath = resolveSafePath("AGENTS.md");
            if (Files.exists(agentsPath)) {
                upsertMarkdownFile(baseId, agentsPath, result);
            } else {
                log.warn("AGENTS.md 文件不存在");
            }
        } catch (Exception e) {
            log.error("导入 AGENTS.md 失败", e);
            result.incrementFailed();
        }
        return result;
    }

    /**
     * 导入或更新单个 Markdown 文件
     */
    private void upsertMarkdownFile(Long baseId, Path filePath, MarkdownImportResult result) {
        try {
            String content = Files.readString(filePath);
            String sourcePath = toRelativeSourcePath(filePath);
            String fileName = filePath.getFileName().toString();
            String title = fileName.substring(0, fileName.lastIndexOf('.'));

            KnowledgeDocumentBO docBO = new KnowledgeDocumentBO();
            docBO.setBaseId(baseId);
            docBO.setTitle(title);
            docBO.setDocType(DOC_TYPE_INTERNAL);
            docBO.setContent(content);
            docBO.setFileType(FILE_TYPE_MD);
            docBO.setFileSize((long) content.getBytes(StandardCharsets.UTF_8).length);
            docBO.setStatus(STATUS_PUBLISHED);
            docBO.setSortOrder(0);

            boolean created = knowledgeService.upsertImportedMarkdown(docBO, sourcePath);
            if (created) {
                result.incrementCreated();
                log.info("新增 Markdown 文档: {}", sourcePath);
            } else {
                result.incrementUpdated();
                log.info("更新 Markdown 文档: {}", sourcePath);
            }
        } catch (Exception e) {
            result.incrementFailed();
            log.error("导入 Markdown 文件失败: {}, 错误: {}", filePath.getFileName(), e.getMessage());
        }
    }

    /**
     * 计算相对项目根目录的源文件路径，用作导入去重键
     */
    private String toRelativeSourcePath(Path filePath) {
        Path baseRoot = SafePathUtils.resolveBaseRoot(importBaseDir);
        return baseRoot.relativize(filePath.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }

    /**
     * 解析并校验导入路径，防止路径穿越
     */
    private Path resolveSafePath(String relativePath) {
        Path baseRoot = SafePathUtils.resolveBaseRoot(importBaseDir);
        List<String> allowedDirs = SafePathUtils.parseCsv(importAllowedDirs);
        List<String> allowedFiles = SafePathUtils.parseCsv(importAllowedFiles);
        return SafePathUtils.resolveAllowedPath(baseRoot, relativePath, allowedDirs, allowedFiles);
    }
}
