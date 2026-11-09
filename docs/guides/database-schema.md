# 数据库表结构与配置

> Brief-Wisdom 数据库设计文档，包含表结构关系、MyBatis-Plus 配置、逻辑删除和自动填充机制。

**技术栈**：Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 / MySQL 8.0+

---

## 数据库连接信息

- **主机**：localhost:3306
- **数据库名**：brief_wisdom
- **字符集**：utf8mb4（支持 emoji）

### 初始化方式

**方式一：执行一体化初始化脚本（推荐）**

```bash
mysql -u root -p < brief-wisdom-web/src/main/resources/all-init.sql
```

该脚本包含：创建数据库、18 张表结构（含 RBAC 表）、默认用户（`default-user/guest` 和 `admin/mouhin`
）、默认角色（super_admin/admin/normal）、树形菜单、AI 模型配置、简历示例数据、知识库初始数据。

> **警告**：当前数据库实际共有 **23 张表**，`all-init.sql` 停留在 18 张表，**缺少** V9~V12 新增的
> `chat_attachment`（V9）、`team` / `team_member` / `kb_share`（V10）、`kb_team`（V12）共 5 张表。用 `all-init.sql`
> 初始化的库需要另外补建这些表；新建库建议以 Flyway 迁移（V1~V13）为准。

> `all-init.sql` 已包含 Flyway V1~V7 的所有变更，V8~V13 的变更需通过 Flyway 迁移补齐。项目同时使用 Flyway
> 管理增量迁移（V1 基线 → … → V13 修复中文字符集，完整清单见文末「Flyway 数据库迁移」）。

**方式二：应用启动时自动建表**

MyBatis-Plus 启动后会自动创建表结构（需提前创建数据库），但 RBAC 初始数据仍需手动执行。

### 数据库备份与恢复

```bash
mysqldump -u root -p brief_wisdom > backup_$(date +%Y%m%d).sql
mysql -u root -p brief_wisdom < backup_20260629.sql
```

---

## 数据关系

```
chat_user (用户)
    ├── chat_session (会话) [一对多]
    │       └── chat_message (消息) [一对多]
    ├── user_oauth (第三方登录绑定) [一对多]
    ├── ai_audit_log (AI审计日志) [一对多]
    ├── chat_memory (对话记忆·会话内隔离) [一对多]
    ├── chat_reminder (定时提醒) [一对多]
    └── sys_user_role (用户角色关联) [一对多]
            └── sys_role (角色)

sys_role (角色)
    └── sys_role_menu (角色菜单关联) [一对多]
            └── sys_menu (菜单)

sys_menu (系统菜单) [树形结构，parent_id 自引用]

ai_model (AI模型配置) [独立]

knowledge_base (知识库) [树形结构，parent_id 自引用]
    └── knowledge_document (知识文档) [一对多]

work_experience (工作经历)
    ├── project (项目) [一对多]
    │       └── project_achievement (项目成果) [一对多]
    └── work_experience_stack (技术栈) [一对多]
```

---

## 表结构详情

> 所有表均包含公共字段：`id`（BIGINT AUTO_INCREMENT 主键）、`create_time`（DATETIME）、`update_time`（DATETIME）、`is_deleted`（TINYINT 逻辑删除），
> 由 `BaseEntity` 基类统一定义。以下仅列出业务字段。

### chat_user（用户表）

| 字段          | 类型           | 说明                     |
|-------------|--------------|------------------------|
| user_id     | VARCHAR(36)  | 用户ID (UUID, UNIQUE)    |
| username    | VARCHAR(100) | 用户名 (UNIQUE)           |
| nickname    | VARCHAR(200) | 昵称                     |
| avatar      | VARCHAR(500) | 头像URL                  |
| password    | VARCHAR(200) | BCrypt加密后的密码           |
| phone       | VARCHAR(20)  | 手机号 (UNIQUE)           |
| user_level  | VARCHAR(20)  | 用户级别: admin/vip/normal |

### chat_session（会话表）

| 字段            | 类型           | 说明                   |
|---------------|--------------|----------------------|
| session_id    | VARCHAR(36)  | 会话ID (UUID, UNIQUE)  |
| user_id       | VARCHAR(36)  | 用户ID                 |
| title         | VARCHAR(200) | 会话标题                 |
| description   | TEXT         | 会话描述                 |
| page_context  | VARCHAR(200) | 页面上下文（如 /about.html） |
| message_count | INT          | 消息数量                 |

### chat_message（消息表）

| 字段               | 类型              | 说明                     |
|------------------|-----------------|------------------------|
| session_id       | VARCHAR(36)     | 会话ID                   |
| user_id          | VARCHAR(36)     | 用户ID                   |
| role             | VARCHAR(20)     | 角色 (user/assistant)    |
| content          | LONGTEXT        | 消息内容                   |
| model            | VARCHAR(500)    | AI模型名称                 |
| tokens           | INT             | Token数量                |
| cost             | DOUBLE          | 费用                     |
| timestamp        | DATETIME        | 消息时间                   |
| message_type     | VARCHAR(50)     | 消息类型 (text/image/code) |
| feedback_score   | TINYINT UNSIGNED | 用户反馈评分: 1-5（V7 新增）   |
| feedback_comment | VARCHAR(500)    | 用户反馈备注（V7 新增）        |
| feedback_time    | DATETIME        | 反馈时间（V7 新增）          |

### ai_model（AI模型配置表）

| 字段                       | 类型           | 说明                     |
|--------------------------|--------------|------------------------|
| model_name               | VARCHAR(100) | 模型标识(如 qwen-max)       |
| display_name             | VARCHAR(200) | 显示名称                   |
| provider                 | VARCHAR(50)  | 服务商(dashscope/openai等) |
| description              | VARCHAR(500) | 模型描述                   |
| is_active                | TINYINT      | 是否激活(同时只激活一个)          |
| is_enabled               | TINYINT      | 是否启用                   |
| sort_order               | INT          | 排序序号                   |
| input_price_per_million  | DOUBLE       | 每百万输入token价格(元)        |
| output_price_per_million | DOUBLE       | 每百万输出token价格(元)        |
| thinking_mode            | VARCHAR(20)  | 思考模式: normal/thinking  |

### user_oauth（第三方登录绑定表）

| 字段       | 类型           | 说明                              |
|----------|--------------|---------------------------------|
| user_id  | VARCHAR(36)  | 关联 chat_user.user_id            |
| provider | VARCHAR(32)  | 平台标识: wechat/dingtalk/qq/alipay |
| openid   | VARCHAR(128) | 平台 OpenID                       |
| unionid  | VARCHAR(128) | 平台 UnionID                      |
| nickname | VARCHAR(200) | 该平台显示昵称                         |
| avatar   | VARCHAR(500) | 该平台头像URL                        |

**唯一约束**：`UNIQUE KEY uk_provider_openid (provider, openid)`

### sys_menu（系统菜单表）

| 字段            | 类型           | 说明                     |
|---------------|--------------|------------------------|
| parent_id     | BIGINT       | 父菜单ID，0表示顶级            |
| name          | VARCHAR(100) | 菜单名称                   |
| url           | VARCHAR(500) | 菜单链接                   |
| icon          | VARCHAR(100) | 菜单图标                   |
| target        | VARCHAR(20)  | 打开方式: _self/_blank     |
| type          | TINYINT      | 菜单类型: 0-目录, 1-菜单, 2-按钮 |
| permission    | VARCHAR(200) | 权限标识（如 `user:list`）    |
| sort_order    | INT          | 排序序号                   |
| is_visible    | TINYINT      | 是否显示: 1-显示, 0-隐藏       |
| require_login | TINYINT      | 是否需要登录: 0-否, 1-是       |

### sys_role（角色表）

| 字段          | 类型           | 说明             |
|-------------|--------------|----------------|
| role_name   | VARCHAR(100) | 角色名称           |
| role_key    | VARCHAR(100) | 角色标识 (UNIQUE)  |
| description | VARCHAR(500) | 角色描述           |
| status      | TINYINT      | 状态: 1-启用, 0-禁用 |

### sys_user_role（用户-角色关联表）

| 字段      | 类型          | 说明   |
|---------|-------------|------|
| user_id | VARCHAR(36) | 用户ID |
| role_id | BIGINT      | 角色ID |

**唯一约束**：`UNIQUE KEY uk_user_role (user_id, role_id)`

### sys_role_menu（角色-菜单关联表）

| 字段      | 类型     | 说明   |
|---------|--------|------|
| role_id | BIGINT | 角色ID |
| menu_id | BIGINT | 菜单ID |

**唯一约束**：`UNIQUE KEY uk_role_menu (role_id, menu_id)`

### work_experience（工作经历表）

| 字段          | 类型           | 说明               |
|-------------|--------------|------------------|
| title       | VARCHAR(500) | 职位标题             |
| job         | VARCHAR(200) | 岗位角色             |
| description | TEXT         | 整体描述             |
| sort_order  | INT          | 排序序号             |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏 |

### project（项目表）

| 字段            | 类型           | 说明                    |
|---------------|--------------|-----------------------|
| experience_id | BIGINT       | 关联 work_experience.id |
| name          | VARCHAR(200) | 项目名称                  |
| lifecycle     | VARCHAR(100) | 项目周期                  |
| background    | TEXT         | 项目背景                  |
| duty          | TEXT         | 职责描述                  |
| sort_order    | INT          | 排序序号                  |

### project_achievement（项目成果表）

| 字段         | 类型   | 说明            |
|------------|------|---------------|
| project_id | BIGINT | 关联 project.id |
| content    | TEXT | 成果内容          |
| sort_order | INT  | 排序序号          |

### work_experience_stack（技术栈表）

| 字段            | 类型           | 说明                    |
|---------------|--------------|-----------------------|
| experience_id | BIGINT       | 关联 work_experience.id |
| tech_name     | VARCHAR(100) | 技术名称                  |
| sort_order    | INT          | 排序序号                  |

### ai_audit_log（AI 审计日志表）

| 字段               | 类型           | 说明                                                |
|------------------|--------------|---------------------------------------------------|
| session_id       | VARCHAR(36)  | 会话 ID                                             |
| user_id          | VARCHAR(36)  | 用户 ID                                             |
| message_id       | BIGINT       | 关联消息 ID                                           |
| audit_type       | VARCHAR(50)  | 审计类型: INPUT_BLOCKED/OUTPUT_FILTERED/RISK_DETECTED |
| risk_level       | VARCHAR(20)  | 风险等级: LOW/MEDIUM/HIGH/CRITICAL                    |
| trigger_keyword  | VARCHAR(500) | 触发的关键词或模式                                         |
| original_content | TEXT         | 原始内容（脱敏后）                                         |
| filtered_content | TEXT         | 过滤后的内容                                            |
| action_taken     | VARCHAR(100) | 采取的动作: BLOCKED/FILTERED/WARNED/ALLOWED            |
| confidence_score | DOUBLE       | 置信度分数 (0-1)                                       |

### knowledge_base（知识库分类表）

| 字段          | 类型           | 说明                          |
|-------------|--------------|-----------------------------|
| name        | VARCHAR(100) | 知识库名称                       |
| description | VARCHAR(500) | 知识库描述                       |
| icon        | VARCHAR(50)  | 图标（默认 📚）                   |
| parent_id   | BIGINT       | 父级ID，0表示顶级                  |
| sort_order  | INT          | 排序序号                        |
| is_public   | TINYINT      | 是否公开: 0-私有, 1-公开            |

### knowledge_document（知识文档表）

| 字段          | 类型           | 说明                                      |
|-------------|--------------|-----------------------------------------|
| base_id     | BIGINT       | 所属知识库ID（关联 knowledge_base.id）           |
| title       | VARCHAR(200) | 文档标题                                    |
| doc_type    | VARCHAR(20)  | 文档类型: INTERNAL/FILE/LINK                |
| content     | LONGTEXT     | 文档内容（INTERNAL类型使用，富文本HTML）              |
| file_url    | VARCHAR(500) | 文件URL（FILE类型使用）                         |
| file_name   | VARCHAR(200) | 文件名（FILE类型使用）                           |
| file_size   | BIGINT       | 文件大小（字节，FILE类型使用）                       |
| file_type   | VARCHAR(50)  | 文件类型/MIME（FILE类型使用）                     |
| link_url    | VARCHAR(500) | 外部链接URL（LINK类型使用）                       |
| link_desc   | TEXT         | 链接描述（LINK类型使用，支持Markdown格式）             |
| tags        | VARCHAR(500) | 标签，逗号分隔                                 |
| view_count  | INT          | 浏览次数                                    |
| sort_order  | INT          | 排序序号                                    |
| status      | TINYINT      | 状态: 0-草稿, 1-已发布, 2-已归档                  |

### chat_memory（对话记忆表·会话内隔离）

| 字段                | 类型              | 说明                             |
|-------------------|-----------------|--------------------------------|
| user_id           | VARCHAR(64)     | 用户 ID                          |
| category          | VARCHAR(50)     | 记忆类别: preference/fact/context  |
| memory_key        | VARCHAR(200)    | 记忆键（如 name、tech_stack、company） |
| memory_value      | TEXT            | 记忆值                            |
| source_session_id | VARCHAR(64)     | 来源会话 ID（记忆按来源会话隔离）            |
| access_count      | INT             | 访问次数（默认 0）                     |

> ChatMemory 读取时按 `source_session_id` 过滤，同一用户不同会话之间的记忆相互隔离，并非跨会话共享。

### chat_reminder（定时提醒表）

| 字段            | 类型              | 说明                             |
|---------------|-----------------|--------------------------------|
| user_id       | VARCHAR(64)     | 用户 ID                          |
| reminder_text | TEXT            | 提醒内容                           |
| remind_time   | DATETIME        | 提醒时间                           |
| status        | TINYINT UNSIGNED | 状态: 0-待处理, 1-已完成, 2-已过期, 3-已取消 |

### chat_attachment（聊天附件表，V9 新增）

用途：多模态附件元信息登记（图片理解链路）。

| 字段         | 类型           | 说明                              |
|------------|--------------|---------------------------------|
| user_id    | VARCHAR(64)  | 上传者用户 ID                         |
| session_id | VARCHAR(64)  | 所属会话 ID（发送消息时回填）                 |
| message_id | BIGINT       | 所属消息 ID（发送消息时回填）                 |
| file_name  | VARCHAR(255) | 原始文件名                            |
| file_key   | VARCHAR(500) | 存储 key                           |
| file_url   | VARCHAR(1000) | 可访问 URL                          |
| media_type | VARCHAR(100) | MIME 类型                          |
| file_size  | BIGINT       | 文件字节数                            |
| category   | VARCHAR(20)  | 附件分类: image-图片, document-文档, audio-音频 |

**索引**：`idx_chat_attachment_user_id (user_id)`、`idx_chat_attachment_session_id (session_id)`、`idx_chat_attachment_message_id (message_id)`

### team（团队表，V10 新增）

用途：团队协作与知识库共享授权。

| 字段          | 类型           | 说明                    |
|-------------|--------------|-----------------------|
| name        | VARCHAR(100) | 团队名称                  |
| description | VARCHAR(500) | 团队描述                  |
| avatar      | VARCHAR(50)  | 团队头像/图标（默认 👥）        |
| owner_id    | VARCHAR(64)  | 拥有者用户 ID               |

**索引**：`idx_owner_id (owner_id)`、`idx_is_deleted (is_deleted)`

### team_member（团队成员表，V10 新增）

用途：团队协作与知识库共享授权。

| 字段      | 类型           | 说明                                  |
|---------|--------------|-------------------------------------|
| team_id | BIGINT       | 团队 ID                                |
| user_id | VARCHAR(64)  | 用户 ID                                |
| role    | VARCHAR(20)  | 角色: owner-拥有者, admin-管理员, member-成员 |

**唯一约束**：`UNIQUE INDEX uk_team_user (team_id, user_id, is_deleted)`
**索引**：`idx_user_id (user_id)`、`idx_team_id (team_id)`

### kb_share（知识库共享授权表，V10 新增）

用途：团队协作与知识库共享授权。

| 字段          | 类型           | 说明                        |
|-------------|--------------|---------------------------|
| kb_id       | BIGINT       | 知识库 ID                     |
| target_type | VARCHAR(20)  | 授权目标类型: team-团队, user-个人  |
| target_id   | VARCHAR(64)  | 授权目标 ID（团队 ID 或用户 ID）     |
| permission  | VARCHAR(20)  | 权限: read-只读（目前仅支持 read）  |
| shared_by   | VARCHAR(64)  | 授权人用户 ID                   |

**唯一约束**：`UNIQUE INDEX uk_kb_target (kb_id, target_type, target_id, is_deleted)`
**索引**：`idx_target (target_type, target_id)`、`idx_kb_id (kb_id)`

### kb_team（知识库-团队关联表，V12 新增）

用途：团队协作与知识库共享授权（支持一个知识库归属多个团队，即多 owner）。

| 字段      | 类型     | 说明     |
|---------|--------|--------|
| kb_id   | BIGINT | 知识库 ID |
| team_id | BIGINT | 团队 ID  |

**唯一约束**：`UNIQUE INDEX uk_kb_team (kb_id, team_id, is_deleted)`
**索引**：`idx_team_id (team_id)`、`idx_kb_id (kb_id)`

---

## 主键策略

所有表均使用 `@TableId(type = IdType.AUTO)` 自增主键，`user_id` / `session_id` 为业务唯一键（UNIQUE）。

| 表             | 主键                         | 业务键                             |
|---------------|----------------------------|---------------------------------|
| chat_user     | `id` BIGINT AUTO_INCREMENT | `user_id` VARCHAR(36) UNIQUE    |
| chat_session  | `id` BIGINT AUTO_INCREMENT | `session_id` VARCHAR(36) UNIQUE |
| chat_message  | `id` BIGINT AUTO_INCREMENT | -                               |
| user_oauth    | `id` BIGINT AUTO_INCREMENT | `UNIQUE(provider, openid)`      |
| ai_model      | `id` BIGINT AUTO_INCREMENT | `model_name` VARCHAR(100)       |
| sys_menu      | `id` BIGINT AUTO_INCREMENT | -                               |
| sys_role      | `id` BIGINT AUTO_INCREMENT | `role_key` VARCHAR(100) UNIQUE  |
| sys_user_role | `id` BIGINT AUTO_INCREMENT | `UNIQUE(user_id, role_id)`      |
| sys_role_menu | `id` BIGINT AUTO_INCREMENT | `UNIQUE(role_id, menu_id)`      |
| chat_memory   | `id` BIGINT AUTO_INCREMENT | -                               |
| chat_reminder | `id` BIGINT AUTO_INCREMENT | -                               |

> 按业务键查询时需使用 `LambdaQueryWrapper`，而非 `selectById()`。表设计不使用数据库外键，关联关系通过应用层维护。

---

## MyBatis-Plus 配置

### application.yml

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.mouhin.brief.wisdom.persistence.model
  configuration:
    map-underscore-to-camel-case: true   # 驼峰转下划线
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL日志
  global-config:
    db-config:
      id-type: assign_uuid               # 全局默认策略（实际由实体 @TableId 覆盖）
      logic-delete-field: is_deleted     # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 分页插件

已配置 MyBatis-Plus 分页插件（最大单页 1000 条），并通过 `PaginationProperties` 实现按业务维度配置分页参数：

```yaml
app:
  pagination:
    session-list:
      default-size: 20    # 会话列表每页条数
      max-size: 100
    message-history:
      default-size: 20    # 消息历史每页条数
      max-size: 200
```

前端可通过 `GET /api/ai/config/pagination` 获取分页配置，避免硬编码。

---

## 逻辑删除

所有表都包含 `is_deleted` 字段，实现软删除。

### 实体类配置

```java
@TableLogic
@TableField(value = "is_deleted")
private Integer isDeleted;  // 0-未删除, 1-已删除
```

### 工作原理

| 操作 | 实际 SQL                                         |
|----|------------------------------------------------|
| 删除 | `UPDATE table SET is_deleted = 1 WHERE id = ?` |
| 查询 | `SELECT * FROM table WHERE is_deleted = 0`     |

### 注意事项

- MyBatis-Plus 内置方法（`selectById`, `deleteById`）自动处理逻辑删除
- 自定义 `@Select` 需手动添加 `AND is_deleted = 0` 条件
- 逻辑删除后数据仍可恢复：`UPDATE table SET is_deleted = 0 WHERE ...`

---

## 自动填充

通过 `MetaObjectHandler` 实现时间字段自动填充。

### 配置（MybatisPlusConfig.java）

```java
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "timestamp", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 字段映射

| Java 字段    | 数据库字段       | 填充时机            |
|------------|-------------|-----------------|
| createTime | create_time | INSERT          |
| updateTime | update_time | INSERT + UPDATE |
| timestamp  | timestamp   | INSERT（消息表）     |
| isDeleted  | is_deleted  | INSERT（默认 0）    |

> `strictInsertFill` 只在字段值为 null 时填充，不会覆盖手动设置的值。

---

## Flyway 数据库迁移

项目使用 Flyway 管理数据库版本迁移，迁移脚本位于 `brief-wisdom-web/src/main/resources/db/migration/` 目录下。

| 版本 | 脚本文件                                   | 说明                                                   |
|----|----------------------------------------|------------------------------------------------------|
| V1 | `V1__baseline.sql`                     | 基线标记（实际建表由 all-init.sql 或 knowledge_init.sql 完成）     |
| V2 | `V2__add_performance_indexes.sql`      | 补充高频查询字段性能索引                                         |
| V3 | `V3__alter_link_desc_to_text.sql`      | knowledge_document.link_desc 从 VARCHAR(500) 扩展为 TEXT |
| V4 | `V4__create_chat_reminder_table.sql`   | 创建 chat_reminder 定时提醒表                               |
| V5 | `V5__create_chat_memory_table.sql`     | 创建 chat_memory 对话记忆表（会话内隔离）                          |
| V6 | `V6__add_missing_indexes.sql`          | 补充 chat_reminder 性能索引                                |
| V7 | `V7__add_feedback_fields.sql`          | chat_message 新增反馈字段（feedback_score/comment/time）     |
| V8 | `V8__init_resume_knowledge_base.sql`   | 初始化简历知识库数据                                          |
| V9 | `V9__multimodal_attachment.sql`        | ai_model 增加 supports_vision 视觉能力标记；创建 chat_attachment 多模态附件表 |
| V10 | `V10__team_and_kb_share.sql`           | 创建 team/team_member/kb_share 表；knowledge_base 增加 owner_type/owner_id/creator_id 归属字段 |
| V11 | `V11__move_team_menu_to_system_settings.sql` | 将团队相关菜单迁移到系统设置下                                |
| V12 | `V12__kb_team_multi_owner.sql`         | 创建 kb_team 关联表，支持知识库归属多个团队（多 owner）                |
| V13 | `V13__fix_chinese_charset.sql`         | 修复中文字符集                                              |

---

**最后更新**: 2026-07-21