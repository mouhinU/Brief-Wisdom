-- ============================================
-- AI 模型配置表 DDL + 初始化数据
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. AI 模型配置表 (ai_model)
-- ============================================
DROP TABLE IF EXISTS ai_model;

CREATE TABLE ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    model_name VARCHAR(100) NOT NULL COMMENT '模型标识(如 qwen-max, qwen-plus)',
    display_name VARCHAR(200) NOT NULL COMMENT '显示名称',
    provider VARCHAR(50) NOT NULL DEFAULT 'dashscope' COMMENT '服务商(dashscope/openai等)',
    description VARCHAR(500) COMMENT '模型描述',
    is_active TINYINT NOT NULL DEFAULT 0 COMMENT '是否激活(同时只激活一个): 1-激活, 0-未激活',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1-启用, 0-禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_model_name (model_name),
    INDEX idx_is_active (is_active),
    INDEX idx_is_enabled (is_enabled),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';

-- ============================================
-- 2. 初始化数据
-- ============================================
INSERT INTO ai_model (model_name, display_name, provider, description, is_active, is_enabled, sort_order) VALUES
('qwen-max', '通义千问 Max', 'dashscope', '最强模型，适合复杂任务', 0, 1, 1),
('qwen-plus', '通义千问 Plus', 'dashscope', '均衡模型，性价比高', 1, 1, 2),
('qwen-turbo', '通义千问 Turbo', 'dashscope', '快速模型，响应最快', 0, 1, 3),
('qwen3.7-plus', 'Qwen3.7-Plus', 'dashscope', 'Qwen3.7 Plus，新一代增强模型', 0, 1, 4);

-- 确保只有一个激活模型
UPDATE ai_model SET is_active = 0;
UPDATE ai_model SET is_active = 1 WHERE model_name = 'qwen-plus';

-- ============================================
-- 3. 验证
-- ============================================
SELECT 'AI模型配置初始化完成！' AS status;
SELECT COUNT(*) AS model_count FROM ai_model;
SELECT model_name, display_name, is_active FROM ai_model;

