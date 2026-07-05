-- ============================================
-- 数据库逻辑删除字段补全脚本
-- 日期: 2026-07-05
-- 说明: 为缺少 is_deleted 字段的表添加逻辑删除字段及索引
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. sys_role 表添加 is_deleted 字段
-- ============================================
ALTER TABLE sys_role 
ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除' AFTER status,
ADD INDEX idx_is_deleted (is_deleted);

-- ============================================
-- 2. ai_audit_log 表添加 is_deleted 字段（如果不存在）
-- ============================================
-- 检查字段是否存在，避免重复添加
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'brief_wisdom' 
    AND TABLE_NAME = 'ai_audit_log' 
    AND COLUMN_NAME = 'is_deleted'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE ai_audit_log ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-未删除, 1-已删除'' AFTER confidence_score',
    'SELECT ''ai_audit_log.is_deleted 字段已存在，跳过'' AS info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'brief_wisdom' 
    AND TABLE_NAME = 'ai_audit_log' 
    AND INDEX_NAME = 'idx_is_deleted'
);

SET @sql = IF(@index_exists = 0, 
    'ALTER TABLE ai_audit_log ADD INDEX idx_is_deleted (is_deleted)',
    'SELECT ''ai_audit_log.idx_is_deleted 索引已存在，跳过'' AS info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. 验证修改结果
-- ============================================
SELECT '逻辑删除字段补全完成！' AS status;

-- 验证 sys_role 表结构
DESC sys_role;

-- 验证 ai_audit_log 表结构
DESC ai_audit_log;

-- 检查所有继承 BaseEntity 的表是否都有 is_deleted 字段
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'brief_wisdom'
AND COLUMN_NAME = 'is_deleted'
ORDER BY TABLE_NAME;

