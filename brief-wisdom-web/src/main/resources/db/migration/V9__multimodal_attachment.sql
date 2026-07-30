-- V9: 多模态能力 - 附件表 + 模型视觉能力标记
-- 用于支持图片理解等多模态对话：登记上传文件元信息，并标记支持视觉输入的模型

-- ============================================
-- 1. ai_model 增加视觉能力标记字段
-- ============================================
ALTER TABLE ai_model ADD COLUMN supports_vision TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持视觉(图片)输入: 0-不支持, 1-支持';

-- 标记现有支持视觉的模型
UPDATE ai_model SET supports_vision = 1 WHERE model_name IN ('gpt-4o', 'gpt-4o-mini', 'claude-sonnet-4-20250514');

-- 补充 DashScope 视觉模型（默认服务商即可支持图片理解），已存在则跳过
INSERT INTO ai_model (model_name, display_name, provider, description, is_active, is_enabled, sort_order,
                      input_price_per_million, output_price_per_million, thinking_mode, supports_vision)
SELECT 'qwen-vl-max', '通义千问 VL Max', 'dashscope', '通义千问视觉模型，支持图片理解', 0, 1, 5, 20.0, 60.0, 'normal', 1
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_name = 'qwen-vl-max');

-- ============================================
-- 2. 聊天附件表（多模态文件元信息）
-- ============================================
CREATE TABLE IF NOT EXISTS chat_attachment (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id     VARCHAR(64)   NOT NULL COMMENT '上传者用户ID',
    session_id  VARCHAR(64)   NULL COMMENT '所属会话ID（发送消息时回填）',
    message_id  BIGINT        NULL COMMENT '所属消息ID（发送消息时回填）',
    file_name   VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_key    VARCHAR(500)  NOT NULL COMMENT '存储 key',
    file_url    VARCHAR(1000) NOT NULL COMMENT '可访问 URL',
    media_type  VARCHAR(100)  NULL COMMENT 'MIME 类型',
    file_size   BIGINT        NULL COMMENT '文件字节数',
    category    VARCHAR(20)   NOT NULL DEFAULT 'image' COMMENT '附件分类: image-图片, document-文档, audio-音频',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX idx_chat_attachment_user_id (user_id),
    INDEX idx_chat_attachment_session_id (session_id),
    INDEX idx_chat_attachment_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天附件表（多模态文件元信息）';
