package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.service.AiAuditService;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.common.ai.AiAuditLogDTO;
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
    @GetMapping("/logs")
    public Result<PageResult<AiAuditLogDTO>> listAuditLogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "auditType", required = false) String auditType,
            @RequestParam(value = "riskLevel", required = false) String riskLevel) {
        
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
    @GetMapping("/session/{sessionId}")
    public Result<PageResult<AiAuditLogDTO>> getAuditLogsBySession(
            @PathVariable String sessionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        
        int resolvedSize = (size != null && size > 0) ? size : 20;
        PageResult<AiAuditLogDTO> result = auditService.getAuditLogsBySession(sessionId, page, resolvedSize);
        
        return Result.success(result);
    }

    /**
     * 获取审计统计信息
     *
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Result<AiAuditService.AuditStatisticsDTO> getAuditStatistics() {
        AiAuditService.AuditStatisticsDTO stats = auditService.getAuditStatistics();
        return Result.success(stats);
    }
}
