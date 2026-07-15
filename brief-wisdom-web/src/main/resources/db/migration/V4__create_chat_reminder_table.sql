-- 用户提醒事项表
CREATE TABLE IF NOT EXISTS chat_reminder (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id     VARCHAR(64)  NOT NULL COMMENT '用户ID',
    reminder_text TEXT       NOT NULL COMMENT '提醒内容',
    remind_time DATETIME     NOT NULL COMMENT '提醒时间',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理, 1-已完成, 2-已过期, 3-已取消',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX idx_reminder_user_status (user_id, status),
    INDEX idx_reminder_time (remind_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提醒事项表';
