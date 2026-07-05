package com.mouhin.brief.wisdom.ai.controller;

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
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @param userId    用户ID（可选，筛选指定用户）
     * @param auditType 审计类型（可选：INPUT_BLOCKED/OUTPUT_FILTERED/RISK_DETECTED）
     * @param riskLevel 风险等级（可选：LOW/MEDIUM/HIGH/CRITICAL）
     * @return 分页结果
     */
    @Operation(summary = "分页查询审计日志")
    @GetMapping("/logs")
    public Result<PageResult<AiAuditLogDTO>> listAuditLogs(
            @Parameter(description = "页码，从1开始") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "用户ID筛选") @RequestParam(value = "userId", required = false) String userId,
            @Parameter(description = "审计类型筛选") @RequestParam(value = "auditType", required = false) String auditType,
            @Parameter(description = "风险等级筛选") @RequestParam(value = "riskLevel", required = false) String riskLevel) {
        
        int resolvedSize = (size != null && size > 0) ? size : 20;
        PageResult<AiAuditLogDTO> result = auditService.listAuditLogs(page, resolvedSize, userId, auditType, riskLevel);
        
        return Result.success(result);
    }

    /**
     * 获取指定会话的审计日志
     *
     * @param sessionId 会话ID
     * @param page      页码
     * @param size      每页大小
     * @return 分页结果
     */
    @Operation(summary = "获取会话审计日志")
    @GetMapping("/session/{sessionId}")
    public Result<PageResult<AiAuditLogDTO>> getAuditLogsBySession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", required = false) Integer size) {
        
        int resolvedSize = (size != null && size > 0) ? size : 20;
        PageResult<AiAuditLogDTO> result = auditService.getAuditLogsBySession(sessionId, page, resolvedSize);
        
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
