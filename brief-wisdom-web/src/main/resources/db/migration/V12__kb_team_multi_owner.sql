-- V12: 知识库-团队多对多关联表
-- 支持一个知识库属于多个团队，也支持无团队属性（个人知识库）

-- 1. 知识库-团队关联表
CREATE TABLE IF NOT EXISTS kb_team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    kb_id BIGINT NOT NULL COMMENT '知识库ID',
    team_id BIGINT NOT NULL COMMENT '团队ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    UNIQUE INDEX uk_kb_team (kb_id, team_id, is_deleted),
    INDEX idx_team_id (team_id),
    INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库-团队关联表';

-- 2. 迁移存量数据：将已有团队归属的知识库写入关联表
INSERT INTO kb_team (kb_id, team_id)
SELECT kb.id, CAST(kb.owner_id AS UNSIGNED)
FROM knowledge_base kb
WHERE kb.owner_type = 'team'
  AND kb.owner_id IS NOT NULL
  AND kb.owner_id != ''
  AND kb.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM kb_team kt
      WHERE kt.kb_id = kb.id AND kt.team_id = CAST(kb.owner_id AS UNSIGNED)
  );
