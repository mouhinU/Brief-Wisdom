package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 批量导入指定目录下的所有 Markdown 文件到知识库
     *
     * @param baseId 目标知识库 ID
     * @param sourceDir 源目录路径（相对于项目根目录）
     * @param recursive 是否递归子目录
     * @return 成功导入的文件数量
     */
    public int importMarkdownFiles(Long baseId, String sourceDir, boolean recursive) {
        Path basePath = Paths.get(sourceDir);
        
        if (!Files.exists(basePath)) {
            log.error("源目录不存在: {}", sourceDir);
            throw new IllegalArgumentException("源目录不存在: " + sourceDir);
        }

        int[] successCount = {0};
        int[] failCount = {0};

        try (Stream<Path> paths = recursive ? Files.walk(basePath) : Files.list(basePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> {
                     try {
                         importSingleFile(baseId, path);
                         successCount[0]++;
                         log.info("成功导入 Markdown 文件: {}", path.getFileName());
                     } catch (Exception e) {
                         failCount[0]++;
                         log.error("导入 Markdown 文件失败: {}, 错误: {}", path.getFileName(), e.getMessage());
                     }
                 });
        } catch (IOException e) {
            log.error("遍历目录失败: {}", sourceDir, e);
            throw new RuntimeException("遍历目录失败", e);
        }

        log.info("Markdown 文件导入完成 - 成功: {}, 失败: {}", successCount[0], failCount[0]);
        return successCount[0];
    }

    /**
     * 导入单个 Markdown 文件
     *
     * @param baseId 知识库 ID
     * @param filePath 文件路径
     */
    private void importSingleFile(Long baseId, Path filePath) throws IOException {
        // 读取文件内容
        String content = Files.readString(filePath);
        
        // 获取文件名（不含扩展名）作为标题
        String fileName = filePath.getFileName().toString();
        String title = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // 创建文档业务对象
        KnowledgeDocumentBO docBO = new KnowledgeDocumentBO();
        docBO.setBaseId(baseId);
        docBO.setTitle(title);
        docBO.setDocType("INTERNAL"); // Markdown 作为内部文档存储
        docBO.setContent(content); // 直接存储 Markdown 原文
        docBO.setStatus(1); // 已发布
        docBO.setSortOrder(0);
        
        // 调用知识库服务创建文档
        knowledgeService.createDocument(docBO);
    }

    /**
     * 导入 docs 目录下的所有 Markdown 文件到指定知识库
     *
     * @param baseId 目标知识库 ID
     * @return 成功导入的文件数量
     */
    public int importDocsDirectory(Long baseId) {
        return importMarkdownFiles(baseId, "docs", true);
    }

    /**
     * 导入 AGENTS.md 文件到指定知识库
     *
     * @param baseId 目标知识库 ID
     * @return 成功导入的文件数量
     */
    public int importAgentsMd(Long baseId) {
        try {
            Path agentsPath = Paths.get("AGENTS.md");
            if (Files.exists(agentsPath)) {
                importSingleFile(baseId, agentsPath);
                return 1;
            } else {
                log.warn("AGENTS.md 文件不存在");
                return 0;
            }
        } catch (IOException e) {
            log.error("导入 AGENTS.md 失败", e);
            return 0;
        }
    }
}
