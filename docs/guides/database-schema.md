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
mysql -u root -p < brief-wisdom-web/src/main/resources/init-20260629.sql
```

该脚本包含：创建数据库、所有表结构（含 RBAC 表）、默认用户（`default-user/guest` 和 `admin/mouhin`
）、默认角色（super_admin/admin/normal）、树形菜单、AI 模型配置、简历示例数据。

**其他 SQL 脚本**：

| 脚本文件                 | 说明           |
|----------------------|--------------|
| `init-20260629.sql`  | 一体化初始化脚本（推荐） |
| `init.sql`           | 基础建表脚本       |
| `ai_model_init.sql`  | AI 模型初始化数据   |
| `knowledge_init.sql` | 知识库初始数据      |
| `resume_init.sql`    | 简历示例数据       |
| `menu_init.sql`      | 菜单初始化        |
| `rbac_migration.sql` | RBAC 权限迁移脚本  |
| `ai_audit_init.sql`  | AI 审计日志表建表脚本 |

**方式二：应用启动时自动建表**

MyBatis-Plus 启动后会自动创建表结构（需提前创建数据库），但 RBAC 初始数据仍需手动执行 `rbac_migration.sql`。

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
    ├── chat_memory (跨会话记忆) [一对多]
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

### chat_user（用户表）

| 字段          | 类型           | 说明                     |
|-------------|--------------|------------------------|
| id          | BIGINT       | 自增主键                   |
| user_id     | VARCHAR(36)  | 用户ID (UUID, UNIQUE)    |
| username    | VARCHAR(100) | 用户名 (UNIQUE)           |
| nickname    | VARCHAR(200) | 昵称                     |
| avatar      | VARCHAR(500) | 头像URL                  |
| password    | VARCHAR(200) | BCrypt加密后的密码           |
| user_level  | VARCHAR(20)  | 用户级别: admin/vip/normal |
| create_time | DATETIME     | 创建时间                   |
| update_time | DATETIME     | 更新时间                   |
| is_deleted  | TINYINT      | 逻辑删除: 0-未删除, 1-已删除     |

### chat_session（会话表）

| 字段            | 类型           | 说明                   |
|---------------|--------------|----------------------|
| id            | BIGINT       | 自增主键                 |
| session_id    | VARCHAR(36)  | 会话ID (UUID, UNIQUE)  |
| user_id       | VARCHAR(36)  | 用户ID                 |
| title         | VARCHAR(200) | 会话标题                 |
| description   | TEXT         | 会话描述                 |
| page_context  | VARCHAR(200) | 页面上下文（如 /about.html） |
| message_count | INT          | 消息数量                 |
| create_time   | DATETIME     | 创建时间                 |
| update_time   | DATETIME     | 更新时间                 |
| is_deleted    | TINYINT      | 逻辑删除                 |

### chat_message（消息表）

| 字段           | 类型           | 说明                     |
|--------------|--------------|------------------------|
| id           | BIGINT       | 自增主键                   |
| session_id   | VARCHAR(36)  | 会话ID                   |
| user_id      | VARCHAR(36)  | 用户ID                   |
| role         | VARCHAR(20)  | 角色 (user/assistant)    |
| content      | LONGTEXT     | 消息内容                   |
| model        | VARCHAR(500) | AI模型名称                 |
| tokens       | INT          | Token数量                |
| cost         | DOUBLE       | 费用                     |
| timestamp    | DATETIME     | 消息时间                   |
| message_type | VARCHAR(50)  | 消息类型 (text/image/code) |
| is_deleted   | TINYINT      | 逻辑删除                   |

### ai_model（AI模型配置表）

| 字段                       | 类型           | 说明                     |
|--------------------------|--------------|------------------------|
| id                       | BIGINT       | 自增主键                   |
| model_name               | VARCHAR(100) | 模型标识(如 qwen-max)       |
| display_name             | VARCHAR(200) | 显示名称                   |
| provider                 | VARCHAR(50)  | 服务商(dashscope/openai等) |
| description              | VARCHAR(500) | 模型描述                   |
| is_active                | TINYINT      | 是否激活(同时只激活一个)          |
| is_enabled               | TINYINT      | 是否启用                   |
| sort_order               | INT          | 排序序号                   |
| input_price_per_million  | DOUBLE       | 每百万输入token价格(元)        |
| output_price_per_million | DOUBLE       | 每百万输出token价格(元)        |
| create_time              | DATETIME     | 创建时间                   |
| update_time              | DATETIME     | 更新时间                   |
| is_deleted               | TINYINT      | 逻辑删除                   |

### user_oauth（第三方登录绑定表）

| 字段          | 类型           | 说明                              |
|-------------|--------------|---------------------------------|
| id          | BIGINT       | 自增主键                            |
| user_id     | VARCHAR(36)  | 关联 chat_user.user_id            |
| provider    | VARCHAR(32)  | 平台标识: wechat/dingtalk/qq/alipay |
| openid      | VARCHAR(128) | 平台 OpenID                       |
| unionid     | VARCHAR(128) | 平台 UnionID                      |
| nickname    | VARCHAR(200) | 该平台显示昵称                         |
| avatar      | VARCHAR(500) | 该平台头像URL                        |
| create_time | DATETIME     | 绑定时间                            |
| update_time | DATETIME     | 更新时间                            |
| is_deleted  | TINYINT      | 逻辑删除                            |

**唯一约束**：`UNIQUE KEY uk_provider_openid (provider, openid)`

### sys_menu（系统菜单表）

| 字段          | 类型           | 说明                     |
|-------------|--------------|------------------------|
| id          | BIGINT       | 自增主键                   |
| parent_id   | BIGINT       | 父菜单ID，0表示顶级            |
| name        | VARCHAR(100) | 菜单名称                   |
| url         | VARCHAR(500) | 菜单链接                   |
| icon        | VARCHAR(100) | 菜单图标                   |
| target      | VARCHAR(20)  | 打开方式: _self/_blank     |
| type        | TINYINT      | 菜单类型: 0-目录, 1-菜单, 2-按钮 |
| permission  | VARCHAR(200) | 权限标识（如 `user:list`）    |
| sort_order  | INT          | 排序序号                   |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏       |
| create_time | DATETIME     | 创建时间                   |
| update_time | DATETIME     | 更新时间                   |

### sys_role（角色表）

| 字段          | 类型           | 说明             |
|-------------|--------------|----------------|
| id          | BIGINT       | 自增主键           |
| role_name   | VARCHAR(100) | 角色名称           |
| role_key    | VARCHAR(100) | 角色标识 (UNIQUE)  |
| description | VARCHAR(500) | 角色描述           |
| status      | TINYINT      | 状态: 1-启用, 0-禁用 |
| create_time | DATETIME     | 创建时间           |
| update_time | DATETIME     | 更新时间           |

### sys_user_role（用户-角色关联表）

| 字段      | 类型          | 说明   |
|---------|-------------|------|
| id      | BIGINT      | 自增主键 |
| user_id | VARCHAR(36) | 用户ID |
| role_id | BIGINT      | 角色ID |

**唯一约束**：`UNIQUE KEY uk_user_role (user_id, role_id)`

### sys_role_menu（角色-菜单关联表）

| 字段      | 类型     | 说明   |
|---------|--------|------|
| id      | BIGINT | 自增主键 |
| role_id | BIGINT | 角色ID |
| menu_id | BIGINT | 菜单ID |

**唯一约束**：`UNIQUE KEY uk_role_menu (role_id, menu_id)`

### work_experience（工作经历表）

| 字段          | 类型           | 说明               |
|-------------|--------------|------------------|
| id          | BIGINT       | 自增主键             |
| title       | VARCHAR(500) | 职位标题             |
| job         | VARCHAR(200) | 岗位角色             |
| description | TEXT         | 整体描述             |
| sort_order  | INT          | 排序序号             |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏 |
| create_time | DATETIME     | 创建时间             |
| update_time | DATETIME     | 更新时间             |
| is_deleted  | TINYINT      | 逻辑删除             |

### project（项目表）

| 字段            | 类型           | 说明                    |
|---------------|--------------|-----------------------|
| id            | BIGINT       | 自增主键                  |
| experience_id | BIGINT       | 关联 work_experience.id |
| name          | VARCHAR(200) | 项目名称                  |
| lifecycle     | VARCHAR(100) | 项目周期                  |
| background    | TEXT         | 项目背景                  |
| duty          | TEXT         | 职责描述                  |
| sort_order    | INT          | 排序序号                  |
| create_time   | DATETIME     | 创建时间                  |
| update_time   | DATETIME     | 更新时间                  |
| is_deleted    | TINYINT      | 逻辑删除                  |

### project_achievement（项目成果表）

| 字段          | 类型       | 说明            |
|-------------|----------|---------------|
| id          | BIGINT   | 自增主键          |
| project_id  | BIGINT   | 关联 project.id |
| content     | TEXT     | 成果内容          |
| sort_order  | INT      | 排序序号          |
| create_time | DATETIME | 创建时间          |
| update_time | DATETIME | 更新时间          |
| is_deleted  | TINYINT  | 逻辑删除          |

### work_experience_stack（技术栈表）

| 字段            | 类型           | 说明                    |
|---------------|--------------|-----------------------|
| id            | BIGINT       | 自增主键                  |
| experience_id | BIGINT       | 关联 work_experience.id |
| tech_name     | VARCHAR(100) | 技术名称                  |
| sort_order    | INT          | 排序序号                  |
| create_time   | DATETIME     | 创建时间                  |
| update_time   | DATETIME     | 更新时间                  |
| is_deleted    | TINYINT      | 逻辑删除                  |

### ai_audit_log（AI 审计日志表）

| 字段               | 类型           | 说明                                                |
|------------------|--------------|---------------------------------------------------|
| id               | BIGINT       | 自增主键                                              |
| user_id          | VARCHAR(36)  | 用户 ID                                             |
| session_id       | VARCHAR(36)  | 会话 ID                                             |
| audit_type       | VARCHAR(50)  | 审计类型: INPUT_BLOCKED/OUTPUT_FILTERED/RISK_DETECTED |
| risk_level       | VARCHAR(20)  | 风险等级: HIGH/CRITICAL                               |
| action           | VARCHAR(50)  | 处置动作: BLOCKED/FILTERED/LOGGED                     |
| confidence       | DOUBLE       | 置信度 (0.0-1.0)                                     |
| keyword          | VARCHAR(200) | 触发关键词/模式                                          |
| original_content | TEXT         | 原始内容                                              |
| filtered_content | TEXT         | 过滤后内容                                             |
| description      | TEXT         | 风险描述                                              |
| create_time      | DATETIME     | 创建时间                                              |

### knowledge_base（知识库表）

| 字段          | 类型           | 说明                          |
|-------------|--------------|-----------------------------|
| id          | BIGINT       | 自增主键                        |
| name        | VARCHAR(200) | 知识库名称                       |
| description | TEXT         | 知识库描述                       |
| type        | VARCHAR(50)  | 知识库类型: general/professional |
| is_enabled  | TINYINT      | 是否启用: 1-启用, 0-禁用            |
| sort_order  | INT          | 排序序号                        |
| create_time | DATETIME     | 创建时间                        |
| update_time | DATETIME     | 更新时间                        |
| is_deleted  | TINYINT      | 逻辑删除                        |

### knowledge_document（知识文档表）

| 字段          | 类型           | 说明                                      |
|-------------|--------------|-----------------------------------------|
| id          | BIGINT       | 自增主键                                    |
| kb_id       | BIGINT       | 关联 knowledge_base.id                    |
| title       | VARCHAR(500) | 文档标题                                    |
| content     | LONGTEXT     | 文档内容                                    |
| file_name   | VARCHAR(200) | 原始文件名                                   |
| file_type   | VARCHAR(50)  | 文件类型: txt/md/pdf/docx                   |
| chunk_count | INT          | 分块数量                                    |
| status      | VARCHAR(20)  | 状态: pending/processing/completed/failed |
| error_msg   | TEXT         | 错误信息                                    |
| sort_order  | INT          | 排序序号                                    |
| create_time | DATETIME     | 创建时间                                    |
| update_time | DATETIME     | 更新时间                                    |
| is_deleted  | TINYINT      | 逻辑删除                                    |

### chat_memory（跨会话记忆表）

| 字段                | 类型           | 说明                             |
|-------------------|--------------|--------------------------------|
| id                | BIGINT       | 自增主键                           |
| user_id           | VARCHAR(36)  | 用户 ID                          |
| category          | VARCHAR(50)  | 记忆类别: preference/fact/context  |
| memory_key        | VARCHAR(200) | 记忆键（如 name、tech_stack、company） |
| memory_value      | TEXT         | 记忆值                            |
| source_session_id | VARCHAR(36)  | 来源会话 ID                        |
| access_count      | INT          | 访问次数（默认 0）                     |
| create_time       | DATETIME     | 创建时间                           |
| update_time       | DATETIME     | 更新时间                           |

### chat_reminder（定时提醒表）

| 字段            | 类型          | 说明                             |
|---------------|-------------|--------------------------------|
| id            | BIGINT      | 自增主键                           |
| user_id       | VARCHAR(36) | 用户 ID                          |
| reminder_text | TEXT        | 提醒内容                           |
| remind_time   | DATETIME    | 提醒时间                           |
| status        | TINYINT     | 状态: 0-待处理, 1-已完成, 2-已过期, 3-已取消 |
| create_time   | DATETIME    | 创建时间                           |
| update_time   | DATETIME    | 更新时间                           |

---

## 主键策略

所有表均使用 `@TableId(type = IdType.AUTO)` 自增主键，`user_id` / `session_id` 为业务唯一键（UNIQUE）。

| 表             | 主键                         | 业务键                             |
|---------------|----------------------------|---------------------------------|
| chat_user     | `id` BIGINT AUTO_INCREMENT | `user_id` VARCHAR(36) UNIQUE    |
| chat_session  | `id` BIGINT AUTO_INCREMENT | `session_id` VARCHAR(36) UNIQUE |
| chat_message  | `id` BIGINT AUTO_INCREMENT | -                               |
| user_oauth    | `id` BIGINT AUTO_INCREMENT | -                               |
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

| 版本 | 脚本文件                                 | 说明                                                   |
|----|--------------------------------------|------------------------------------------------------|
| V1 | `V1__init.sql`                       | 初始化基础表结构                                             |
| V2 | `V2__add_rbac_tables.sql`            | 添加 RBAC 权限管理表                                        |
| V3 | `V3__alter_link_desc_to_text.sql`    | knowledge_document.link_desc 从 VARCHAR(500) 扩展为 TEXT |
| V4 | `V4__create_chat_reminder_table.sql` | 创建 chat_reminder 定时提醒表                               |

---

**最后更新**: 2026-07-16
