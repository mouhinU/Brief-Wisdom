-- ============================================
-- AI 审计日志表添加 update_time 字段迁移脚本
-- 日期: 2026-07-06
-- 说明: 为 ai_audit_log 表添加缺失的 update_time 字段
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. 检查字段是否存在，不存在则添加
-- ============================================
-- MySQL 不支持 ALTER TABLE ... IF NOT EXISTS，需要先判断
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'brief_wisdom' 
      AND TABLE_NAME = 'ai_audit_log' 
      AND COLUMN_NAME = 'update_time'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE ai_audit_log ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
    'SELECT ''字段 update_time 已存在，无需添加'' AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. 验证字段添加成功
-- ============================================
SELECT 'AI 审计日志表 update_time 字段迁移完成！' AS status;
DESC ai_audit_log;
