-- ============================================
-- AI模型思考模式功能迁移脚本
-- 添加 thinking_mode 字段并初始化现有模型
-- 执行日期: 2026-07-05
-- ============================================

-- 1. 检查并添加 thinking_mode 字段
SET @dbname = DATABASE();
SET @tablename = 'ai_model';
SET @columnname = 'thinking_mode';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) DEFAULT \'normal\' COMMENT \'思考模式: normal-普通模式, thinking-思考模式\' AFTER output_price_per_million')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 初始化现有模型的思考模式（默认都为普通模式）
UPDATE ai_model 
SET thinking_mode = 'normal' 
WHERE thinking_mode IS NULL OR thinking_mode = '';

-- 3. 为特定模型设置思考模式（可选配置）
-- 例如：qwen-max 和 qwen3.7-plus 启用思考模式
UPDATE ai_model SET thinking_mode = 'thinking' WHERE model_name IN ('qwen-max', 'qwen3.7-plus');

-- 4. 验证修改结果
SELECT id, model_name, display_name, provider, thinking_mode 
FROM ai_model 
WHERE is_deleted = 0 
ORDER BY sort_order ASC;
