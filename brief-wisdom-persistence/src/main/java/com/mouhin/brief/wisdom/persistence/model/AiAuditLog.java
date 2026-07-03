package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 安全审计日志实体类
 * <p>
 * 记录所有 AI 交互过程中的安全审计事件，包括：
 * - 输入内容拦截（违规关键词）
 * - 输出内容过滤（敏感信息）
 * - 风险检测告警（异常行为模式）
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Data
@TableName("ai_audit_log")
public class AiAuditLog {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
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
     * - INPUT_BLOCKED: 输入拦截
     * - OUTPUT_FILTERED: 输出过滤
     * - RISK_DETECTED: 风险检测
     */
    private String auditType;

    /**
     * 风险等级
     * - LOW: 低
     * - MEDIUM: 中
     * - HIGH: 高
     * - CRITICAL: 严重
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
     * - BLOCKED: 拦截
     * - FILTERED: 过滤
     * - WARNED: 警告
     * - ALLOWED: 放行
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
