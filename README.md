# Brief-Wisdom

> 基于 Spring Boot 3 + Spring AI 的 AI 智能对话平台，支持多会话管理、页面上下文感知、多模型管理、多端实时同步、第三方登录、简历管理等功能。

---

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [模块架构](#模块架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [数据库配置与初始化](#数据库配置与初始化)
- [数据库表结构](#数据库表结构)
- [MyBatis-Plus 配置](#mybatis-plus-配置)
- [逻辑删除](#逻辑删除)
- [自动填充](#自动填充)
- [AI 智能体](#ai-智能体)
- [安全与合规](#安全与合规)
- [会话管理](#会话管理)
- [API 接口](#api-接口)
- [用户认证](#用户认证)
- [前端页面](#前端页面)
- [常见问题排查](#常见问题排查)
- [编译与运行](#编译与运行)

---

## 项目简介

Brief-Wisdom 是一个 AI 智能对话平台，核心功能包括：

- **多会话管理**：支持创建、切换、删除多个会话，分页加载（无限滚动）
- **页面上下文感知**：AI 助手识别当前页面，提供针对性的对话能力（如简历页提供简历优化建议）
- **多模型管理**：支持 AI 模型的动态管理（启用/禁用/切换/价格配置），当前支持通义千问系列
- **多端实时同步**：基于 SSE 的实时数据同步，支持多设备同时在线
- **用户认证体系**：支持用户名/密码登录、微信扫码登录、钉钉扫码登录、支付宝扫码登录
- **访客系统**：未登录用户基于 IP + 浏览器 + 设备类型生成唯一指纹，保证会话连续性
- **个人简历管理**：完整的工作经历、项目经历、项目成果、技术栈的 CRUD 管理
- **系统管理**：用户管理（级别/状态）、菜单管理（动态配置）
- **AI 管理后台**：按用户级别查看会话历史、消息记录
- **安全合规**：输入关键词拦截、输出敏感信息过滤、接口限流保护

---

## 核心功能

### 1. AI 智能对话

- **多会话支持**：每个用户可创建多个独立会话，会话间上下文隔离
- **上下文记忆**：每个会话保留最近 10 条消息作为 AI 上下文
- **页面上下文感知**：AI 助手根据当前页面（首页/简历/设置等）自动切换角色定位
- **多模型支持**：用户可在聊天界面切换 AI 模型（通义千问 Max/Plus/Turbo 等）
- **Token 统计**：自动记录每次对话的 Token 用量和费用
- **欢迎界面**：每次打开 AI 助手显示欢迎界面，介绍能力和使用说明

### 2. 多端实时同步

- **SSE 推送**：基于 Server-Sent Events 实现实时数据同步
- **事件类型**：会话创建、会话删除、消息添加等事件实时推送到所有在线设备
- **连接管理**：关闭聊天窗口时主动断开 SSE 连接，清理服务端资源
- **同步状态检测**：提供轻量级同步状态接口，前端轮询检测数据变更

### 3. 安全与合规

- **三层防线**：
  1. **系统提示词**：引导 AI 遵守伦理合规和安全风控准则
  2. **输入预过滤**：关键词拦截（炸弹、毒品、枪支等），命中即拒绝，不消耗 Token
  3. **输出过滤**：正则检测 AI 回复中的敏感信息（身份证、手机号、银行卡号），自动替换
- **接口限流**：滑动窗口算法，每用户每分钟 20 次、每天 200 次
- **输入校验**：空值检测、长度限制（最大 10000 字符）

### 4. 用户认证

- **用户名/密码登录**：支持注册、登录，密码 BCrypt 加密
- **微信扫码登录**：OAuth2.0 标准流程
- **钉钉扫码登录**：OAuth2.0 标准流程
- **支付宝扫码登录**：OAuth2.0 标准流程
- **访客模式**：未登录用户基于客户端指纹生成唯一 ID，保证会话连续性
- **用户级别**：admin（管理员）、vip（会员）、normal（普通用户）

### 5. 简历管理

- **工作经历**：支持 CRUD，包含职位、岗位、描述、排序、显示控制
- **项目经历**：关联工作经历，包含项目名称、周期、背景、职责
- **项目成果**：关联项目，记录具体成果内容
- **技术栈**：关联工作经历，记录使用的技术

### 6. 系统管理

- **用户管理**：分页查询、级别修改、删除用户、重置密码
- **菜单管理**：动态菜单配置（名称、链接、图标、排序、显示控制）
- **AI 模型管理**：模型 CRUD、启用/禁用、激活切换、价格配置

---

## 模块架构

```
Brief-Wisdom/
├── brief-wisdom-persistence/   # 数据持久化模块（Entity + Mapper + Repository）
├── brief-wisdom-ai/            # AI 智能模块（Service + 业务逻辑）
├── brief-wisdom-web/           # Web 应用模块（Controller + 前端资源）
├── brief-wisdom-api/           # API 接口定义模块
├── brief-wisdom-service/       # 通用服务模块
├── brief-wisdom-resume/        # 个人简历模块
└── brief-wisdom-common/        # 公共模块（DTO + 工具类）
```

**依赖关系**：

```
brief-wisdom-web (Web入口)
    ├── brief-wisdom-ai (AI功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-resume (简历功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-api (API定义)
    ├── brief-wisdom-service (通用服务)
    └── brief-wisdom-common (公共DTO)
```

### 各模块职责

| 模块          | 职责            | 关键类                                                                             |
|-------------|---------------|---------------------------------------------------------------------------------|
| persistence | 数据存储和访问层      | `ChatUser`, `ChatSession`, `ChatMessage`, `AiModel`, `WorkExperience`, `Project`, 各 Mapper/Repository |
| ai          | AI 服务和业务逻辑    | `AiAgentService`, `ChatSyncService`, `ContentFilterService`, `RateLimitService`, `SystemPrompts` |
| web         | Web 入口和前端资源   | `WebApplication`, `AiAgentController`, `AuthController`, `UserController`, `MenuController`, `SecurityConfig` |
| resume      | 个人简历展示和管理      | `ResumeController`, `ResumeManageController`, `ResumeService`, `ResumeManageService` |
| common      | 公共 DTO 和工具    | `ApiResponse`, `PageResult`, `SessionMetaDTO`, `ChatMessageDTO`, `AiModelDTO` 等 |
| api         | API 接口定义和 DTO | 通用响应封装                                                                          |
| service     | 通用服务层         | 业务服务类                                                                           |

---

## 技术栈

| 组件          | 版本/说明              |
|-------------|--------------------|
| Java        | 17                 |
| Spring Boot | 3.5.7              |
| Spring AI   | 1.0.0 (Spring AI Alibaba) |
| ORM         | MyBatis-Plus 3.5.5 |
| 数据库         | MySQL 8.0+         |
| 安全          | Spring Security 6  |
| 前端          | 原生 HTML/CSS/JS     |
| 构建工具        | Maven              |

---

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Maven（或使用项目自带 `mvnw`）

### 2. 初始化数据库

```bash
# 初始化基础表结构
mysql -u root -p < brief-wisdom-web/src/main/resources/init.sql

# 初始化 AI 模型配置
mysql -u root -p < brief-wisdom-web/src/main/resources/ai_model_init.sql

# 初始化简历数据
mysql -u root -p < brief-wisdom-web/src/main/resources/resume_init.sql

# 初始化菜单数据
mysql -u root -p < brief-wisdom-web/src/main/resources/menu_init.sql
```

### 3. 配置 AI 提供商

编辑 `brief-wisdom-web/src/main/resources/application-ai.yml`，配置 AI 模型参数。

### 4. 启动应用

```bash
./mvnw spring-boot:run -pl brief-wisdom-web
```

访问：http://localhost:8090

> 支持 DevTools 热部署，修改 Java 代码后自动重启。

---

## 数据库配置与初始化

### 连接信息

- **主机**：localhost:3306
- **数据库名**：brief_wisdom
- **用户名**：root
- **密码**：123456
- **字符集**：utf8mb4（支持 emoji）

### 初始化方式

**方式一：手动执行初始化脚本（推荐）**

```bash
mysql -u root -p123456 < brief-wisdom-web/src/main/resources/init.sql
```

**方式二：应用启动时自动建表**

MyBatis-Plus 启动后会自动创建表结构（需提前创建数据库）。

### 初始化数据

初始化脚本会自动：

1. 创建数据库 `brief_wisdom`（如果不存在）
2. 创建所有必要的表
3. 插入默认用户：`default-user/guest`（访客）和 `admin/mouhin`

### 数据库备份与恢复

```bash
# 备份
mysqldump -u root -p brief_wisdom > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p brief_wisdom < backup_20260101.sql
```

---

## 数据库表结构

### 数据关系

```
chat_user (用户)
    ├── chat_session (会话) [一对多]
    │       └── chat_message (消息) [一对多]
    ├── user_oauth (第三方登录绑定) [一对多]
    └── ai_model (AI模型配置) [独立]

work_experience (工作经历)
    ├── project (项目) [一对多]
    │       └── project_achievement (项目成果) [一对多]
    └── work_experience_stack (技术栈) [一对多]

sys_menu (系统菜单) [独立]
```

### chat_user（用户表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| user_id     | VARCHAR(36)  | 用户ID (UUID, UNIQUE) |
| username    | VARCHAR(100) | 用户名 (UNIQUE)        |
| nickname    | VARCHAR(200) | 昵称                  |
| avatar      | VARCHAR(500) | 头像URL               |
| password    | VARCHAR(200) | BCrypt加密后的密码        |
| user_level  | VARCHAR(20)  | 用户级别: admin/vip/normal |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除: 0-未删除, 1-已删除  |

### chat_session（会话表）

| 字段            | 类型           | 说明                  |
|---------------|--------------|---------------------|
| id            | BIGINT       | 自增主键                |
| session_id    | VARCHAR(36)  | 会话ID (UUID, UNIQUE) |
| user_id       | VARCHAR(36)  | 用户ID (外键)           |
| title         | VARCHAR(200) | 会话标题                |
| description   | TEXT         | 会话描述                |
| page_context  | VARCHAR(200) | 页面上下文（如 /about.html） |
| message_count | INT          | 消息数量                |
| create_time   | DATETIME     | 创建时间                |
| update_time   | DATETIME     | 更新时间                |
| is_deleted    | TINYINT      | 逻辑删除                |

### chat_message（消息表）

| 字段           | 类型           | 说明                     |
|--------------|--------------|------------------------|
| id           | BIGINT       | 自增主键                   |
| session_id   | VARCHAR(36)  | 会话ID (外键)              |
| user_id      | VARCHAR(36)  | 用户ID (外键)              |
| role         | VARCHAR(20)  | 角色 (user/assistant)    |
| content      | LONGTEXT     | 消息内容                   |
| model        | VARCHAR(500) | AI模型名称                 |
| tokens       | INT          | Token数量                |
| cost         | DOUBLE       | 费用                     |
| timestamp    | DATETIME     | 消息时间                   |
| message_type | VARCHAR(50)  | 消息类型 (text/image/code) |
| is_deleted   | TINYINT      | 逻辑删除                   |

### ai_model（AI模型配置表）

| 字段                    | 类型           | 说明                        |
|-----------------------|--------------|---------------------------|
| id                    | BIGINT       | 自增主键                      |
| model_name            | VARCHAR(100) | 模型标识(如 qwen-max)          |
| display_name          | VARCHAR(200) | 显示名称                      |
| provider              | VARCHAR(50)  | 服务商(dashscope/openai等)     |
| description           | VARCHAR(500) | 模型描述                      |
| is_active             | TINYINT      | 是否激活(同时只激活一个)            |
| is_enabled            | TINYINT      | 是否启用                      |
| sort_order            | INT          | 排序序号                      |
| input_price_per_million  | DOUBLE    | 每百万输入token价格(元)          |
| output_price_per_million | DOUBLE    | 每百万输出token价格(元)          |
| create_time           | DATETIME     | 创建时间                      |
| update_time           | DATETIME     | 更新时间                      |
| is_deleted            | TINYINT      | 逻辑删除                      |

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

### work_experience（工作经历表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| title       | VARCHAR(500) | 职位标题                |
| job         | VARCHAR(200) | 岗位角色                |
| description | TEXT         | 整体描述                |
| sort_order  | INT          | 排序序号                |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏   |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除                |

### project（项目表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| experience_id | BIGINT     | 关联 work_experience.id |
| name        | VARCHAR(200) | 项目名称                |
| lifecycle   | VARCHAR(100) | 项目周期                |
| background  | TEXT         | 项目背景                |
| duty        | TEXT         | 职责描述                |
| sort_order  | INT          | 排序序号                |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除                |

### project_achievement（项目成果表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| project_id  | BIGINT       | 关联 project.id       |
| content     | TEXT         | 成果内容                |
| sort_order  | INT          | 排序序号                |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除                |

### work_experience_stack（技术栈表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| experience_id | BIGINT     | 关联 work_experience.id |
| tech_name   | VARCHAR(100) | 技术名称                |
| sort_order  | INT          | 排序序号                |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除                |

### sys_menu（系统菜单表）

| 字段          | 类型           | 说明                  |
|-------------|--------------|---------------------|
| id          | BIGINT       | 自增主键                |
| name        | VARCHAR(100) | 菜单名称                |
| url         | VARCHAR(500) | 菜单链接                |
| icon        | VARCHAR(100) | 菜单图标                |
| target      | VARCHAR(20)  | 打开方式: _self/_blank   |
| sort_order  | INT          | 排序序号                |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏   |
| create_time | DATETIME     | 创建时间                |
| update_time | DATETIME     | 更新时间                |
| is_deleted  | TINYINT      | 逻辑删除                |

### 外键约束

- `chat_session.user_id` → `chat_user.user_id` (CASCADE DELETE)
- `chat_message.session_id` → `chat_session.session_id` (CASCADE DELETE)
- `chat_message.user_id` → `chat_user.user_id` (CASCADE DELETE)
- `user_oauth.user_id` → `chat_user.user_id` (CASCADE DELETE)

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

### 主键策略

所有表均使用 `@TableId(type = IdType.AUTO)` 自增主键，`user_id` / `session_id` 为业务唯一键（UNIQUE）。

| 表            | 主键                         | 业务键                             |
|--------------|----------------------------|---------------------------------|
| chat_user    | `id` BIGINT AUTO_INCREMENT | `user_id` VARCHAR(36) UNIQUE    |
| chat_session | `id` BIGINT AUTO_INCREMENT | `session_id` VARCHAR(36) UNIQUE |
| chat_message | `id` BIGINT AUTO_INCREMENT | -                               |
| user_oauth   | `id` BIGINT AUTO_INCREMENT | -                               |
| ai_model     | `id` BIGINT AUTO_INCREMENT | `model_name` VARCHAR(100)       |

> 按业务键查询时需使用 `LambdaQueryWrapper`，而非 `selectById()`。

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

## AI 智能体

### 当前 AI 提供商

项目当前使用 **阿里通义千问（DashScope）**，通过 OpenAI 兼容协议对接，配置文件 `application-ai.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-max
```

### 多模型管理

系统支持动态管理多个 AI 模型：

- **模型列表**：qwen-max、qwen-plus、qwen-turbo、qwen3.7-plus 等
- **激活切换**：同时只激活一个模型作为默认模型
- **启用/禁用**：可禁用某些模型，禁用后不在聊天界面显示
- **价格配置**：配置每个模型的输入/输出价格（元/百万Token），自动计算费用

### 页面上下文感知

AI 助手根据当前页面自动切换角色定位：

| 页面路径 | 页面名称 | AI 角色定位 |
|---------|---------|------------|
| `/` | 首页 | 综合性 AI 助手，知识问答、文案撰写、编程辅助等 |
| `/about.html` | 个人简历 | 了解简历内容，协助优化描述、提炼亮点、面试建议 |
| `/resume-manage.html` | 简历数据维护 | 协助整理工作经历、优化项目成果、技术栈分类建议 |
| `/system-settings.html` | 系统设置 | 协助了解系统配置、AI 模型管理等 |
| `/ai-manage.html` | AI 助手管理 | 协助了解模型特性、提供模型选择建议 |

### 系统提示词

所有对话均附加系统提示词，包含：

- **伦理与法律合规**：不生成违法违规内容、不泄露隐私、不歧视侮辱
- **安全与风险控制**：拒绝越狱指令、拒绝 Prompt 注入、不编造事实
- **回复规范**：使用中文、专业友善、简洁明了

---

## 安全与合规

### 三层防线

#### 1. 系统提示词（软控制）

通过 `SystemPrompts.BASE_SYSTEM_PROMPT` 引导 AI 遵守伦理合规准则。

#### 2. 输入预过滤（硬拦截）

通过 `ContentFilterService` 检测输入关键词，命中即拒绝，不消耗 Token：

- **违法类**：制作炸弹、制造毒品、购买枪支、黑客攻击教程等
- **有害类**：自杀方法、自残教程、如何杀人、下毒方法等

#### 3. 输出过滤（兜底）

通过正则检测 AI 回复中的敏感信息，自动替换为 `[敏感信息已过滤]`：

- **身份证号**：18位数字（最后一位可为X）
- **手机号**：11位数字（1开头）
- **银行卡号**：16-19位数字

### 接口限流

通过 `RateLimitService` 实现滑动窗口限流，防止恶意刷接口：

- **分钟级限制**：每用户每分钟最多 20 次请求
- **天级限制**：每用户每天最多 200 次请求
- **内存级实现**：重启后计数清零，分布式场景建议接入 Redis

### 输入校验

- **空值检测**：消息内容不能为空
- **长度限制**：单条消息最大 10000 字符

---

## 会话管理

### 会话生命周期

```
用户打开页面
    ↓
加载分页配置（/api/ai/config/pagination）
    ↓
建立 SSE 连接（/api/ai/sync/events）
    ↓
显示欢迎界面（介绍 AI 能力和使用说明）
    ↓
用户点击会话 / 创建新会话（传递当前页面上下文）
    ↓
发送消息 → 保存到 chat_message
    ↓
调用 AI 获取回复 → 保存到 chat_message（含 Token 统计和费用计算）
    ↓
更新会话统计（message_count, update_time, title）
    ↓
SSE 通知其他设备同步
    ↓
重新加载会话列表（更新标题和时间排序）
```

### 上下文记忆

每个会话保留最近 **10 条消息** 作为 AI 上下文，保证对话连贯性。

### 会话列表分页（无限滚动）

- 会话列表支持分页加载，前端通过滚动触底自动加载下一页
- 分页参数由后端 `PaginationProperties` 配置，前端动态获取
- 会话按 `update_time` 倒序排列（即最后一条消息时间）
- 切换会话时清除旧聊天内容，加载新会话历史
- 删除会话时自动切换到第一个会话

### 多端实时同步

- **SSE 连接**：前端打开聊天窗口时建立，关闭时主动断开
- **事件推送**：会话创建/删除、消息添加等事件实时推送到所有在线设备
- **同步状态**：提供轻量级接口检测数据变更，前端轮询对比指纹

### 前端关键函数

| 函数                              | 说明               |
|---------------------------------|------------------|
| `loadPaginationConfig()`        | 加载分页配置           |
| `loadSessions()`                | 加载会话列表（第一页）      |
| `loadMoreSessions()`            | 加载更多会话（下一页，无限滚动） |
| `createNewSession()`            | 创建新会话（传递页面上下文）    |
| `selectSession(sessionId)`      | 切换会话             |
| `sendMessage()`                 | 发送消息             |
| `loadSessionHistory(sessionId)` | 加载历史消息           |
| `connectSyncSSE()`              | 建立 SSE 连接        |
| `disconnectSyncSSE()`           | 断开 SSE 连接        |

---

## API 接口

### AI 会话管理

| 接口                                    | 方法     | 说明                                     |
|---------------------------------------|--------|----------------------------------------|
| `/api/ai/session`                     | POST   | 创建新会话（支持传入 pageContext）                |
| `/api/ai/session/{sessionId}`         | DELETE | 删除会话（逻辑删除）                             |
| `/api/ai/sessions?page=1&size=20`     | GET    | 分页获取会话列表（返回 `records/total/hasMore` 等） |
| `/api/ai/session/{sessionId}/history` | GET    | 分页获取会话历史消息（第1页为最新消息）                  |
| `/api/ai/config/pagination`           | GET    | 获取分页配置（sessionList/messageHistory）     |

### AI 聊天

| 接口                                 | 方法   | 说明        |
|------------------------------------|------|-----------|
| `/api/ai/chat`                     | POST | 简单聊天（无上下文） |
| `/api/ai/chat-with-prompt`         | POST | 带系统提示的聊天  |
| `/api/ai/ask`                      | POST | 智能问答      |
| `/api/ai/chat/session/{sessionId}` | POST | 带上下文的会话聊天（支持指定模型） |

### AI 多端同步

| 接口                    | 方法     | 说明                |
|-----------------------|--------|-------------------|
| `/api/ai/sync/events` | GET    | SSE 实时同步事件流       |
| `/api/ai/sync/events` | DELETE | 断开 SSE 连接         |
| `/api/ai/sync/status` | GET    | 获取同步状态（指纹对比）     |

### AI 模型管理

| 接口                          | 方法     | 说明           |
|-----------------------------|--------|--------------|
| `/api/ai/models`            | GET    | 获取所有模型列表     |
| `/api/ai/models/enabled`    | GET    | 获取启用的模型列表    |
| `/api/ai/models/active`     | GET    | 获取当前激活的模型    |
| `/api/ai/models/activate/{id}` | PUT | 切换激活模型        |
| `/api/ai/models`            | POST   | 新增模型          |
| `/api/ai/models`            | PUT    | 更新模型          |
| `/api/ai/models/{id}`       | DELETE | 删除模型          |
| `/api/ai/models/{id}/toggle` | PUT   | 启用/禁用模型       |

### AI 管理后台

| 接口                                     | 方法  | 说明              |
|----------------------------------------|-----|-----------------|
| `/api/ai/manage/users`                 | GET | 获取用户列表（支持按级别筛选） |
| `/api/ai/manage/levels`                | GET | 获取所有用户级别        |
| `/api/ai/manage/sessions/user/{userId}` | GET | 按用户ID查询会话列表     |
| `/api/ai/manage/sessions/level/{level}` | GET | 按用户级别查询会话列表     |
| `/api/ai/manage/session/{sessionId}/messages` | GET | 获取会话消息历史      |

### 认证

| 接口                      | 方法   | 说明            |
|-------------------------|------|---------------|
| `/api/auth/register`    | POST | 用户注册          |
| `/api/auth/login`       | POST | 用户名/密码登录      |
| `/auth/wechat/login`    | GET  | 获取微信扫码登录 URL  |
| `/auth/wechat/callback` | GET  | 微信授权回调        |
| `/auth/dingtalk/login`  | GET  | 获取钉钉扫码登录 URL  |
| `/auth/dingtalk/callback` | GET | 钉钉授权回调       |
| `/auth/alipay/login`    | GET  | 获取支付宝扫码登录 URL |
| `/auth/alipay/callback` | GET  | 支付宝授权回调       |
| `/api/auth/status`      | GET  | 检查登录状态        |
| `/api/auth/user`        | GET  | 获取当前用户信息（需登录） |
| `/auth/logout`          | POST | 退出登录          |

### 用户管理

| 接口                          | 方法     | 说明        |
|-----------------------------|--------|-----------|
| `/api/user/list`            | GET    | 分页获取用户列表  |
| `/api/user/levels`          | GET    | 获取用户级别选项  |
| `/api/user/{id}/level`      | PUT    | 修改用户级别    |
| `/api/user/{id}`            | DELETE | 删除用户      |
| `/api/user/{id}/reset-password` | PUT | 重置用户密码   |

### 菜单管理

| 接口                  | 方法     | 说明           |
|---------------------|--------|--------------|
| `/api/menu/list`    | GET    | 获取可见菜单列表     |
| `/api/menu/all`     | GET    | 获取全部菜单（含隐藏） |
| `/api/menu`         | POST   | 新增菜单         |
| `/api/menu`         | PUT    | 更新菜单         |
| `/api/menu/{id}`    | DELETE | 删除菜单         |
| `/api/menu/{id}/toggle` | PUT | 切换菜单显示/隐藏   |

### 简历数据

| 接口                              | 方法     | 说明        |
|---------------------------------|--------|-----------|
| `/api/resume/experiences`       | GET    | 获取所有工作经历  |
| `/api/resume/manage/experiences` | GET    | 获取工作经历列表  |
| `/api/resume/manage/experiences` | POST   | 创建工作经历    |
| `/api/resume/manage/experiences/{id}` | PUT | 更新工作经历  |
| `/api/resume/manage/experiences/{id}` | DELETE | 删除工作经历 |
| `/api/resume/manage/projects`   | GET    | 获取项目列表    |
| `/api/resume/manage/projects`   | POST   | 创建项目      |
| `/api/resume/manage/projects/{id}` | PUT | 更新项目     |
| `/api/resume/manage/projects/{id}` | DELETE | 删除项目   |
| `/api/resume/manage/achievements` | GET  | 获取项目成果列表  |
| `/api/resume/manage/achievements` | POST | 创建项目成果    |
| `/api/resume/manage/achievements/{id}` | PUT | 更新项目成果 |
| `/api/resume/manage/achievements/{id}` | DELETE | 删除项目成果 |
| `/api/resume/manage/stacks`     | GET    | 获取技术栈列表   |
| `/api/resume/manage/stacks`     | POST   | 创建技术栈     |
| `/api/resume/manage/stacks/{id}` | PUT   | 更新技术栈     |
| `/api/resume/manage/stacks/{id}` | DELETE | 删除技术栈     |

---

## 用户认证

### 认证方式

系统支持多种认证方式：

| 认证方式 | 说明 |
|--------|------|
| 用户名/密码 | 标准注册登录，密码 BCrypt 加密 |
| 微信扫码 | OAuth2.0 标准流程 |
| 钉钉扫码 | OAuth2.0 标准流程 |
| 支付宝扫码 | OAuth2.0 标准流程 |
| 访客模式 | 基于客户端指纹生成唯一 ID |

### 访客指纹生成

未登录用户基于以下因子生成唯一访客 ID：

- **客户端 IP**：支持反向代理场景（X-Forwarded-For、X-Real-IP）
- **浏览器类型**：Chrome、Firefox、Safari、Edge、Opera 等（不含版本号）
- **设备类型**：PC、Mobile、Tablet

指纹算法：`SHA256(IP + browserType + deviceType)` 前 16 位，格式为 `guest-{fingerprint}`。

同一设备（同一 IP + 同一浏览器 + 同一设备类型）始终生成相同的访客 ID，保证会话连续性。

### 安全配置

`SecurityConfig` 配置了 Spring Security 路由权限：

- 静态资源、AI API、认证 API、简历 API、菜单 API 全部公开访问
- `/api/auth/user` 需要登录才能获取用户信息
- 禁用 CSRF、formLogin、httpBasic
- 同一用户最多 1 个并发 Session，新登录踢掉旧会话
- 未认证时返回 `401 JSON` 响应

### 用户级别

| 级别 | 说明 |
|------|------|
| admin | 管理员，可访问管理后台 |
| vip | 会员，享受更多权限 |
| normal | 普通用户，基础权限 |

---

## 前端页面

| 页面   | 路径                   | 说明                         |
|------|----------------------|----------------------------|
| 主页   | `/` / `index.html`   | AI 聊天助手入口，右下角悬浮按钮打开聊天窗口 |
| 个人简历 | `/about.html`        | 个人简历展示页，支持深色/浅色主题切换、PDF 导出 |
| 简历数据维护 | `/resume-manage.html` | 工作经历、项目、成果、技术栈的 CRUD 管理 |
| 系统设置 | `/system-settings.html` | 用户管理、菜单管理等系统配置 |
| AI助手管理 | `/ai-manage.html` | AI 模型管理、用户会话查看 |
| 测试页  | `/test-session.html` | 会话 API 独立测试页面 |

---

## 常见问题排查

### 问题 1：sessionId 为 null

**症状**：后端日志显示 `sessionId: null`，消息发送失败

**排查步骤**：

1. **清除浏览器缓存**：硬刷新（Ctrl+Shift+R）或使用无痕模式
2. **检查前端 Console**：确认 `currentSessionId` 有值
3. **检查 Network 标签**：确认请求 URL 包含正确的 sessionId
4. **测试后端 API**：
   ```bash
   # 创建会话
   curl -X POST http://localhost:8090/api/ai/session
   # 应返回: {"success":true,"data":"uuid-here"}
   ```

### 问题 2：create_time cannot be null

**原因**：`MetaObjectHandler` 未正确配置

**解决**：确保 `MybatisPlusConfig` 实现了 `MetaObjectHandler` 接口，并在 `insertFill()` 方法中填充时间字段。

### 问题 3：会话删除后无法使用

**原因**：会话被逻辑删除（`is_deleted = 1`）

**解决**：

```sql
-- 查看会话状态
SELECT * FROM chat_session WHERE session_id = 'xxx';

-- 恢复会话
UPDATE chat_session SET is_deleted = 0 WHERE session_id = 'xxx';
```

### 问题 4：主键改造后查询失败

**原因**：`chat_user`/`chat_session` 改为 `id` 自增主键后，`selectById()` 查询的是 `id` 字段而非业务键

**解决**：按业务键查询需改用 `LambdaQueryWrapper`：

```java
LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
qw.eq(ChatSession::getSessionId, sessionId);
ChatSession session = sessionMapper.selectOne(qw);
```

### 问题 5：SSE 连接累积

**症状**：连续点击 AI 图标导致连接数增加

**原因**：前端 `EventSource.close()` 后服务端不会立即感知断开

**解决**：关闭聊天窗口时主动调用 `DELETE /api/ai/sync/events` 断开连接，清理服务端资源。

---

## 编译与运行

```bash
# 编译整个项目
./mvnw clean compile

# 运行应用
./mvnw spring-boot:run -pl brief-wisdom-web

# 打包
./mvnw clean package
```

### 开发模式

- **热部署**：已启用 DevTools 热重启，修改 Java 代码后自动重启
- **LiveReload**：已启用 LiveReload（端口 35729），前端修改后浏览器自动刷新
- **SQL 日志**：控制台输出完整 SQL 日志，便于调试

### 验证清单

- [ ] 数据库 `brief_wisdom` 已创建，表结构完整
- [ ] 默认用户（`default-user/guest` 和 `admin/mouhin`）已插入
- [ ] AI 模型配置已初始化（`ai_model_init.sql`）
- [ ] 应用启动无报错
- [ ] 访问 http://localhost:8090 页面正常显示
- [ ] 创建会话、发送消息、切换会话功能正常
- [ ] 会话列表无限滚动加载正常
- [ ] AI 回复正常返回（Markdown 渲染）
- [ ] SSE 实时同步正常（多设备测试）
- [ ] 访问 http://localhost:8090/about.html 个人简历页面正常

---

## 扩展方向

| 方向      | 说明             |
|---------|----------------|
| 会话标签/分类 | 为会话添加标签，便于管理   |
| 消息搜索    | 全文搜索历史消息       |
| 多端同步增强  | WebSocket 替代 SSE，支持双向通信 |
| AI 模型扩展 | 支持 OpenAI、Claude 等更多提供商 |
| 费用统计    | Token 用量和费用报表  |
| 知识库管理  | 上传文档构建知识库，增强 AI 回答 |
| 插件系统    | 支持 AI 调用外部工具（如搜索引擎、API 调用） |

