package com.mouhin.brief.wisdom.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
import com.mouhin.brief.wisdom.ai.service.MarkdownImportService;
import com.mouhin.brief.wisdom.common.knowledge.*;
import com.mouhin.brief.wisdom.knowledge.req.DocumentListQueryRequest;
import com.mouhin.brief.wisdom.knowledge.req.DocumentSearchQueryRequest;
import com.mouhin.brief.wisdom.knowledge.req.KnowledgeBasePagedQueryRequest;
import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import com.mouhin.brief.wisdom.exception.BizException;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理 REST 接口
 */
/**
 * KnowledgeController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "知识库", description = "知识库管理与 RAG 检索相关接口")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final MarkdownImportService markdownImportService;
    private final UserContextHelper userContextHelper;

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    @Operation(summary = "获取知识库树")
    @GetMapping("/bases/tree")
    public List<KnowledgeBaseDTO> listBasesTree() {
        return knowledgeService.listBasesTree();
    }

    /**
     * 获取所有知识库（平铺列表）
     */
    @Operation(summary = "获取知识库列表", description = "平铺列表")
    @GetMapping("/bases")
    public List<KnowledgeBaseDTO> listBases() {
        return knowledgeService.listBases();
    }

    /**
     * 分页获取顶级知识库
     */
    @PostMapping("/bases/paged")
    public Page<KnowledgeBaseDTO> listBasesPaged(@RequestBody KnowledgeBasePagedQueryRequest request) {
        return knowledgeService.listTopBasesPaged(request.getPage(), request.getSize());
    }

    /**
     * 获取子知识库
     */
    @GetMapping("/bases/{parentId}/children")
    public List<KnowledgeBaseDTO> listChildBases(@PathVariable Long parentId) {
        return knowledgeService.listChildBases(parentId);
    }

    /**
     * 创建知识库
     */
    @PostMapping("/bases")
    public KnowledgeBaseDTO createBase(@RequestBody KnowledgeBaseRequest request) {
        KnowledgeBaseBO bo = convertToBaseBO(request);
        return knowledgeService.createBase(bo);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/bases/{id}")
    public KnowledgeBaseDTO updateBase(@PathVariable Long id, @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBaseBO bo = convertToBaseBO(request);
        return knowledgeService.updateBase(id, bo);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/bases/{id}")
    public Boolean deleteBase(@PathVariable Long id) {
        knowledgeService.deleteBase(id);
        return true;
    }

    // ==================== 文档 ====================

    /**
     * 获取知识库下的文档列表（分页）
     */
    @PostMapping("/bases/{baseId}/documents")
    public Page<KnowledgeDocumentDTO> listDocuments(
            @PathVariable Long baseId,
            @RequestBody DocumentListQueryRequest request) {
        return knowledgeService.listDocuments(baseId, request.getDocType(), request.getPage(), request.getSize());
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/documents/{id}")
    public KnowledgeDocumentDTO getDocument(@PathVariable Long id) {
        return knowledgeService.getDocument(id);
    }

    /**
     * 创建文档
     */
    @PostMapping("/documents")
    public KnowledgeDocumentDTO createDocument(@RequestBody KnowledgeDocumentRequest request) {
        KnowledgeDocumentBO bo = convertToDocumentBO(request);
        return knowledgeService.createDocument(bo);
    }

    /**
     * 更新文档
     */
    @PutMapping("/documents/{id}")
    public KnowledgeDocumentDTO updateDocument(@PathVariable Long id, @RequestBody KnowledgeDocumentRequest request) {
        KnowledgeDocumentBO bo = convertToDocumentBO(request);
        return knowledgeService.updateDocument(id, bo);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    public Boolean deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return true;
    }

    /**
     * 搜索文档
     */
    @Operation(summary = "搜索文档", description = "需要登录才能访问")
    @PostMapping("/documents/search")
    public Page<KnowledgeDocumentDTO> searchDocuments(
            @RequestBody DocumentSearchQueryRequest request,
            HttpServletRequest httpRequest) {
        // 检查用户是否登录
        checkLoginRequired(httpRequest);
        return knowledgeService.searchDocuments(request.getKeyword(), request.getPage(), request.getSize());
    }

    // ==================== Markdown 导入 ====================

    /**
     * 批量导入指定目录下的 Markdown 文件
     */
    @Operation(summary = "导入 Markdown 文件", description = "批量导入指定目录下的所有 .md 文件到知识库")
    @PostMapping("/import/markdown")
    public Integer importMarkdownFiles(
            @RequestParam("baseId") Long baseId,
            @RequestParam("sourceDir") String sourceDir,
            @RequestParam(value = "recursive", defaultValue = "true") boolean recursive) {
        log.info("开始导入 Markdown 文件 - baseId: {}, sourceDir: {}, recursive: {}", baseId, sourceDir, recursive);
        int count = markdownImportService.importMarkdownFiles(baseId, sourceDir, recursive);
        log.info("Markdown 文件导入完成 - 成功导入 {} 个文件", count);
        return count;
    }

    /**
     * 导入 docs 目录下的所有 Markdown 文件
     */
    @Operation(summary = "导入 docs 目录", description = "导入项目 docs 目录下的所有 Markdown 文件")
    @PostMapping("/import/docs")
    public Integer importDocsDirectory(@RequestParam("baseId") Long baseId) {
        log.info("开始导入 docs 目录 - baseId: {}", baseId);
        int count = markdownImportService.importDocsDirectory(baseId);
        log.info("docs 目录导入完成 - 成功导入 {} 个文件", count);
        return count;
    }

    /**
     * 导入 AGENTS.md 文件
     */
    @Operation(summary = "导入 AGENTS.md", description = "导入项目根目录的 AGENTS.md 文件")
    @PostMapping("/import/agents")
    public Integer importAgentsMd(@RequestParam("baseId") Long baseId) {
        log.info("开始导入 AGENTS.md - baseId: {}", baseId);
        int count = markdownImportService.importAgentsMd(baseId);
        log.info("AGENTS.md 导入完成 - 成功导入 {} 个文件", count);
        return count;
    }

    // ==================== 私有转换方法 ====================

    /**
     * 检查用户是否已登录，未登录则抛出异常
     */
    private void checkLoginRequired(HttpServletRequest request) {
        if (!userContextHelper.isLoggedIn()) {
            log.warn("[知识库检索] 访客尝试访问受限接口 - ip: {}", getClientIp(request));
            throw new BizException(BizExceptionEnums.UNAUTHORIZED, "需要登录后才能检索知识库内容");
        }
    }

    /**
     * 获取客户端 IP 地址（用于日志记录）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * Request -> BO (知识库)
     */
    private KnowledgeBaseBO convertToBaseBO(KnowledgeBaseRequest request) {
        KnowledgeBaseBO bo = new KnowledgeBaseBO();
        bo.setName(request.getName());
        bo.setDescription(request.getDescription());
        bo.setIcon(request.getIcon());
        bo.setParentId(request.getParentId());
        bo.setSortOrder(request.getSortOrder());
        bo.setIsPublic(request.getIsPublic());
        return bo;
    }

    /**
     * Request -> BO (知识文档)
     */
    private KnowledgeDocumentBO convertToDocumentBO(KnowledgeDocumentRequest request) {
        KnowledgeDocumentBO bo = new KnowledgeDocumentBO();
        bo.setBaseId(request.getBaseId());
        bo.setTitle(request.getTitle());
        bo.setDocType(request.getDocType());
        bo.setContent(request.getContent());
        bo.setFileUrl(request.getFileUrl());
        bo.setFileName(request.getFileName());
        bo.setFileSize(request.getFileSize());
        bo.setFileType(request.getFileType());
        bo.setLinkUrl(request.getLinkUrl());
        bo.setLinkDesc(request.getLinkDesc());
        bo.setTags(request.getTags());
        bo.setSortOrder(request.getSortOrder());
        bo.setStatus(request.getStatus());
        return bo;
    }
}
