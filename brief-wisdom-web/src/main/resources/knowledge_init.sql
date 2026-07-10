-- ============================================
-- 知识库管理表结构
-- ============================================

-- 知识库分类表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) COMMENT '知识库描述',
    icon VARCHAR(50) DEFAULT '📚' COMMENT '图标',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID，0表示顶级',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开: 0-私有, 1-公开',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类表';

-- 知识文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    base_id BIGINT NOT NULL COMMENT '所属知识库ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    doc_type VARCHAR(20) NOT NULL COMMENT '文档类型: INTERNAL-内部文档, FILE-文件, LINK-外部链接',
    content LONGTEXT COMMENT '文档内容（INTERNAL类型使用，富文本HTML）',
    file_url VARCHAR(500) COMMENT '文件URL（FILE类型使用）',
    file_name VARCHAR(200) COMMENT '文件名（FILE类型使用）',
    file_size BIGINT COMMENT '文件大小（字节，FILE类型使用）',
    file_type VARCHAR(50) COMMENT '文件类型/MIME（FILE类型使用）',
    link_url VARCHAR(500) COMMENT '外部链接URL（LINK类型使用）',
    link_desc TEXT COMMENT '链接描述（LINK类型使用，支持Markdown格式）',
    tags VARCHAR(500) COMMENT '标签，逗号分隔',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-草稿, 1-已发布, 2-已归档',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_base_id (base_id),
    INDEX idx_doc_type (doc_type),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档表';

-- 插入默认知识库
INSERT INTO knowledge_base (name, description, icon, sort_order, is_public) VALUES
('默认知识库', '系统默认知识库，用于存放通用文档', '📚', 1, 1),
('技术文档', '技术相关的文档集合', '💻', 2, 1),
('产品文档', '产品相关的文档集合', '📦', 3, 1);

-- 插入示例文档
INSERT INTO knowledge_document (base_id, title, doc_type, content, sort_order, status) VALUES
(1, '欢迎使用知识库', 'INTERNAL', '<h2>欢迎使用简知知识库管理系统</h2><p>这是一个功能强大的知识管理平台，支持：</p><ul><li><strong>内部文档</strong> - 使用富文本编辑器创建文档</li><li><strong>文件上传</strong> - 上传PDF、Word等文件</li><li><strong>外部链接</strong> - 引用外部文档链接</li></ul>', 1, 1);

INSERT INTO knowledge_document (base_id, title, doc_type, link_url, link_desc, sort_order, status) VALUES
(1, 'Spring Boot 官方文档', 'LINK', 'https://spring.io/projects/spring-boot', 'Spring Boot 官方文档，快速开发框架', 2, 1);

SELECT '知识库表结构初始化完成！' AS status;

