-- ============================================
-- Brief-Wisdom 数据库初始化脚本
-- 基于 MyBatis-Plus
-- 数据库: MySQL 8.0+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS brief_wisdom 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE brief_wisdom;

-- ============================================
-- 1. 用户表 (chat_user)
-- ============================================
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS user_oauth;
DROP TABLE IF EXISTS chat_user;

CREATE TABLE chat_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID (UUID)',
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名',
    nickname VARCHAR(200) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_create_time (create_time),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 用户 OAuth 绑定表 (user_oauth)
--    存储各平台（微信/钉钉/QQ/支付宝）的 OpenID 绑定关系
-- ============================================
CREATE TABLE user_oauth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(36) NOT NULL COMMENT '关联 chat_user.user_id',
    provider VARCHAR(32) NOT NULL COMMENT '平台标识: wechat / dingtalk / qq / alipay',
    openid VARCHAR(128) NOT NULL COMMENT '平台 OpenID（平台内唯一标识）',
    unionid VARCHAR(128) COMMENT '平台 UnionID（微信/钉钉跨应用唯一标识）',
    nickname VARCHAR(200) COMMENT '该平台显示昵称',
    avatar VARCHAR(500) COMMENT '该平台头像URL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_provider_openid (provider, openid),
    INDEX idx_user_id (user_id),
    INDEX idx_provider (provider),
    FOREIGN KEY (user_id) REFERENCES chat_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 OAuth 绑定表';

-- ============================================
-- 3. 会话表 (chat_session)
-- ============================================
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    session_id VARCHAR(36) NOT NULL UNIQUE COMMENT '会话ID (UUID)',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    description TEXT COMMENT '会话描述',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_update_time (update_time),
    INDEX idx_create_time (create_time),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (user_id) REFERENCES chat_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================
-- 4. 消息表 (chat_message)
-- ============================================
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID (自增)',
    session_id VARCHAR(36) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    model VARCHAR(500) COMMENT 'AI模型名称',
    tokens INT COMMENT 'Token数量',
    cost DOUBLE COMMENT '费用',
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
    message_type VARCHAR(50) COMMENT '消息类型: text/image/code等',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_role (role),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES chat_user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================
-- 5. 初始化数据
-- ============================================

-- 插入默认用户
INSERT INTO chat_user (user_id, username, nickname, avatar) VALUES
('default-user', 'guest', '访客', NULL)
ON DUPLICATE KEY UPDATE username=username;
INSERT INTO chat_user (user_id, username, nickname, avatar) VALUES('admin', 'mougin', 'mouhin', NULL);

-- ============================================
-- 6. 验证
-- ============================================
SELECT '数据库初始化完成！' AS status;
SELECT COUNT(*) AS user_count FROM chat_user;
SELECT COUNT(*) AS session_count FROM chat_session;
SELECT COUNT(*) AS message_count FROM chat_message;
SELECT COUNT(*) AS oauth_count FROM user_oauth;

