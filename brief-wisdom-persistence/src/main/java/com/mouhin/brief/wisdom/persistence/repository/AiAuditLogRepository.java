package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.AiAuditLogMapper;
import com.mouhin.brief.wisdom.persistence.model.AiAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * AI 审计日志 Repository
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Repository
@RequiredArgsConstructor
public class AiAuditLogRepository {

    private final AiAuditLogMapper mapper;

    /**
     * 保存审计日志
     *
     * @param log 审计日志实体
     */
    public void save(AiAuditLog log) {
        mapper.insert(log);
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
    public Page<AiAuditLog> findByPage(int page, int size, String userId, String auditType, String riskLevel) {
        LambdaQueryWrapper<AiAuditLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null && !userId.isBlank()) {
            wrapper.eq(AiAuditLog::getUserId, userId);
        }
        if (auditType != null && !auditType.isBlank()) {
            wrapper.eq(AiAuditLog::getAuditType, auditType);
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            wrapper.eq(AiAuditLog::getRiskLevel, riskLevel);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(AiAuditLog::getCreateTime);
        
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据会话ID查询审计日志
     *
     * @param sessionId 会话 ID
     * @param page      页码（从 1 开始）
     * @param size      每页大小
     * @return 分页结果
     */
    public Page<AiAuditLog> findBySessionId(String sessionId, int page, int size) {
        LambdaQueryWrapper<AiAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAuditLog::getSessionId, sessionId);
        wrapper.orderByDesc(AiAuditLog::getCreateTime);
        
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 统计指定用户的审计日志数量
     *
     * @param userId 用户 ID
     * @return 审计日志数量
     */
    public long countByUserId(String userId) {
        LambdaQueryWrapper<AiAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAuditLog::getUserId, userId);
        return mapper.selectCount(wrapper);
    }

    /**
     * 统计指定风险等级的审计日志数量
     *
     * @param riskLevel 风险等级
     * @return 审计日志数量
     */
    public long countByRiskLevel(String riskLevel) {
        LambdaQueryWrapper<AiAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAuditLog::getRiskLevel, riskLevel);
        return mapper.selectCount(wrapper);
    }

    /**
     * 按审计类型统计数量
     *
     * @param auditType 审计类型
     * @return 审计日志数量
     */
    public long countByAuditType(String auditType) {
        LambdaQueryWrapper<AiAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAuditLog::getAuditType, auditType);
        return mapper.selectCount(wrapper);
    }

    /**
     * 统计所有审计日志总数
     */
    public long countAll() {
        return mapper.selectCount(new LambdaQueryWrapper<>());
    }
}
