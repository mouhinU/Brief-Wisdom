-- V7: 添加对话质量评估（用户反馈）字段
-- 用于收集用户对 AI 回复的满意度评价

ALTER TABLE chat_message ADD COLUMN feedback_score TINYINT UNSIGNED NULL COMMENT '用户反馈评分: 1-5（1=很差, 5=很好）';
ALTER TABLE chat_message ADD COLUMN feedback_comment VARCHAR(500) NULL COMMENT '用户反馈备注';
ALTER TABLE chat_message ADD COLUMN feedback_time DATETIME NULL COMMENT '反馈时间';

-- 反馈统计索引（用于后台分析查询）
CREATE INDEX idx_chat_message_feedback ON chat_message(feedback_score, feedback_time);
