-- ============================================
-- 系统菜单表 DDL + 初始化数据
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. 系统菜单表 (sys_menu)
-- ============================================
DROP TABLE IF EXISTS sys_menu;

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    url VARCHAR(500) NOT NULL COMMENT '菜单链接',
    icon VARCHAR(100) COMMENT '菜单图标',
    target VARCHAR(20) NOT NULL DEFAULT '_self' COMMENT '打开方式: _self-当前窗口, _blank-新窗口',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 1-显示, 0-隐藏',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_visible (is_visible),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ============================================
-- 2. 初始化数据
-- ============================================
INSERT INTO sys_menu (name, url, icon, target, sort_order, is_visible) VALUES
('首页', '/', '🏠', '_self', 1, 1),
('简历', '/about.html', '👤', '_self', 2, 1),
('简历数据维护', '/resume-manage.html', '📝', '_self', 3, 1),
('AI助手', '/#chat', '🤖', '_self', 4, 1),
('系统设置', '/system-settings.html', '⚙️', '_self', 5, 1);

-- ============================================
-- 3. 验证
-- ============================================
SELECT '菜单数据初始化完成！' AS status;
SELECT COUNT(*) AS menu_count FROM sys_menu;

