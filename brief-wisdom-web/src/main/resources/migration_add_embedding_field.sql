-- ============================================
-- 知识库向量化检索功能 - 数据库迁移脚本
-- 添加 embedding 字段用于存储向量嵌入
-- ============================================

-- 1. 为 knowledge_document 表添加 embedding 字段
ALTER TABLE knowledge_document 
ADD COLUMN embedding LONGTEXT COMMENT '向量嵌入(JSON格式数组，用于语义检索)';

-- 2. 添加索引优化查询性能(可选，embedding字段较大不建议建索引)
-- 注意：LONGTEXT类型不支持普通索引，如需高性能检索请使用Milvus等向量数据库

SELECT '知识库向量化字段添加完成！' AS status;
