-- V10: 团队管理 + 知识库团队归属 + 共享授权
-- 支持三级角色（owner/admin/member）、团队级和个人级知识库共享

-- 1. 团队表
CREATE TABLE IF NOT EXISTS team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    name VARCHAR(100) NOT NULL COMMENT '团队名称',
    description VARCHAR(500) COMMENT '团队描述',
    avatar VARCHAR(50) DEFAULT '👥' COMMENT '团队头像/图标',
    owner_id VARCHAR(64) NOT NULL COMMENT '拥有者用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_owner_id (owner_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- 2. 团队成员表
CREATE TABLE IF NOT EXISTS team_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: owner-拥有者, admin-管理员, member-成员',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE INDEX uk_team_user (team_id, user_id, is_deleted),
    INDEX idx_user_id (user_id),
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员表';

-- 3. 扩展知识库表：增加归属信息
ALTER TABLE knowledge_base ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '归属类型: personal-个人, team-团队' AFTER is_public;
ALTER TABLE knowledge_base ADD COLUMN owner_id VARCHAR(64) COMMENT '归属ID（用户ID或团队ID）' AFTER owner_type;
ALTER TABLE knowledge_base ADD COLUMN creator_id VARCHAR(64) COMMENT '创建者用户ID' AFTER owner_id;
ALTER TABLE knowledge_base ADD INDEX idx_owner (owner_type, owner_id);

-- 4. 知识库共享授权表
CREATE TABLE IF NOT EXISTS kb_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    kb_id BIGINT NOT NULL COMMENT '知识库ID',
    target_type VARCHAR(20) NOT NULL COMMENT '授权目标类型: team-团队, user-个人',
    target_id VARCHAR(64) NOT NULL COMMENT '授权目标ID（团队ID或用户ID）',
    permission VARCHAR(20) NOT NULL DEFAULT 'read' COMMENT '权限: read-只读',
    shared_by VARCHAR(64) NOT NULL COMMENT '授权人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE INDEX uk_kb_target (kb_id, target_type, target_id, is_deleted),
    INDEX idx_target (target_type, target_id),
    INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库共享授权表';

-- 5. 菜单：团队管理（AI助手管理子菜单，parent_id=3）
INSERT INTO sys_menu (id, parent_id, name, url, icon, target, type, permission, sort_order, is_visible, require_login)
SELECT 30, 3, '团队管理', '', '👥', '_self', 1, NULL, 8, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30);

-- 6. 角色菜单权限：超级管理员(1) + 管理员(2) 可见团队管理
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 30 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 30);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, 30 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 30);
