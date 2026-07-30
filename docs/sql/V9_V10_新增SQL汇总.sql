-- ============================================================
-- Brief-Wisdom 新增 SQL 汇总（V9 + V10）
-- 整理日期: 2026-07-25
-- ============================================================


-- ************************************************************
-- V9: 多模态能力 - 附件表 + 模型视觉能力标记
-- 用途: 支持图片理解等多模态对话
-- ************************************************************

-- 1. ai_model 增加视觉能力标记字段
ALTER TABLE ai_model
    ADD COLUMN supports_vision TINYINT NOT NULL DEFAULT 0
    COMMENT '是否支持视觉(图片)输入: 0-不支持, 1-支持';

-- 标记现有支持视觉的模型
UPDATE ai_model SET supports_vision = 1
WHERE model_name IN ('gpt-4o', 'gpt-4o-mini', 'claude-sonnet-4-20250514');

-- 补充 DashScope 视觉模型（已存在则跳过）
INSERT INTO ai_model (model_name, display_name, provider, description,
                      is_active, is_enabled, sort_order,
                      input_price_per_million, output_price_per_million,
                      thinking_mode, supports_vision)
SELECT 'qwen-vl-max', '通义千问 VL Max', 'dashscope', '通义千问视觉模型，支持图片理解',
       0, 1, 5, 20.0, 60.0, 'normal', 1
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE model_name = 'qwen-vl-max');

-- 2. 聊天附件表（多模态文件元信息）
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


-- ************************************************************
-- V10: 团队管理 + 知识库团队归属 + 共享授权
-- 用途: 三级角色(owner/admin/member)、团队级和个人级知识库共享
-- ************************************************************

-- 1. 团队表
CREATE TABLE IF NOT EXISTS team (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    name        VARCHAR(100) NOT NULL COMMENT '团队名称',
    description VARCHAR(500) COMMENT '团队描述',
    avatar      VARCHAR(50) DEFAULT '👥' COMMENT '团队头像/图标',
    owner_id    VARCHAR(64) NOT NULL COMMENT '拥有者用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX idx_owner_id (owner_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- 2. 团队成员表
CREATE TABLE IF NOT EXISTS team_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    team_id     BIGINT NOT NULL COMMENT '团队ID',
    user_id     VARCHAR(64) NOT NULL COMMENT '用户ID',
    role        VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: owner-拥有者, admin-管理员, member-成员',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    UNIQUE INDEX uk_team_user (team_id, user_id, is_deleted),
    INDEX idx_user_id (user_id),
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员表';

-- 3. 扩展知识库表：增加归属信息
ALTER TABLE knowledge_base
    ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT 'personal' COMMENT '归属类型: personal-个人, team-团队' AFTER is_public;
ALTER TABLE knowledge_base
    ADD COLUMN owner_id VARCHAR(64) COMMENT '归属ID（用户ID或团队ID）' AFTER owner_type;
ALTER TABLE knowledge_base
    ADD COLUMN creator_id VARCHAR(64) COMMENT '创建者用户ID' AFTER owner_id;
ALTER TABLE knowledge_base
    ADD INDEX idx_owner (owner_type, owner_id);

-- 4. 知识库共享授权表
CREATE TABLE IF NOT EXISTS kb_share (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    kb_id       BIGINT NOT NULL COMMENT '知识库ID',
    target_type VARCHAR(20) NOT NULL COMMENT '授权目标类型: team-团队, user-个人',
    target_id   VARCHAR(64) NOT NULL COMMENT '授权目标ID（团队ID或用户ID）',
    permission  VARCHAR(20) NOT NULL DEFAULT 'read' COMMENT '权限: read-只读',
    shared_by   VARCHAR(64) NOT NULL COMMENT '授权人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

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
