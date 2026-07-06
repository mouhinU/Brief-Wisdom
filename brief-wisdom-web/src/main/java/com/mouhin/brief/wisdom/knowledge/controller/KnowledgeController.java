package com.mouhin.brief.wisdom.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
import com.mouhin.brief.wisdom.ai.service.MarkdownImportService;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseRequest;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "知识库", description = "知识库管理与 RAG 检索相关接口")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final MarkdownImportService markdownImportService;

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
    @GetMapping("/bases/paged")
    public Page<KnowledgeBaseDTO> listBasesPaged(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return knowledgeService.listTopBasesPaged(page, size);
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
    @GetMapping("/bases/{baseId}/documents")
    public Page<KnowledgeDocumentDTO> listDocuments(
            @PathVariable Long baseId,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return knowledgeService.listDocuments(baseId, docType, page, size);
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
    @GetMapping("/documents/search")
    public Page<KnowledgeDocumentDTO> searchDocuments(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return knowledgeService.searchDocuments(keyword, page, size);
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
