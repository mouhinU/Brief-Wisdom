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
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS chat_user;
DROP TABLE IF EXISTS project_achievement;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS work_experience_stack;
DROP TABLE IF EXISTS work_experience;

CREATE TABLE chat_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID (UUID)',
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名',
    nickname VARCHAR(200) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    password VARCHAR(200) DEFAULT NULL COMMENT 'BCrypt加密后的密码',
    user_level VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '用户级别: admin-管理员, vip-会员, normal-普通用户',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username)
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
    INDEX idx_provider (provider)
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
    page_context VARCHAR(200) COMMENT '页面上下文（如 /about.html, /resume-manage.html）',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
-- 5. 工作经历表 (work_experience)
-- ============================================
CREATE TABLE work_experience (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    title VARCHAR(500) NOT NULL COMMENT '职位标题',
    job VARCHAR(200) NOT NULL COMMENT '岗位角色',
    description TEXT COMMENT '整体描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 1-显示, 0-隐藏',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_visible (is_visible),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作经历表';

-- ============================================
-- 6. 项目表 (project)
-- ============================================
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    experience_id BIGINT NOT NULL COMMENT '关联 work_experience.id',
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    lifecycle VARCHAR(100) COMMENT '项目周期',
    background TEXT COMMENT '项目背景',
    duty TEXT COMMENT '职责描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_experience_id (experience_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ============================================
-- 7. 项目成果表 (project_achievement)
-- ============================================
CREATE TABLE project_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    project_id BIGINT NOT NULL COMMENT '关联 project.id',
    content TEXT NOT NULL COMMENT '成果内容',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成果表';

-- ============================================
-- 8. 技术栈表 (work_experience_stack)
-- ============================================
CREATE TABLE work_experience_stack (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    experience_id BIGINT NOT NULL COMMENT '关联 work_experience.id',
    tech_name VARCHAR(100) NOT NULL COMMENT '技术名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_experience_id (experience_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作经历技术栈表';

-- ============================================
-- 9. 系统菜单表 (sys_menu) - RBAC
-- ============================================
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID，0表示顶级',
    name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    url VARCHAR(500) DEFAULT NULL COMMENT '菜单链接',
    icon VARCHAR(100) COMMENT '菜单图标',
    target VARCHAR(20) NOT NULL DEFAULT '_self' COMMENT '打开方式: _self-当前窗口, _blank-新窗口',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '菜单类型: 0-目录, 1-菜单, 2-按钮',
    permission VARCHAR(200) COMMENT '权限标识，如 user:list',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 1-显示, 0-隐藏',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_visible (is_visible),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ============================================
-- 10. 系统角色表 (sys_role) - RBAC
-- ============================================
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(100) NOT NULL UNIQUE COMMENT '角色标识，如 super_admin',
    description VARCHAR(500) COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role_key (role_key),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ============================================
-- 11. 用户-角色关联表 (sys_user_role) - RBAC
-- ============================================
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============================================
-- 12. 角色-菜单关联表 (sys_role_menu) - RBAC
-- ============================================
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

-- ============================================
-- 13. 初始化数据
-- ============================================

-- 插入默认用户
INSERT INTO chat_user (user_id, username, nickname, avatar) VALUES
('default-user', 'guest', '访客', NULL)
ON DUPLICATE KEY UPDATE username=username;
INSERT INTO chat_user (user_id, username, nickname, avatar) VALUES('admin', 'mougin', 'mouhin', NULL);

-- 插入默认角色
INSERT INTO sys_role (role_name, role_key, description, status) VALUES
('超级管理员', 'super_admin', '拥有系统所有权限', 1),
('管理员', 'admin', '系统管理权限', 1),
('普通用户', 'normal', '基本访问权限', 1);

-- 插入菜单数据（树形结构）
-- 顶级菜单
INSERT INTO sys_menu (parent_id, name, url, icon, target, type, permission, sort_order, is_visible) VALUES
(0, '首页', '/', '🏠', '_self', 1, NULL, 1, 1),
(0, '简历', '/about.html', '👤', '_self', 2, NULL, 2, 1),
(0, 'AI助手', '/#chat', '🤖', '_self', 3, NULL, 3, 1),
(0, '系统设置', NULL, '⚙️', '_self', 0, NULL, 4, 1);

-- 获取系统设置菜单ID
SET @system_menu_id = (SELECT id FROM sys_menu WHERE name = '系统设置' AND parent_id = 0 LIMIT 1);

-- 系统设置子菜单
INSERT INTO sys_menu (parent_id, name, url, icon, target, type, permission, sort_order, is_visible) VALUES
(@system_menu_id, '用户管理', '/system-settings.html?tab=user', '👥', '_self', 1, 'user:list', 1, 1),
(@system_menu_id, '角色管理', '/system-settings.html?tab=role', '🔐', '_self', 1, 'role:list', 2, 1),
(@system_menu_id, '菜单管理', '/system-settings.html?tab=menu', '📋', '_self', 1, 'menu:list', 3, 1);

-- 为超级管理员分配所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 为管理员分配除角色管理外的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id NOT IN (SELECT id FROM sys_menu WHERE permission = 'role:list');

-- 为普通用户分配基本菜单（首页、简历、AI助手）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE parent_id = 0 AND name IN ('首页', '简历', 'AI助手');

-- 为管理员用户分配管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
('admin', 1);

-- ============================================
-- 14. 验证
-- ============================================
SELECT '数据库初始化完成！' AS status;
SELECT COUNT(*) AS user_count FROM chat_user;
SELECT COUNT(*) AS role_count FROM sys_role;
SELECT COUNT(*) AS user_role_count FROM sys_user_role;
SELECT COUNT(*) AS menu_count FROM sys_menu;
SELECT COUNT(*) AS role_menu_count FROM sys_role_menu;

