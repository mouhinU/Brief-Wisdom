-- 高频查询字段索引优化

CREATE INDEX idx_chat_session_user_id ON chat_session (user_id);
CREATE INDEX idx_chat_session_user_update ON chat_session (user_id, update_time);
CREATE INDEX idx_chat_message_session_id ON chat_message (session_id);
CREATE INDEX idx_chat_message_session_timestamp ON chat_message (session_id, timestamp);
CREATE INDEX idx_project_experience_id ON project (experience_id);
CREATE INDEX idx_project_achievement_project_id ON project_achievement (project_id);
CREATE INDEX idx_work_experience_stack_experience_id ON work_experience_stack (experience_id);
CREATE INDEX idx_knowledge_document_base_file ON knowledge_document (base_id, file_name(191));
