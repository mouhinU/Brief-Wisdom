-- V11: 将团队管理菜单从 AI助手管理(parent_id=3) 迁移到 系统设置(parent_id=4)
-- 与用户管理、角色管理同级

UPDATE sys_menu SET parent_id = 4, sort_order = 4 WHERE id = 30;
