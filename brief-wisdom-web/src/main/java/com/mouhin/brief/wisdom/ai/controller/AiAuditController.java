package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.req.AiAuditLogQueryRequest;
import com.mouhin.brief.wisdom.ai.service.AiAuditService;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.common.ai.AiAuditLogDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 安全审计 Controller
 * <p>
 * 提供审计日志查询、统计等管理接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@RestController
@RequestMapping("/api/ai/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI管理", description = "AI 安全审计日志查询与统计")
public class AiAuditController {

    private final AiAuditService auditService;

    /**
     * 分页查询审计日志
     *
     * @param request 查询请求（包含页码、每页大小、用户ID、审计类型、风险等级筛选）
     * @return 分页结果
     */
    @Operation(summary = "分页查询审计日志")
    @GetMapping("/logs")
    public Result<PageResult<AiAuditLogDTO>> listAuditLogs(AiAuditLogQueryRequest request) {

        int pageNum = Math.max(request.getPage(), 1);
        int resolvedSize = (request.getSize() >= 1 && request.getSize() <= 100) ? request.getSize() : 20;

        PageResult<AiAuditLogDTO> result = auditService.listAuditLogs(
                pageNum, resolvedSize, request.getUserId(), request.getAuditType(), request.getRiskLevel());

        return Result.success(result);
    }

    /**
     * 获取指定会话的审计日志
     *
     * @param sessionId 会话ID
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @return 分页结果
     */
    @Operation(summary = "获取会话审计日志")
    @GetMapping("/session/{sessionId}")
    public Result<PageResult<AiAuditLogDTO>> getAuditLogsBySession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "20") int size) {

        int pageNum = Math.max(page, 1);
        int resolvedSize = (size >= 1 && size <= 100) ? size : 20;

        PageResult<AiAuditLogDTO> result = auditService.getAuditLogsBySession(sessionId, pageNum, resolvedSize);

        return Result.success(result);
    }

    /**
     * 获取审计统计信息
     *
     * @return 统计数据
     */
    @Operation(summary = "获取审计统计信息")
    @GetMapping("/statistics")
    public Result<AiAuditService.AuditStatisticsDTO> getAuditStatistics() {
        AiAuditService.AuditStatisticsDTO stats = auditService.getAuditStatistics();
        return Result.success(stats);
    }
}
