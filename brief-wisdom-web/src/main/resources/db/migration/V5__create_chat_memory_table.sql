-- 用户记忆表（AI 对话上下文记忆持久化）
CREATE TABLE IF NOT EXISTS chat_memory (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id           VARCHAR(64)  NOT NULL COMMENT '用户ID',
    category          VARCHAR(50)  NOT NULL COMMENT '记忆分类: preference-偏好, fact-事实, context-上下文',
    memory_key        VARCHAR(200) NOT NULL COMMENT '记忆键（如: preferred_language, tech_stack）',
    memory_value      TEXT         NOT NULL COMMENT '记忆值',
    source_session_id VARCHAR(64)  NULL     COMMENT '来源会话ID',
    access_count      INT          NOT NULL DEFAULT 0 COMMENT '访问次数（用于权重排序）',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX idx_chat_memory_user_id (user_id),
    INDEX idx_chat_memory_user_category (user_id, category),
    INDEX idx_chat_memory_access_count (user_id, access_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记忆表';

