package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 审计日志 DTO
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Data
public class AiAuditLogDTO {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 关联消息ID
     */
    private Long messageId;

    /**
     * 审计类型
     */
    private String auditType;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 触发的关键词或模式
     */
    private String triggerKeyword;

    /**
     * 原始内容（脱敏后）
     */
    private String originalContent;

    /**
     * 过滤后的内容
     */
    private String filteredContent;

    /**
     * 采取的动作
     */
    private String actionTaken;

    /**
     * 置信度分数 (0-1)
     */
    private Double confidenceScore;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
