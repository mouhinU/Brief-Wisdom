-- 将 link_desc 字段从 VARCHAR(500) 改为 TEXT，支持 Markdown 格式的长文本描述
ALTER TABLE knowledge_document MODIFY COLUMN link_desc TEXT COMMENT '链接描述（LINK类型使用，支持Markdown格式）';
