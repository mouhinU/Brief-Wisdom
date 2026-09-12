-- V13: 修复菜单及其他表中文乱码问题
-- 根因：MySQL 容器默认 client charset 为 latin1，导致 UTF-8 数据被双重编码
-- 此迁移重新插入所有受影响的中文数据

-- 1. 修复 sys_menu
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

INSERT INTO sys_menu (id, parent_id, name, url, icon, target, type, permission, sort_order, is_visible, require_login) VALUES
(1, 0, '首页', '/', '🏠', '_self', 1, NULL, 1, 1, 0),
(2, 0, '简历', '/about.html', '👤', '_self', 1, NULL, 2, 1, 0),
(3, 0, 'AI助手管理', 'ai-manage.html', '🤖', '_self', 3, NULL, 4, 1, 1),
(4, 0, '系统设置', 'system-settings.html', '⚙️', '_self', 1, 'system:settings', 5, 1, 1),
(8, 0, '简历数据管理', 'resume-manage.html', '👤', '_self', 1, NULL, 3, 1, 1),
(21, 0, '知识库', 'knowledge.html', '📚', '_self', 1, NULL, 6, 1, 0),
(5, 4, '用户管理', '/system-settings.html?tab=user', '👥', '_self', 1, 'user:list', 1, 1, 1),
(6, 4, '角色管理', '/system-settings.html?tab=role', '🔐', '_self', 1, 'role:list', 2, 1, 1),
(7, 4, '菜单管理', '/system-settings.html?tab=menu', '📋', '_self', 1, 'menu:list', 3, 1, 1),
(9, 4, '用户管理', '', '👥', '_self', 1, 'user:manage', 6, 0, 1),
(10, 4, '角色管理', '', '🛡️', '_self', 1, 'role:manage', 8, 0, 1),
(11, 4, '菜单管理', '', '📋', '_self', 1, 'menu:manage', 7, 0, 1),
(30, 4, '团队管理', '', '👥', '_self', 1, NULL, 4, 0, 0),
(12, 0, '简历数据管理', NULL, '', '_self', 2, 'resume:manage', 4, 0, 1),
(13, 0, 'AI助手管理', NULL, '', '_self', 2, 'ai:manage', 5, 0, 1),
(14, 3, '模型管理', '', '🧠', '_self', 1, NULL, 9, 0, 0),
(15, 3, '会话历史', '', '💬', '_self', 1, NULL, 10, 0, 0),
(22, 3, '费用统计', '', '💰', '_self', 1, NULL, 11, 0, 0),
(23, 3, 'AI安全审计', '', '🔍', '_self', 1, 'ai:audit', 12, 0, 1),
(16, 8, '在线编辑', '', '✏️', '_self', 1, NULL, 16, 0, 0),
(17, 8, '工作经历', '', '💼', '_self', 1, NULL, 12, 0, 0),
(18, 8, '项目经历', '', '📁', '_self', 1, NULL, 13, 0, 0),
(19, 8, '项目成果', '', '🏆', '_self', 1, NULL, 14, 0, 0),
(20, 8, '技术栈', '', '🔧', '_self', 1, NULL, 15, 0, 0);

-- 角色菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13),
(1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
(1, 21), (1, 22), (1, 23), (1, 30),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 8),
(2, 9), (2, 14), (2, 15), (2, 16), (2, 17), (2, 18), (2, 19), (2, 20),
(2, 21), (2, 22), (2, 23), (2, 30),
(3, 1), (3, 2);

-- 2. 修复 sys_role
UPDATE sys_role SET role_name = '超级管理员', description = '拥有系统所有权限' WHERE id = 1;
UPDATE sys_role SET role_name = '管理员', description = '系统管理权限' WHERE id = 2;
UPDATE sys_role SET role_name = '普通用户', description = '基本访问权限' WHERE id = 3;
