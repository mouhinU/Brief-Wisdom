package com.mouhin.brief.wisdom.common.ai;

import java.time.LocalDateTime;

/**
 * AI 审计日志传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record AiAuditLogDTO(
        Long id,
        String sessionId,
        String userId,
        Long messageId,
        String auditType,
        String riskLevel,
        String triggerKeyword,
        String originalContent,
        String filteredContent,
        String actionTaken,
        Double confidenceScore,
        LocalDateTime createTime
) {

    public Long getId() { return id; }

    public String getSessionId() { return sessionId; }

    public String getUserId() { return userId; }

    public Long getMessageId() { return messageId; }

    public String getAuditType() { return auditType; }

    public String getRiskLevel() { return riskLevel; }

    public String getTriggerKeyword() { return triggerKeyword; }

    public String getOriginalContent() { return originalContent; }

    public String getFilteredContent() { return filteredContent; }

    public String getActionTaken() { return actionTaken; }

    public Double getConfidenceScore() { return confidenceScore; }

    public LocalDateTime getCreateTime() { return createTime; }
}
