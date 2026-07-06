-- ============================================
-- AI 安全审计日志表初始化脚本
-- 日期: 2026-07-02
-- 说明: 创建 AI 安全审计日志表及相关索引
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. 删除旧表（如果存在）
-- ============================================
DROP TABLE IF EXISTS ai_audit_log;

-- ============================================
-- 2. 创建 AI 安全审计日志表
-- ============================================
CREATE TABLE ai_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    session_id VARCHAR(36) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    message_id BIGINT COMMENT '关联消息ID',
    audit_type VARCHAR(50) NOT NULL COMMENT '审计类型: INPUT_BLOCKED-输入拦截, OUTPUT_FILTERED-输出过滤, RISK_DETECTED-风险检测',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级: LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重',
    trigger_keyword VARCHAR(500) COMMENT '触发的关键词或模式',
    original_content TEXT COMMENT '原始内容（脱敏后）',
    filtered_content TEXT COMMENT '过滤后的内容',
    action_taken VARCHAR(100) NOT NULL COMMENT '采取的动作: BLOCKED-拦截, FILTERED-过滤, WARNED-警告, ALLOWED-放行',
    confidence_score DOUBLE COMMENT '置信度分数 (0-1)',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引优化查询性能
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_audit_type (audit_type),
    INDEX idx_risk_level (risk_level),
    INDEX idx_create_time (create_time),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 安全审计日志表';

-- ============================================
-- 3. 验证表创建成功
-- ============================================
SELECT 'AI 安全审计日志表创建成功！' AS status;
SHOW TABLES LIKE 'ai_audit_log';
DESC ai_audit_log;
