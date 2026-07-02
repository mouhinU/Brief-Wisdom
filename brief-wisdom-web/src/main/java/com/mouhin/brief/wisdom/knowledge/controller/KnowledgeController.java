package com.mouhin.brief.wisdom.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseRequest;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentRequest;
import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
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
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    // ==================== 知识库 ====================

    /**
     * 获取所有知识库（树形结构）
     */
    @GetMapping("/bases/tree")
    public List<KnowledgeBaseDTO> listBasesTree() {
        return knowledgeService.listBasesTree();
    }

    /**
     * 获取所有知识库（平铺列表）
     */
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
        return knowledgeService.createBase(request);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/bases/{id}")
    public KnowledgeBaseDTO updateBase(@PathVariable Long id, @RequestBody KnowledgeBaseRequest request) {
        return knowledgeService.updateBase(id, request);
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
        return knowledgeService.createDocument(request);
    }

    /**
     * 更新文档
     */
    @PutMapping("/documents/{id}")
    public KnowledgeDocumentDTO updateDocument(@PathVariable Long id, @RequestBody KnowledgeDocumentRequest request) {
        return knowledgeService.updateDocument(id, request);
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
}
