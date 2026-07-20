package com.mouhin.brief.wisdom.ai.req;

import com.mouhin.brief.wisdom.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 审计日志分页查询请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiAuditLogQueryRequest extends PageRequest {

    /**
     * 用户ID筛选（可选）
     */
    private String userId;

    /**
     * 审计类型筛选（可选：INPUT_BLOCKED/OUTPUT_FILTERED/RISK_DETECTED）
     */
    private String auditType;

    /**
     * 风险等级筛选（可选：LOW/MEDIUM/HIGH/CRITICAL）
     */
    private String riskLevel;
}