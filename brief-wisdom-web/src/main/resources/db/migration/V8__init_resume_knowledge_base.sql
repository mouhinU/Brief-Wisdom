-- 初始化「我的简历」知识库，用于简历数据自动同步到知识库参与 RAG 检索
INSERT INTO knowledge_base (name, description, parent_id, sort_order, is_public, create_time, update_time)
SELECT '我的简历', '简历数据自动同步，包含工作经历、项目经验、技术栈等', 0, 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE name = '我的简历');
