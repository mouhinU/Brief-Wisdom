package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

/**
 * AI 审计日志分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
public class AiAuditLogQueryRequest {

    /** 页码，从1开始，默认1 */
    private int page = 1;

    /** 每页大小，默认20，最大100 */
    private int size = 20;

    /** 用户ID筛选（可选） */
    private String userId;

    /** 审计类型筛选（可选：INPUT_BLOCKED/OUTPUT_FILTERED/RISK_DETECTED） */
    private String auditType;

    /** 风险等级筛选（可选：LOW/MEDIUM/HIGH/CRITICAL） */
    private String riskLevel;
}