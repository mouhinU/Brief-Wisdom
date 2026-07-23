package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.AiAuditLogDTO;
import com.mouhin.brief.wisdom.enums.AiActionTakenEnum;
import com.mouhin.brief.wisdom.enums.AiAuditTypeEnum;
import com.mouhin.brief.wisdom.enums.AiRiskLevelEnum;
import com.mouhin.brief.wisdom.persistence.model.AiAuditLog;
import com.mouhin.brief.wisdom.persistence.repository.AiAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 安全审计服务
 * <p>
 * 负责记录和管理所有 AI 交互过程中的安全审计事件：
 * - 输入内容拦截日志
 * - 输出内容过滤日志
 * - 风险检测与告警
 * - 审计日志查询与统计
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditService {

    private final AiAuditLogRepository auditLogRepository;

    /**
     * 记录输入拦截事件
     *
     * @param sessionId       会话ID
     * @param userId          用户ID
     * @param keyword         触发的关键词
     * @param originalContent 原始内容（脱敏后）
     */
    @Transactional
    public void logInputBlocked(String sessionId, String userId, String keyword, String originalContent) {
        AiAuditLog auditLog = new AiAuditLog();
        auditLog.setSessionId(sessionId);
        auditLog.setUserId(userId);
        auditLog.setAuditType(AiAuditTypeEnum.INPUT_BLOCKED.getCode());
        auditLog.setRiskLevel(AiRiskLevelEnum.HIGH.getCode());
        auditLog.setTriggerKeyword(keyword);
        auditLog.setOriginalContent(truncateContent(originalContent, 500));
        auditLog.setActionTaken(AiActionTakenEnum.BLOCKED.getCode());
        auditLog.setConfidenceScore(1.0);
        auditLog.setCreateTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.warn("[AI审计] 输入拦截: sessionId={}, userId={}, keyword={}", sessionId, userId, keyword);
    }

    /**
     * 记录输出过滤事件
     *
     * @param sessionId       会话ID
     * @param userId          用户ID
     * @param messageId       消息ID
     * @param triggerPattern  触发的模式
     * @param originalContent 原始内容（脱敏后）
     * @param filteredContent 过滤后的内容
     */
    @Transactional
    public void logOutputFiltered(String sessionId, String userId, Long messageId, String triggerPattern,
                                  String originalContent, String filteredContent) {
        AiAuditLog auditLog = new AiAuditLog();
        auditLog.setSessionId(sessionId);
        auditLog.setUserId(userId);
        auditLog.setMessageId(messageId);
        auditLog.setAuditType(AiAuditTypeEnum.OUTPUT_FILTERED.getCode());
        auditLog.setRiskLevel(AiRiskLevelEnum.MEDIUM.getCode());
        auditLog.setTriggerKeyword(triggerPattern);
        auditLog.setOriginalContent(truncateContent(originalContent, 500));
        auditLog.setFilteredContent(truncateContent(filteredContent, 500));
        auditLog.setActionTaken(AiActionTakenEnum.FILTERED.getCode());
        auditLog.setConfidenceScore(0.9);
        auditLog.setCreateTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.info("[AI审计] 输出过滤: sessionId={}, userId={}, pattern={}", sessionId, userId, triggerPattern);
    }

    /**
     * 记录风险检测事件
     *
     * @param sessionId       会话ID
     * @param userId          用户ID
     * @param riskLevel       风险等级（LOW/MEDIUM/HIGH/CRITICAL）
     * @param description     风险描述
     * @param confidenceScore 置信度分数 (0-1)
     */
    @Transactional
    public void logRiskDetected(String sessionId, String userId, String riskLevel, String description, Double confidenceScore) {
        AiAuditLog auditLog = new AiAuditLog();
        auditLog.setSessionId(sessionId);
        auditLog.setUserId(userId);
        auditLog.setAuditType(AiAuditTypeEnum.RISK_DETECTED.getCode());
        auditLog.setRiskLevel(riskLevel != null ? riskLevel : AiRiskLevelEnum.MEDIUM.getCode());
        auditLog.setTriggerKeyword(description);
        auditLog.setActionTaken(AiActionTakenEnum.WARNED.getCode());
        auditLog.setConfidenceScore(confidenceScore != null ? confidenceScore : 0.5);
        auditLog.setCreateTime(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.warn("[AI审计] 风险检测: sessionId={}, userId={}, level={}, desc={}", sessionId, userId, riskLevel, description);
    }

    /**
     * 分页查询审计日志
     *
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @param userId    用户ID（可选）
     * @param auditType 审计类型（可选）
     * @param riskLevel 风险等级（可选）
     * @return 分页结果
     */
    public PageResult<AiAuditLogDTO> listAuditLogs(int page, int size, String userId, String auditType, String riskLevel) {
        Page<AiAuditLog> result = auditLogRepository.findByPage(page, size, userId, auditType, riskLevel);

        List<AiAuditLogDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResult<AiAuditLogDTO> pageResult = new PageResult<>();
        pageResult.setRecords(dtoList);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setPages(result.getPages());
        pageResult.setHasMore(result.getCurrent() < result.getPages());
        return pageResult;
    }

    /**
     * 根据会话ID查询审计日志
     *
     * @param sessionId 会话ID
     * @param page      页码
     * @param size      每页大小
     * @return 分页结果
     */
    public PageResult<AiAuditLogDTO> getAuditLogsBySession(String sessionId, int page, int size) {
        Page<AiAuditLog> result = auditLogRepository.findBySessionId(sessionId, page, size);

        List<AiAuditLogDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResult<AiAuditLogDTO> pageResult = new PageResult<>();
        pageResult.setRecords(dtoList);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setPages(result.getPages());
        pageResult.setHasMore(result.getCurrent() < result.getPages());
        return pageResult;
    }

    /**
     * 获取审计统计信息
     *
     * @return 统计结果
     */
    public AuditStatisticsDTO getAuditStatistics() {
        AuditStatisticsDTO stats = new AuditStatisticsDTO();
        stats.setTotalCount(auditLogRepository.countAll());
        stats.setInputBlockedCount(countByType("INPUT_BLOCKED"));
        stats.setOutputFilteredCount(countByType("OUTPUT_FILTERED"));
        stats.setRiskDetectedCount(countByType("RISK_DETECTED"));
        stats.setHighRiskCount(countByRiskLevel("HIGH"));
        stats.setCriticalRiskCount(countByRiskLevel("CRITICAL"));
        return stats;
    }

    /**
     * 转换为 DTO
     */
    private AiAuditLogDTO convertToDTO(AiAuditLog entity) {
        return new AiAuditLogDTO(
                entity.getId(),
                entity.getSessionId(),
                entity.getUserId(),
                entity.getMessageId(),
                entity.getAuditType(),
                entity.getRiskLevel(),
                entity.getTriggerKeyword(),
                entity.getOriginalContent(),
                entity.getFilteredContent(),
                entity.getActionTaken(),
                entity.getConfidenceScore(),
                entity.getCreateTime()
        );
    }

    /**
     * 按类型统计数量
     */
    private long countByType(String auditType) {
        return auditLogRepository.countByAuditType(auditType);
    }

    /**
     * 按风险等级统计数量
     */
    private long countByRiskLevel(String riskLevel) {
        return auditLogRepository.countByRiskLevel(riskLevel);
    }

    /**
     * 截断内容长度（防止存储过长内容）
     *
     * @param content   原始内容
     * @param maxLength 最大长度
     * @return 截断后的内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 审计统计 DTO
     */
    @lombok.Data
    public static class AuditStatisticsDTO {
        private long totalCount;
        private long inputBlockedCount;
        private long outputFilteredCount;
        private long riskDetectedCount;
        private long highRiskCount;
        private long criticalRiskCount;
    }
}
