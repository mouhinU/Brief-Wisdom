package com.mouhin.brief.wisdom.ai.service;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 项目代码索引服务
 * <p>
 * 将项目结构和代码作为知识库的一部分，支持 AI 助手理解项目上下文：
 * - 自动扫描项目目录结构
 * - 提取关键文件（Java、配置文件、文档等）的元数据
 * - 构建可检索的代码索引
 * - 与 RAG 系统集成，为 AI 对话提供项目上下文
 *
 * @author Brief-Wisdom
 * @date 2026-07-06
 */
@Slf4j
@Service
public class ProjectCodeIndexService {

    /** 项目根目录（通过 application.yml 配置，支持环境变量和系统属性覆盖） */
    @Value("${app.project.root:${PROJECT_ROOT:/Users/mac/CodeDir/Brief-Wisdom}}")
    private String projectRoot;

    /** 需要索引的文件扩展名 */
    private static final Set<String> INDEXED_EXTENSIONS = Set.of(
            ".java",      // Java 源代码
            ".yml",       // YAML 配置
            ".yaml",      // YAML 配置
            ".properties",// Properties 配置
            ".xml",       // Maven/MyBatis 配置
            ".md",        // Markdown 文档
            ".sql"        // SQL 脚本
    );

    /** 需要排除的目录 */
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "target", "node_modules", ".git", ".idea", ".vscode",
            "logs", ".qoder", ".mvn"
    );

    /** 最大索引文件大小（1MB） */
    private static final long MAX_FILE_SIZE = 1_000_000;

    /** 代码文件索引存储 */
    private final Map<String, CodeFileIndex> codeIndex = new ConcurrentHashMap<>();

    /** 模块结构索引 */
    private final Map<String, ModuleStructure> moduleIndex = new ConcurrentHashMap<>();

    /**
     * 初始化时构建索引
     */
    @PostConstruct
    public void init() {
        log.info("[项目索引] 开始构建项目代码索引...");
        try {
            buildProjectIndex();
            log.info("[项目索引] 索引构建完成，共索引 {} 个文件，{} 个模块", 
                     codeIndex.size(), moduleIndex.size());
        } catch (Exception e) {
            log.error("[项目索引] 索引构建失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据关键词搜索相关代码文件
     *
     * @param keyword 搜索关键词
     * @return 匹配的代码文件列表
     */
    public List<CodeFileIndex> searchCodeFiles(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String lowerKeyword = keyword.toLowerCase();
        List<CodeFileIndex> results = new ArrayList<>();

        for (CodeFileIndex index : codeIndex.values()) {
            double score = calculateRelevanceScore(index, lowerKeyword);
            if (score > 0) {
                index.setRelevanceScore(score);
                results.add(index);
            }
        }

        // 按相关度降序排序
        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        // 返回 Top-10
        return results.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 获取项目结构概览
     *
     * @return 项目模块结构信息
     */
    public String getProjectStructureOverview() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 项目结构概览\n\n");

        for (ModuleStructure module : moduleIndex.values()) {
            sb.append("- **").append(module.getModuleName()).append("**: ")
              .append(module.getDescription()).append("\n");
            sb.append("  - 路径: `").append(module.getRelativePath()).append("`\n");
            sb.append("  - 文件数: ").append(module.getFileCount()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取特定文件的详细信息
     *
     * @param relativePath 文件相对路径
     * @return 文件索引信息
     */
    public CodeFileIndex getFileDetail(String relativePath) {
        return codeIndex.get(relativePath);
    }

    /**
     * 构建项目索引
     */
    private void buildProjectIndex() throws IOException {
        Path rootPath = Paths.get(projectRoot);
        log.info("[项目索引] 项目根目录: {}", projectRoot);

        // 扫描所有文件
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (EXCLUDED_DIRS.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String extension = getFileExtension(fileName);

                if (INDEXED_EXTENSIONS.contains(extension)) {
                    try {
                        indexFile(file, rootPath);
                    } catch (Exception e) {
                        log.warn("[项目索引] 文件索引失败: {}, 错误: {}", file, e.getMessage());
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // 构建模块索引
        buildModuleIndex(rootPath);
    }

    /**
     * 索引单个文件
     */
    private void indexFile(Path filePath, Path projectRoot) throws IOException {
        // 检查文件大小
        long fileSize = Files.size(filePath);
        if (fileSize > MAX_FILE_SIZE) {
            return;
        }

        String relativePath = projectRoot.relativize(filePath).toString();
        String content = Files.readString(filePath);

        CodeFileIndex index = new CodeFileIndex();
        index.setFilePath(relativePath);
        index.setFileName(filePath.getFileName().toString());
        index.setExtension(getFileExtension(relativePath));
        index.setFileSize(fileSize);
        index.setLineCount(content.split("\n").length);

        // 提取关键信息
        extractFileInfo(index, content);

        codeIndex.put(relativePath, index);
    }

    /**
     * 提取文件关键信息
     */
    private void extractFileInfo(CodeFileIndex index, String content) {
        // 提取类名（Java 文件）
        if (".java".equals(index.getExtension())) {
            index.setFileType("Java Class");

            // 提取包名
            int packageStart = content.indexOf("package ");
            if (packageStart != -1) {
                int packageEnd = content.indexOf(";", packageStart);
                if (packageEnd != -1) {
                    index.setPackageName(content.substring(packageStart + 8, packageEnd).trim());
                }
            }

            // 提取类名
            int classStart = content.indexOf("class ");
            if (classStart != -1) {
                int classNameStart = classStart + 6;
                int classNameEnd = findNextWhitespace(content, classNameStart);
                if (classNameEnd != -1) {
                    index.setClassName(content.substring(classNameStart, classNameEnd).trim());
                }
            }

            // 提取注释（前5行）
            String[] lines = content.split("\n");
            StringBuilder comments = new StringBuilder();
            for (int i = 0; i < Math.min(lines.length, 20); i++) {
                String line = lines[i].trim();
                if (line.startsWith("/**") || line.startsWith("*") || line.startsWith("//")) {
                    comments.append(line).append("\n");
                } else if (!line.isEmpty() && comments.isEmpty()) {
                    continue;
                } else if (!line.isEmpty()) {
                    break;
                }
            }
            index.setSummary(comments.toString());
        } else if (".yml".equals(index.getExtension()) || ".yaml".equals(index.getExtension())) {
            index.setFileType("YAML Config");
            index.setSummary("配置文件");
        } else if (".properties".equals(index.getExtension())) {
            index.setFileType("Properties Config");
            index.setSummary("属性配置文件");
        } else if (".xml".equals(index.getExtension())) {
            index.setFileType("XML Config");
            index.setSummary("XML 配置文件");
        } else if (".md".equals(index.getExtension())) {
            index.setFileType("Markdown Document");
            // 提取 Markdown 标题
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.startsWith("#")) {
                    index.setSummary(line);
                    break;
                }
            }
        } else if (".sql".equals(index.getExtension())) {
            index.setFileType("SQL Script");
            index.setSummary("数据库脚本");
        }
    }

    /**
     * 构建模块索引
     */
    private void buildModuleIndex(Path projectRoot) throws IOException {
        // brief-wisdom-web
        addModule("brief-wisdom-web", "Web 层", "Controller、全局配置、拦截器、静态资源");

        // brief-wisdom-ai
        addModule("brief-wisdom-ai", "AI 领域模块", "对话、知识库、模型管理、会话历史、内容过滤、限流");

        // brief-wisdom-system
        addModule("brief-wisdom-system", "系统领域模块", "用户管理、菜单管理、角色管理、登录认证、OAuth 三方登录");

        // brief-wisdom-resume
        addModule("brief-wisdom-resume", "简历领域模块", "简历展示 + CRUD 管理");

        // brief-wisdom-api
        addModule("brief-wisdom-api", "API 层", "对外接口 Controller + DTO");

        // brief-wisdom-service
        addModule("brief-wisdom-service", "Service 层", "通用业务逻辑");

        // brief-wisdom-persistence
        addModule("brief-wisdom-persistence", "DAO 层", "MyBatis-Plus Mapper + JPA Repository + Entity");

        // brief-wisdom-common
        addModule("brief-wisdom-common", "公共模块", "DTO、Result、常量、注解、枚举");
    }

    /**
     * 添加模块信息
     */
    private void addModule(String moduleName, String name, String description) {
        ModuleStructure module = new ModuleStructure();
        module.setModuleName(moduleName);
        module.setName(name);
        module.setDescription(description);
        module.setRelativePath(moduleName);

        // 统计文件数量
        long fileCount = codeIndex.values().stream()
                .filter(idx -> idx.getFilePath().startsWith(moduleName + "/src/main/java/"))
                .count();
        module.setFileCount((int) fileCount);

        moduleIndex.put(moduleName, module);
    }

    /**
     * 计算相关度分数
     */
    private double calculateRelevanceScore(CodeFileIndex index, String keyword) {
        double score = 0.0;

        // 文件名匹配
        if (index.getFileName().toLowerCase().contains(keyword)) {
            score += 3.0;
        }

        // 类名匹配
        if (index.getClassName() != null && index.getClassName().toLowerCase().contains(keyword)) {
            score += 5.0;
        }

        // 包名匹配
        if (index.getPackageName() != null && index.getPackageName().toLowerCase().contains(keyword)) {
            score += 2.0;
        }

        // 路径匹配
        if (index.getFilePath().toLowerCase().contains(keyword)) {
            score += 1.5;
        }

        // 摘要匹配
        if (index.getSummary() != null && index.getSummary().toLowerCase().contains(keyword)) {
            score += 2.0;
        }

        // 文件类型加权
        if ("Java Class".equals(index.getFileType())) {
            score *= 1.2;
        }

        return score;
    }

    /**
     * 查找下一个空白字符位置
     */
    private int findNextWhitespace(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    /**
     * 代码文件索引
     */
    @Data
    public static class CodeFileIndex {
        /** 文件相对路径 */
        private String filePath;

        /** 文件名 */
        private String fileName;

        /** 文件扩展名 */
        private String extension;

        /** 文件类型 */
        private String fileType;

        /** 文件大小（字节） */
        private Long fileSize;

        /** 行数 */
        private Integer lineCount;

        /** 包名 */
        private String packageName;

        /** 类名 */
        private String className;

        /** 文件摘要/注释 */
        private String summary;

        /** 相关度分数 */
        private Double relevanceScore = 0.0;
    }

    /**
     * 模块结构
     */
    @Data
    public static class ModuleStructure {
        /** 模块名称 */
        private String moduleName;

        /** 模块显示名称 */
        private String name;

        /** 模块描述 */
        private String description;

        /** 相对路径 */
        private String relativePath;

        /** 文件数量 */
        private Integer fileCount;
    }
}
