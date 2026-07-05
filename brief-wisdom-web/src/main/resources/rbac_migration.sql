-- ============================================
-- RBAC 增量迁移脚本
-- 在已有数据库上执行，不会删除现有数据
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. 增强 sys_menu 表（添加 RBAC 字段）
-- ============================================

-- 添加 parent_id 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'brief_wisdom' AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'parent_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE sys_menu ADD COLUMN parent_id BIGINT DEFAULT 0 COMMENT ''父菜单ID，0表示顶级'' AFTER id', 'SELECT ''parent_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 type 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'brief_wisdom' AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'type');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE sys_menu ADD COLUMN type TINYINT NOT NULL DEFAULT 1 COMMENT ''菜单类型: 0-目录, 1-菜单, 2-按钮'' AFTER target', 'SELECT ''type already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 permission 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'brief_wisdom' AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'permission');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE sys_menu ADD COLUMN permission VARCHAR(200) COMMENT ''权限标识'' AFTER type', 'SELECT ''permission already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 parent_id 索引
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = 'brief_wisdom' AND TABLE_NAME = 'sys_menu' AND INDEX_NAME = 'idx_parent_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE sys_menu ADD INDEX idx_parent_id (parent_id)', 'SELECT ''idx_parent_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有菜单数据：设置 parent_id=0, type=1
UPDATE sys_menu SET parent_id = 0 WHERE parent_id IS NULL;
UPDATE sys_menu SET type = 1 WHERE type IS NULL OR type = 0;

-- ============================================
-- 2. 创建角色表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(100) NOT NULL UNIQUE COMMENT '角色标识',
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
-- 3. 创建用户-角色关联表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============================================
-- 4. 创建角色-菜单关联表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-菜单关联表';

-- ============================================
-- 5. 插入默认角色（避免重复）
-- ============================================
INSERT INTO sys_role (role_name, role_key, description, status) VALUES
('超级管理员', 'super_admin', '拥有系统所有权限', 1),
('管理员', 'admin', '系统管理权限', 1),
('普通用户', 'normal', '基本访问权限', 1)
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- ============================================
-- 6. 更新现有菜单为树形结构
--    将"系统设置"变为目录，添加子菜单
-- ============================================

-- 将"系统设置"改为目录类型 (type=0)
UPDATE sys_menu SET type = 0 WHERE name = '系统设置' AND (parent_id = 0 OR parent_id IS NULL);

-- 插入子菜单（避免重复）
INSERT INTO sys_menu (parent_id, name, url, icon, target, type, permission, sort_order, is_visible)
SELECT m.id, '用户管理', '/system-settings.html?tab=user', '👥', '_self', 1, 'user:list', 1, 1
FROM sys_menu m WHERE m.name = '系统设置' AND m.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '用户管理' AND parent_id = m.id);

INSERT INTO sys_menu (parent_id, name, url, icon, target, type, permission, sort_order, is_visible)
SELECT m.id, '角色管理', '/system-settings.html?tab=role', '🔐', '_self', 1, 'role:list', 2, 1
FROM sys_menu m WHERE m.name = '系统设置' AND m.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '角色管理' AND parent_id = m.id);

INSERT INTO sys_menu (parent_id, name, url, icon, target, type, permission, sort_order, is_visible)
SELECT m.id, '菜单管理', '/system-settings.html?tab=menu', '📋', '_self', 1, 'menu:list', 3, 1
FROM sys_menu m WHERE m.name = '系统设置' AND m.parent_id = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '菜单管理' AND parent_id = m.id);

-- ============================================
-- 7. 为角色分配菜单
-- ============================================

-- 清空角色-菜单关联（重新分配）
DELETE FROM sys_role_menu;

-- 超级管理员：所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE is_deleted = 0;

-- 管理员：除角色管理外的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE is_deleted = 0 AND permission != 'role:list';

-- 普通用户：顶级菜单（首页、简历、AI助手）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE parent_id = 0 AND is_deleted = 0 AND name IN ('首页', '简历', 'AI助手');

-- ============================================
-- 8. 为 admin 用户分配超级管理员角色
-- ============================================
INSERT INTO sys_user_role (user_id, role_id) VALUES ('admin', 1)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ============================================
-- 9. 验证
-- ============================================
SELECT 'RBAC 迁移完成！' AS status;
SELECT COUNT(*) AS role_count FROM sys_role;
SELECT COUNT(*) AS menu_count FROM sys_menu;
SELECT COUNT(*) AS user_role_count FROM sys_user_role;
SELECT COUNT(*) AS role_menu_count FROM sys_role_menu;
