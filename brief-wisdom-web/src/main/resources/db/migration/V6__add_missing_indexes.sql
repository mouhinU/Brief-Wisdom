-- V6: 补充新增表的性能索引
-- 注意: chat_memory 的索引已在 V5 建表时内联创建，此处不再重复

-- chat_reminder: 定时任务按状态+时间查询
CREATE INDEX idx_chat_reminder_status_time ON chat_reminder(status, remind_time);
CREATE INDEX idx_chat_reminder_user_id ON chat_reminder(user_id);

-- knowledge_document: 按知识库查询
CREATE INDEX idx_knowledge_doc_kb_id ON knowledge_document(kb_id);
