# Brief-Wisdom

> 基于 Spring Boot 3 + Spring AI 的 AI 智能对话平台，支持 RBAC 权限控制、多会话管理、页面上下文感知、多模型管理、知识库管理、多端实时同步、第三方登录、Redis 分布式缓存、简历管理等功能。

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
- [RBAC 权限体系](#rbac-权限体系)
- [Redis 缓存架构](#redis-缓存架构)
- [AI 智能体](#ai-智能体)
- [知识库管理](#知识库管理)
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

- **RBAC 权限控制**：基于角色的访问控制，支持超级管理员/管理员/普通用户三级权限体系，细粒度到按钮级别的权限管理
- **多会话管理**：支持创建、切换、删除多个会话，分页加载（无限滚动）
- **页面上下文感知**：AI 助手识别当前页面，提供针对性的对话能力（如简历页提供简历优化建议）
- **多模型管理**：支持 AI 模型的动态管理（启用/禁用/切换/价格配置），已集成通义千问、DeepSeek 等多提供商，支持 OpenAI 兼容协议和 Anthropic 协议
- **知识库管理**：支持文档上传、向量化存储、语义检索，增强 AI 回答准确性，支持内部文档/文件/外部链接三种类型
- **多端实时同步**：支持 SSE 和 WebSocket 两种传输方式，可配置切换，实现多设备同时在线实时同步
- **流式输出**：支持 SSE 流式聊天（打字机效果），可配置开启或关闭，提升用户体验
- **局部更新优化**：前端采用纯内存缓存 + 零网络请求的局部更新策略，消除页面抖动，实现极致流畅的交互体验
- **分布式锁修复**：解决 Redisson Spring Boot Starter 与 Spring Data Redis 的 `pExpire()` 方法递归冲突，确保 Spring Session 稳定性
- **用户认证体系**：支持用户名/密码登录、微信扫码登录、钉钉扫码登录、支付宝扫码登录
- **访客系统**：未登录用户基于 IP + 浏览器 + 设备类型生成唯一指纹，保证会话连续性
- **个人简历管理**：完整的工作经历、项目经历、项目成果、技术栈的 CRUD 管理（组件化架构），支持在线编辑和 AI 文案润色
- **AI 文案润色**：在线编辑器中集成 AI 润色功能，可对工作描述、项目背景、个人职责等字段进行智能优化，支持用户开关控制
- **费用统计**：多维度 AI 使用费用统计，支持按模型、用户、时间段分析，提供可视化图表和汇总报表
- **AI 审计日志**：记录 AI 交互审计日志，支持日志查询和统计分析
- **国际化（i18n）**：前后端完整的多语言框架，支持中文/英文切换，可扩展更多语言
- **系统管理**：用户管理（级别/状态）、角色管理、菜单管理（树形结构 + 动态配置）
- **AI 管理后台**：按用户级别查看会话历史、消息记录
- **安全合规**：输入关键词拦截、输出敏感信息过滤、接口限流保护

---

## 核心功能

### 1. AI 智能对话

- **多会话支持**：每个用户可创建多个独立会话，会话间上下文隔离
- **上下文记忆**：每个会话保留最近 10 条消息作为 AI 上下文
- **会话标题重命名**：用户可自定义修改会话标题，方便管理和查找
- **页面上下文感知**：AI 助手根据当前页面（首页/简历/设置等）自动切换角色定位
- **多模型支持**：用户可在聊天界面切换 AI 模型（通义千问 Max/Plus/Turbo/Qwen3.7-Plus 等）
- **Token 统计**：自动记录每次对话的 Token 用量和费用
- **欢迎界面**：每次打开 AI 助手显示欢迎界面，介绍能力和使用说明

### 2. RBAC 权限控制

- **三级角色**：超级管理员（super_admin）、管理员（admin）、普通用户（normal）
- **树形菜单**：支持目录 → 菜单 → 按钮三级菜单结构
- **细粒度权限**：基于 `permission` 标识的 API 级权限控制，通过 `@RequiresPermission` 注解实现
- **动态菜单**：根据用户角色动态加载可见菜单，前端按权限渲染
- **超级管理员特权**：super_admin 角色拥有所有权限，自动放行

### 3. 流式输出与实时同步

#### 流式聊天（SSE）

系统支持两种聊天模式，通过配置项 `app.chat.streaming` 切换：

- **流式模式**（默认）：
  - 服务端使用 `SseEmitter` 逐块推送 AI 回复
  - 前端使用 `EventSource` API 接收并实时渲染
  - 提供打字机效果，提升用户等待体验
  - 支持完成事件通知，确保消息完整性

- **普通模式**：
  - 阻塞式等待完整响应后一次性返回
  - 适用于对实时性要求不高的场景

#### 多端实时同步

系统提供两种实时同步方案，通过配置项 `app.sync.transport` 切换（默认 `websocket`）：

#### SSE (Server-Sent Events)

- **单向推送**：服务端向客户端推送事件通知
- **连接管理**：前端打开聊天窗口时建立 SSE 连接，关闭时主动断开
- **事件类型**：会话创建/删除、消息添加等事件实时推送到所有在线设备
- **心跳保活**：定时发送 ping 消息保持连接活跃
- **断线重连**：非主动关闭时自动尝试重新连接

#### WebSocket

- **双向通信**：支持客户端和服务端双向发送消息
- **连接管理**：通过 `WebSocketSyncHandler` 处理连接生命周期
- **身份认证**：握手阶段通过 `WebSocketSyncHandshakeInterceptor` 提取用户身份
- **多端支持**：同一用户可在多个设备同时建立连接
- **广播机制**：支持向指定用户或所有用户广播事件

#### 同步事件

- `session_created`：会话创建事件
- `session_deleted`：会话删除事件
- `message_added`：新消息添加事件

### 4. 前端性能优化

#### 局部更新策略

为消除页面刷新抖动，系统实现了纯内存缓存的局部更新机制：

- **零网络请求**：从内存数组 `allSessions` 直接读取数据，无需重新加载整个列表
- **精准 DOM 更新**：只更新单个文本节点（如会话时间），不重建任何组件
- **零视觉抖动**：响应延迟从 200-500ms 降至 <1ms，用户完全感知不到"系统在工作"

#### 核心优化函数

| 函数名 | 功能 | 优化前 | 优化后 |
|--------|------|--------|--------|
| `updateCurrentSessionItem()` | 更新当前会话项 | 发起网络请求获取最新数据 | 纯内存操作，零请求 |
| `addNewSessionToList()` | 添加新会话到列表 | 调用 `loadSessions()` 重新加载 | 局部插入 DOM 元素 |
| `removeSessionFromList()` | 删除会话项 | 调用 `loadSessions()` 重新加载 | 局部移除 DOM 元素 |

### 5. Redis 分布式缓存

- **Spring Cache**：基于注解的缓存管理，不同缓存域设置独立 TTL
- **业务域缓存**：菜单树（10min）、用户权限（5min）、简历数据（30min）、AI 模型（15min）
- **Session 持久化**：Spring Session + Redis，支持分布式会话共享
- **分布式锁**：基于 Redisson 的 `@DistributedLock` 注解，防止并发冲突

### 6. 安全与合规

- **三层防线**：
    1. **系统提示词**：引导 AI 遵守伦理合规和安全风控准则
    2. **输入预过滤**：关键词拦截（炸弹、毒品、枪支等），命中即拒绝，不消耗 Token
    3. **输出过滤**：正则检测 AI 回复中的敏感信息（身份证、手机号、银行卡号），自动替换
- **接口限流**：滑动窗口算法，每用户每分钟 20 次、每天 200 次
- **输入校验**：空值检测、长度限制（最大 10000 字符）

### 7. 用户认证

- **用户名/密码登录**：支持注册、登录，密码 BCrypt 加密
- **微信扫码登录**：OAuth2.0 标准流程
- **钉钉扫码登录**：OAuth2.0 标准流程
- **支付宝扫码登录**：OAuth2.0 标准流程
- **访客模式**：未登录用户基于客户端指纹生成唯一 ID，保证会话连续性
- **用户级别**：admin（管理员）、vip（会员）、normal（普通用户）

### 8. 简历管理

- **工作经历**：支持 CRUD，包含职位、岗位、描述、排序、显示控制
- **项目经历**：关联工作经历，包含项目名称、周期、背景、职责
- **项目成果**：关联项目，记录具体成果内容
- **技术栈**：关联工作经历，记录使用的技术
- **在线编辑器**：分步式编辑界面（工作经历 → 项目经历），支持实时预览和全屏简历查看
- **AI 文案润色**：集成 AI 润色功能，对工作描述、项目背景、个人职责进行智能优化
  - 用户可通过顶部开关控制 AI 功能的启用/禁用
  - 基于 STAR 法则优化表述，突出量化成果
  - 润色结果通过确认弹窗让用户决定是否替换原文

### 9. 费用统计

- **多维度统计**：支持按模型、用户、时间段三个维度分析 AI 使用费用
- **总览面板**：展示总费用、总 Token 数、AI 回复次数、平均费用等关键指标
- **可视化图表**：每日费用趋势柱状图、模型费用分布条形图
- **时间范围**：支持近 7 天 / 30 天 / 90 天切换查看
- **详细报表**：按模型和按用户的详细统计表格

### 10. AI 审计日志

- **操作审计**：记录 AI 交互的审计日志，包括用户操作、模型调用、内容审核等
- **日志查询**：支持分页查询审计日志，按会话查看历史记录
- **统计分析**：提供审计数据的统计概览

### 11. 国际化（i18n）

- **后端支持**：基于 Spring `MessageSource` 的国际化消息，`I18nUtils` 工具类提供便捷的消息获取
- **前端框架**：轻量级 `I18n.js` 工具，支持 `I18n.t()` 翻译函数、`data-i18n` 属性自动翻译、语言切换事件
- **语言包**：中文（zh-CN）和英文（en-US）双语支持，JSON 格式易于扩展
- **语言切换**：导航栏集成语言选择器，Cookie 持久化用户偏好
- **扩展方式**：新增语言只需添加对应的 `.properties` 和 `.json` 翻译文件

### 12. 系统管理

- **用户管理**：分页查询、级别修改、删除用户、重置密码
- **角色管理**：角色 CRUD、角色分配菜单权限、用户角色分配（仅 super_admin 可管理）
- **菜单管理**：树形菜单配置（目录/菜单/按钮、权限标识、排序、显示控制）
- **AI 模型管理**：模型 CRUD、启用/禁用、激活切换、价格配置

---

## 模块架构

```
Brief-Wisdom/
├── brief-wisdom-persistence/   # 数据持久化模块（Entity + Mapper + Repository）
├── brief-wisdom-ai/            # AI 智能模块（Service + 业务逻辑）
├── brief-wisdom-system/        # 系统模块（用户/角色/菜单/认证/OAuth）
├── brief-wisdom-web/           # Web 应用模块（Controller + 配置 + 前端资源）
├── brief-wisdom-api/           # API 接口定义模块
├── brief-wisdom-service/       # 通用服务模块
├── brief-wisdom-resume/        # 个人简历模块
└── brief-wisdom-common/        # 公共模块（DTO + 工具类 + 常量 + 安全注解 + i18n）
```

**依赖关系**：

```
brief-wisdom-web (Web入口)
    ├── brief-wisdom-ai (AI功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-system (系统管理)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-resume (简历功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-api (API定义)
    ├── brief-wisdom-service (通用服务)
    └── brief-wisdom-common (公共DTO/常量/注解)
```

### 各模块职责

| 模块          | 职责            | 关键类                                                                                                                              |
|-------------|---------------|-----------------------------------------------------------------------------------------------------------------------------------|
| persistence | 数据存储和访问层      | `ChatUser`, `ChatSession`, `ChatMessage`, `AiModel`, `SysRole`, `SysMenu`, `RoleMenu`, `UserRole`, `WorkExperience`, `Project`, `KnowledgeBase`, `KnowledgeDocument`, `AiAuditLog`, 各 Mapper/Repository |
| ai          | AI 服务和业务逻辑    | `AiAgentService`, `ChatSyncService`, `ContentFilterService`, `RateLimitService`, `SystemPrompts`, `KnowledgeService`, `SseChatSyncService`, `WebSocketChatSyncService`, `AiAuditService`, `AiManageService`, `AiModelService`, `ChatModelRegistry` |
| system      | 系统管理和认证       | `AuthService`, `UserService`, `MenuService`, `RoleService`, `WechatAuthService`, `DingtalkAuthService`, `AlipayAuthServiceImpl`, `UserContextHelper`, `OAuthProperties` |
| web         | Web 入口、配置和前端资源 | `WebApplication`, `AiAgentController`, `AuthController`, `UserController`, `RoleController`, `MenuController`, `ResumeAiController`, `AiAuditController`, `SecurityConfig`, `RedisConfig`, `I18nConfig`, `PermissionInterceptor`, `WebSocketConfig`, `ResultAutoWrapperAdvice` |
| resume      | 个人简历展示和管理     | `ResumeController`, `ResumeManageController`, `ResumeService`, `ResumeManageService`                                               |
| common      | 公共 DTO、常量、注解和 i18n | `Result`, `PageResult`, `CachePrefix`, `RequiresPermission`, `SessionMetaDTO`, `ChatMessageDTO`, `AiModelDTO`, `KnowledgeDTO`, `CostStatisticsDTO`, `I18nUtils` 等 |
| api         | API 接口定义和 DTO | 通用响应封装                                                                                                                             |
| service     | 通用服务层         | 业务服务类                                                                                                                              |

---

## 技术栈

| 组件            | 版本/说明                        |
|---------------|------------------------------|
| Java          | 17                           |
| Spring Boot   | 3.5.7                        |
| Spring AI     | 1.0.0 (OpenAI 兼容协议对接 DashScope) |
| Spring Security | 6                          |
| Spring Session | Redis 存储（`bw:session` 命名空间） |
| Spring Cache  | Redis 实现，按业务域划分缓存域          |
| ORM           | MyBatis-Plus 3.5.5           |
| 数据库           | MySQL 8.0+                   |
| 缓存            | Redis + Lettuce 连接池          |
| 分布式锁          | Redisson 3.40.2（独立客户端，避免与 Spring Data Redis 冲突） |
| 实时通信          | SSE (Server-Sent Events) / WebSocket |
| 前端            | 原生 HTML/CSS/JS                |
| 构建工具          | Maven                        |

---

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven（或使用项目自带 `mvnw`）

### 2. 初始化数据库

```bash
# 完整初始化（含建表 + RBAC + 初始数据，推荐使用）
mysql -u root -p < brief-wisdom-web/src/main/resources/init-20260629.sql
```

> `init-20260629.sql` 为最新一体化初始化脚本，包含所有表结构、RBAC 权限、菜单树、AI 模型、简历示例数据。

### 3. 启动 Redis

确保 Redis 服务已启动（默认 `localhost:6379`）。

### 4. 配置 AI 提供商

编辑 `brief-wisdom-web/src/main/resources/application.yml`，配置 AI 模型参数和 Redis 连接信息。

### 5. 启动应用

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

**方式一：执行一体化初始化脚本（推荐）**

```bash
mysql -u root -p123456 < brief-wisdom-web/src/main/resources/init-20260629.sql
```

该脚本包含：
1. 创建数据库 `brief_wisdom`
2. 创建所有表（含 RBAC 表：`sys_role`、`sys_user_role`、`sys_role_menu`）
3. 插入默认用户：`default-user/guest`（访客）和 `admin/mouhin`（超级管理员）
4. 插入默认角色：super_admin、admin、normal
5. 插入树形菜单（含系统设置子菜单：用户管理、角色管理、菜单管理、费用统计）
6. 插入角色-菜单权限分配
7. 插入 AI 模型配置（qwen-max/plus/turbo/qwen3.7-plus/deepseek 等）
8. 插入简历示例数据

**其他 SQL 脚本**：

| 脚本文件 | 说明 |
|---------|------|
| `init-20260629.sql` | 一体化初始化脚本（推荐，包含所有表结构和初始数据） |
| `init.sql` | 基础建表脚本（chat_user/chat_session/chat_message） |
| `ai_model_init.sql` | AI 模型初始化数据（含价格配置） |
| `knowledge_init.sql` | 知识库初始数据 |
| `resume_init.sql` | 简历示例数据 |
| `menu_init.sql` | 菜单初始化 |
| `rbac_migration.sql` | RBAC 权限迁移脚本 |
| `ai_audit_init.sql` | AI 审计日志表建表脚本 |
| `test-audit-data.sql` | 审计测试数据 |

**方式二：应用启动时自动建表**

MyBatis-Plus 启动后会自动创建表结构（需提前创建数据库），但 RBAC 初始数据仍需手动执行 `rbac_migration.sql`。

### 数据库备份与恢复

```bash
# 备份
mysqldump -u root -p brief_wisdom > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p brief_wisdom < backup_20260629.sql
```

---

## 数据库表结构

### 数据关系

```
chat_user (用户)
    ├── chat_session (会话) [一对多]
    │       └── chat_message (消息) [一对多]
    ├── user_oauth (第三方登录绑定) [一对多]
    ├── ai_audit_log (AI审计日志) [一对多]
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

### sys_menu（系统菜单表 - RBAC）

| 字段          | 类型           | 说明                       |
|-------------|--------------|--------------------------|
| id          | BIGINT       | 自增主键                     |
| parent_id   | BIGINT       | 父菜单ID，0表示顶级             |
| name        | VARCHAR(100) | 菜单名称                     |
| url         | VARCHAR(500) | 菜单链接                     |
| icon        | VARCHAR(100) | 菜单图标                     |
| target      | VARCHAR(20)  | 打开方式: _self/_blank       |
| type        | TINYINT      | 菜单类型: 0-目录, 1-菜单, 2-按钮  |
| permission  | VARCHAR(200) | 权限标识（如 `user:list`）       |
| sort_order  | INT          | 排序序号                     |
| is_visible  | TINYINT      | 是否显示: 1-显示, 0-隐藏         |
| require_login | TINYINT    | 是否需要登录: 0-否, 1-是        |
| create_time | DATETIME     | 创建时间                     |
| update_time | DATETIME     | 更新时间                     |
| is_deleted  | TINYINT      | 逻辑删除                     |

### sys_role（系统角色表 - RBAC）

| 字段          | 类型           | 说明                 |
|-------------|--------------|--------------------|
| id          | BIGINT       | 自增主键               |
| role_name   | VARCHAR(100) | 角色名称               |
| role_key    | VARCHAR(100) | 角色标识 (UNIQUE)      |
| description | VARCHAR(500) | 角色描述               |
| status      | TINYINT      | 状态: 1-启用, 0-禁用     |
| create_time | DATETIME     | 创建时间               |
| update_time | DATETIME     | 更新时间               |

### sys_user_role（用户-角色关联表）

| 字段      | 类型           | 说明       |
|---------|--------------|----------|
| id      | BIGINT       | 自增主键     |
| user_id | VARCHAR(36)  | 用户ID     |
| role_id | BIGINT       | 角色ID     |

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

| 字段          | 类型           | 说明                    |
|---------------|--------------|-----------------------|
| id            | BIGINT       | 自增主键                  |
| user_id       | VARCHAR(36)  | 用户 ID                  |
| session_id    | VARCHAR(36)  | 会话 ID                  |
| action        | VARCHAR(50)  | 操作类型（chat/polish/audit 等） |
| model         | VARCHAR(100) | 使用的 AI 模型             |
| input_tokens  | INT          | 输入 Token 数             |
| output_tokens | INT          | 输出 Token 数             |
| cost          | DOUBLE       | 费用（元）                 |
| status        | VARCHAR(20)  | 状态（success/failed/filtered） |
| create_time   | DATETIME     | 创建时间                  |

### 主键策略

所有表均使用 `@TableId(type = IdType.AUTO)` 自增主键，`user_id` / `session_id` 为业务唯一键（UNIQUE）。

| 表              | 主键                         | 业务键                             |
|----------------|----------------------------|---------------------------------|
| chat_user      | `id` BIGINT AUTO_INCREMENT | `user_id` VARCHAR(36) UNIQUE    |
| chat_session   | `id` BIGINT AUTO_INCREMENT | `session_id` VARCHAR(36) UNIQUE |
| chat_message   | `id` BIGINT AUTO_INCREMENT | -                               |
| user_oauth     | `id` BIGINT AUTO_INCREMENT | -                               |
| ai_model       | `id` BIGINT AUTO_INCREMENT | `model_name` VARCHAR(100)       |
| sys_menu       | `id` BIGINT AUTO_INCREMENT | -                               |
| sys_role       | `id` BIGINT AUTO_INCREMENT | `role_key` VARCHAR(100) UNIQUE  |
| sys_user_role  | `id` BIGINT AUTO_INCREMENT | `UNIQUE(user_id, role_id)`      |
| sys_role_menu  | `id` BIGINT AUTO_INCREMENT | `UNIQUE(role_id, menu_id)`      |

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

## RBAC 权限体系

### 架构概览

```
用户 (chat_user)
    ↓ 多对多 (sys_user_role)
角色 (sys_role)
    ↓ 多对多 (sys_role_menu)
菜单 (sys_menu) ← 树形结构 (parent_id)
```

### 三级角色定义

| 角色          | role_key    | 权限范围                                  |
|-------------|-------------|---------------------------------------|
| 超级管理员       | super_admin | 拥有系统所有权限，`@RequiresPermission` 自动放行 |
| 管理员         | admin       | 系统管理权限（除角色管理外）                        |
| 普通用户        | normal      | 基本访问权限（首页、简历、AI助手）                   |

### 菜单类型

| type | 名称   | 说明                         |
|------|------|----------------------------|
| 0    | 目录   | 一级分类，可包含子菜单（如"系统设置"）     |
| 1    | 菜单   | 具体页面链接（如"用户管理"、"角色管理"）   |
| 2    | 按钮   | 页面内操作权限标识（如 `user:create`） |

### 权限校验流程

1. **Spring Security 路由级**：基于 `hasRole()` 控制页面和 API 路径的访问
2. **`@RequiresPermission` 注解级**：`PermissionInterceptor` 拦截标注了权限注解的 Controller 方法
3. **前端 `checkMenuPermission`**：根据用户 `roles` 动态渲染菜单和按钮

### 权限标识示例

| 权限标识            | 说明     | 所属角色              |
|-----------------|--------|-------------------|
| `user:list`     | 用户管理   | admin, super_admin |
| `role:list`     | 角色管理   | super_admin 专属     |
| `menu:list`     | 菜单管理   | admin, super_admin |
| `system:settings` | 系统设置目录 | admin, super_admin |

---

## Redis 缓存架构

### Key 命名规范

项目全局前缀 `bw:`，按业务域划分：

| Key 模式                | 用途         | TTL    |
|------------------------|------------|--------|
| `bw:menu:tree::{key}` | 菜单树（按角色）   | 10 分钟  |
| `bw:menu:public::{key}` | 公开菜单      | 10 分钟  |
| `bw:menu:all::{key}`  | 全部菜单（含隐藏） | 10 分钟  |
| `bw:user:roles::{key}` | 用户角色 Key 列表 | 5 分钟  |
| `bw:user:perms::{key}` | 用户权限标识     | 5 分钟   |
| `bw:user:role::{key}` | 角色信息       | 30 分钟  |
| `bw:user:role:list`   | 角色列表（管理用） | 10 分钟  |
| `bw:resume:experiences` | 简历工作经历    | 30 分钟  |
| `bw:ai:model::{key}`  | AI 模型列表    | 15 分钟  |
| `bw:ai:session::{key}` | AI 会话历史    | 5 分钟   |
| `bw:ratelimit::{key}` | 接口限流计数     | 按窗口周期  |
| `bw:lock::{name}`     | 分布式锁       | 按锁定时间  |
| `bw:session::{id}`    | 用户会话（Spring Session） | 按配置 |

### Spring Cache 使用

通过 `@Cacheable` / `@CacheEvict` 注解实现自动缓存与失效：

```java
@Cacheable(value = CachePrefix.MENU_TREE_CACHE, key = "#roles")
public List<MenuDTO> getMenuTree(List<String> roles) { ... }

@CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true)
public void updateMenu(SysMenu menu) { ... }
```

### Session 持久化

```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: bw:session
```

用户登录 Session 存储在 Redis 中，支持分布式部署和多实例共享会话。

### 分布式锁

基于 Redisson 实现，通过自定义 `@DistributedLock` 注解使用：

```java
@DistributedLock(key = "'chat:' + #sessionId")
public void sendMessage(String sessionId, String content) { ... }
```

---

## AI 智能体

### 当前 AI 提供商

项目当前配置了两个 AI 提供商，通过 OpenAI 兼容协议对接：

- **阿里通义千问（DashScope）**：默认提供商，使用 qwen-max 模型
- **DeepSeek**：备选提供商，使用 deepseek-chat 模型

```yaml
# application.yml 中的提供商配置示例
app:
  ai:
    default-provider: dashscope
    providers:
      dashscope:
        type: openai-compatible
        base-url: https://dashscope.aliyuncs.com/compatible-mode
        api-key: ${AI_API_KEY}
        default-model: qwen-max
      deepseek:
        type: openai-compatible
        base-url: https://api.deepseek.com
        api-key: ${DEEPSEEK_API_KEY}
        default-model: deepseek-chat
```

### 多模型管理

系统通过 `ChatModelRegistry` 实现多提供商路由，支持动态扩展：

- **支持的提供商**：
  - **DashScope（通义千问）**：qwen-max、qwen-plus、qwen-turbo、qwen3.7-plus
  - **DeepSeek**：deepseek-chat、deepseek-v3-flash、deepseek-v3-pro
  - **可扩展**：在 `application.yml` 中配置即可接入 OpenAI、Anthropic Claude 等其他提供商
  
- **协议支持**：
  - **OpenAI 兼容协议**：适用于 DashScope、OpenAI、DeepSeek、Ollama 等
  - **Anthropic 协议**：适用于 Claude 系列模型
  
- **动态配置**：
  - **激活切换**：同时只激活一个模型作为默认模型
  - **启用/禁用**：可禁用某些模型，禁用后不在聊天界面显示
  - **价格配置**：配置每个模型的输入/输出价格（元/百万Token），自动计算费用
  - **扩展方式**：在 `application.yml` 的 `app.ai.providers` 中添加新提供商配置即可

### 页面上下文感知

AI 助手根据当前页面自动切换角色定位：

| 页面路径                    | 页面名称    | AI 角色定位                   |
|-------------------------|---------|---------------------------|
| `/`                     | 首页      | 综合性 AI 助手，知识问答、文案撰写、编程辅助等 |
| `/about.html`           | 个人简历    | 了解简历内容，协助优化描述、提炼亮点、面试建议   |
| `/resume-manage.html`   | 简历数据维护  | 协助整理工作经历、优化项目成果、技术栈分类建议   |
| `/system-settings.html` | 系统设置    | 协助了解系统配置、AI 模型管理等         |
| `/ai-manage.html`       | AI 助手管理 | 协助了解模型特性、提供模型选择建议         |

### 系统提示词

所有对话均附加系统提示词，包含：

- **伦理与法律合规**：不生成违法违规内容、不泄露隐私、不歧视侮辱
- **安全与风险控制**：拒绝越狱指令、拒绝 Prompt 注入、不编造事实
- **回复规范**：使用中文、专业友善、简洁明了

---

## 知识库管理

### 功能概述

知识库模块支持文档上传、向量化存储和语义检索，增强 AI 回答的准确性和专业性。

### 核心能力

- **知识库管理**：创建、编辑、删除知识库，支持树形结构组织，设置访问权限
- **文档管理**：支持三种文档类型
  - **内部文档**：使用富文本编辑器直接创建和编辑内容
  - **文件上传**：上传 PDF、Word、TXT、MD 等格式文件
  - **外部链接**：引用外部文档链接，统一管理知识资源
- **文档搜索**：支持按标题关键词搜索文档，快速定位所需内容
- **浏览次数统计**：自动记录文档浏览次数，了解热门文档
- **权限控制**：不同知识库可设置不同的访问权限

### 数据表结构

#### knowledge_base（知识库表）

| 字段          | 类型           | 说明                   |
|-------------|--------------|----------------------|
| id          | BIGINT       | 自增主键                 |
| name        | VARCHAR(200) | 知识库名称               |
| description | TEXT         | 知识库描述               |
| type        | VARCHAR(50)  | 知识库类型: general/professional |
| is_enabled  | TINYINT      | 是否启用: 1-启用, 0-禁用     |
| sort_order  | INT          | 排序序号                 |
| create_time | DATETIME     | 创建时间                 |
| update_time | DATETIME     | 更新时间                 |
| is_deleted  | TINYINT      | 逻辑删除                 |

#### knowledge_document（知识文档表）

| 字段              | 类型           | 说明                      |
|-----------------|--------------|-------------------------|
| id              | BIGINT       | 自增主键                    |
| kb_id           | BIGINT       | 关联 knowledge_base.id    |
| title           | VARCHAR(500) | 文档标题                    |
| content         | LONGTEXT     | 文档内容                    |
| file_name       | VARCHAR(200) | 原始文件名                   |
| file_type       | VARCHAR(50)  | 文件类型: txt/md/pdf/docx   |
| chunk_count     | INT          | 分块数量                    |
| status          | VARCHAR(20)  | 状态: pending/processing/completed/failed |
| error_msg       | TEXT         | 错误信息                    |
| sort_order      | INT          | 排序序号                    |
| create_time     | DATETIME     | 创建时间                    |
| update_time     | DATETIME     | 更新时间                    |
| is_deleted      | TINYINT      | 逻辑删除                    |

### API 接口

| 接口                                  | 方法     | 说明            |
|-------------------------------------|--------|---------------|
| `/api/knowledge/bases`              | GET    | 获取知识库列表      |
| `/api/knowledge/bases/tree`         | GET    | 获取知识库树形结构    |
| `/api/knowledge/bases/paged`        | GET    | 分页获取知识库列表    |
| `/api/knowledge/bases`              | POST   | 创建知识库        |
| `/api/knowledge/bases/{id}`         | PUT    | 更新知识库        |
| `/api/knowledge/bases/{id}`         | DELETE | 删除知识库        |
| `/api/knowledge/bases/{parentId}/children` | GET | 获取子知识库列表     |
| `/api/knowledge/documents`          | GET    | 获取文档列表       |
| `/api/knowledge/documents`          | POST   | 上传文档         |
| `/api/knowledge/documents/{id}`     | GET    | 获取文档详情       |
| `/api/knowledge/documents/{id}`     | PUT    | 更新文档         |
| `/api/knowledge/documents/{id}`     | DELETE | 删除文档         |
| `/api/knowledge/documents/search`   | GET    | 按标题搜索文档      |
| `/api/knowledge/search`             | POST   | 语义检索         |

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

---

## API 接口

### AI 会话管理

| 接口                                    | 方法     | 说明                                     |
|---------------------------------------|--------|----------------------------------------|
| `/api/ai/session`                     | POST   | 创建新会话（支持传入 pageContext）                |
| `/api/ai/session/{sessionId}`         | DELETE | 删除会话（逻辑删除）                             |
| `/api/ai/session/{sessionId}/title`   | PUT    | 重命名会话标题                                |
| `/api/ai/sessions?page=1&size=20`     | GET    | 分页获取会话列表（返回 `records/total/hasMore` 等） |
| `/api/ai/session/{sessionId}/history` | GET    | 分页获取会话历史消息（第1页为最新消息）                   |
| `/api/ai/config/pagination`           | GET    | 获取分页配置（sessionList/messageHistory）     |

### AI 聊天

| 接口                                 | 方法   | 说明                |
|------------------------------------|------|-------------------|
| `/api/ai/chat`                     | POST | 简单聊天（无上下文）        |
| `/api/ai/chat-with-prompt`         | POST | 带系统提示的聊天          |
| `/api/ai/ask`                      | POST | 智能问答              |
| `/api/ai/chat/session/{sessionId}` | POST | 带上下文的会话聊天（支持指定模型） |
| `/api/ai/chat/session/{sessionId}/stream` | GET  | SSE 流式聊天（打字机效果） |
| `/api/ai/message/save`             | POST | 保存流式消息            |
| `/api/ai/models/enabled`           | GET  | 获取启用的模型列表         |

### AI 多端同步

| 接口                    | 方法     | 说明           |
|-----------------------|--------|--------------|
| `/api/ai/sync/events` | GET    | SSE 实时同步事件流  |
| `/api/ai/sync/events` | DELETE | 断开 SSE 连接    |
| `/api/ai/sync/status` | GET    | 获取同步状态（指纹对比） |
| `/api/ai/sync/transport` | GET | 获取同步传输方式配置   |
| `/api/ai/config/chat` | GET    | 获取聊天配置（流式/同步） |

### AI 模型管理（需 admin/super_admin 角色）

| 接口                             | 方法     | 说明        |
|--------------------------------|--------|-----------|
| `/api/ai/models`               | GET    | 获取所有模型列表  |
| `/api/ai/models/enabled`       | GET    | 获取启用的模型列表 |
| `/api/ai/models/active`        | GET    | 获取当前激活的模型 |
| `/api/ai/models/activate/{id}` | PUT    | 切换激活模型    |
| `/api/ai/models`               | POST   | 新增模型      |
| `/api/ai/models`               | PUT    | 更新模型      |
| `/api/ai/models/{id}`          | DELETE | 删除模型      |
| `/api/ai/models/{id}/toggle`   | PUT    | 启用/禁用模型   |

### AI 管理后台（需 admin/super_admin 角色）

| 接口                                            | 方法  | 说明              |
|-----------------------------------------------|-----|-----------------|
| `/api/ai/manage/users`                        | GET | 获取用户列表（支持按级别筛选） |
| `/api/ai/manage/levels`                       | GET | 获取所有用户级别        |
| `/api/ai/manage/sessions/user/{userId}`       | GET | 按用户ID查询会话列表     |
| `/api/ai/manage/sessions/level/{level}`       | GET | 按用户级别查询会话列表     |
| `/api/ai/manage/session/{sessionId}/messages` | GET | 获取会话消息历史        |
| `/api/ai/manage/cost-statistics?days=30`      | GET | 获取费用统计（按模型/用户/日期） |

### AI 审计（需 admin/super_admin 角色）

| 接口                                       | 方法  | 说明           |
|------------------------------------------|-----|--------------|
| `/api/ai/audit/logs`                     | GET | 分页查询审计日志      |
| `/api/ai/audit/session/{sessionId}`      | GET | 查看会话审计记录      |
| `/api/ai/audit/statistics`               | GET | 审计统计概览        |

### 认证

| 接口                        | 方法   | 说明            |
|---------------------------|------|---------------|
| `/api/auth/register`      | POST | 用户注册          |
| `/api/auth/login`         | POST | 用户名/密码登录      |
| `/auth/wechat/login`      | GET  | 获取微信扫码登录 URL  |
| `/auth/wechat/callback`   | GET  | 微信授权回调        |
| `/auth/dingtalk/login`    | GET  | 获取钉钉扫码登录 URL  |
| `/auth/dingtalk/callback` | GET  | 钉钉授权回调        |
| `/auth/alipay/login`      | GET  | 获取支付宝扫码登录 URL |
| `/auth/alipay/callback`   | GET  | 支付宝授权回调       |
| `/api/auth/status`        | GET  | 检查登录状态（含 roles） |
| `/api/auth/user`          | GET  | 获取当前用户信息（需登录） |
| `/auth/logout`            | POST | 退出登录          |

### 用户管理（需 admin/super_admin 角色）

| 接口                              | 方法     | 说明       |
|---------------------------------|--------|----------|
| `/api/user/list`                | GET    | 分页获取用户列表 |
| `/api/user/levels`              | GET    | 获取用户级别选项 |
| `/api/user/{id}/level`          | PUT    | 修改用户级别   |
| `/api/user/{id}`                | DELETE | 删除用户     |
| `/api/user/{id}/reset-password` | PUT    | 重置用户密码   |

### 角色管理（需 super_admin 角色）

| 接口                                  | 方法     | 说明          |
|-------------------------------------|--------|-------------|
| `/api/role/list`                    | GET    | 获取角色列表      |
| `/api/role`                         | POST   | 创建角色        |
| `/api/role`                         | PUT    | 更新角色        |
| `/api/role/{id}`                    | DELETE | 删除角色        |
| `/api/role/{id}/menus`              | GET    | 获取角色关联的菜单ID |
| `/api/role/{id}/menus`              | PUT    | 更新角色菜单权限    |
| `/api/role/user/{userId}`           | GET    | 获取用户的角色列表   |
| `/api/role/user/{userId}`           | PUT    | 更新用户角色      |

### 菜单管理

| 接口                      | 方法     | 说明          |
|-------------------------|--------|-------------|
| `/api/menu/list`        | GET    | 获取可见菜单列表    |
| `/api/menu/all`         | GET    | 获取全部菜单（含隐藏） |
| `/api/menu/tree`        | GET    | 获取菜单树结构     |
| `/api/menu/my-menus`    | GET    | 获取当前用户菜单（需登录） |
| `/api/menu`             | POST   | 新增菜单        |
| `/api/menu`             | PUT    | 更新菜单        |
| `/api/menu/{id}`        | DELETE | 删除菜单        |
| `/api/menu/{id}/toggle` | PUT    | 切换菜单显示/隐藏   |

### 简历数据

| 接口                                     | 方法     | 说明       |
|----------------------------------------|--------|----------|
| `/api/resume/experiences`              | GET    | 获取所有工作经历 |
| `/api/resume/manage/experiences`       | GET    | 获取工作经历列表 |
| `/api/resume/manage/experiences`       | POST   | 创建工作经历   |
| `/api/resume/manage/experiences/{id}`  | PUT    | 更新工作经历   |
| `/api/resume/manage/experiences/{id}`  | DELETE | 删除工作经历   |
| `/api/resume/manage/projects`          | GET    | 获取项目列表   |
| `/api/resume/manage/projects`          | POST   | 创建项目     |
| `/api/resume/manage/projects/{id}`     | PUT    | 更新项目     |
| `/api/resume/manage/projects/{id}`     | DELETE | 删除项目     |
| `/api/resume/manage/achievements`      | GET    | 获取项目成果列表 |
| `/api/resume/manage/achievements`      | POST   | 创建项目成果   |
| `/api/resume/manage/achievements/{id}` | PUT    | 更新项目成果   |
| `/api/resume/manage/achievements/{id}` | DELETE | 删除项目成果   |
| `/api/resume/manage/stacks`            | GET    | 获取技术栈列表  |
| `/api/resume/manage/stacks`            | POST   | 创建技术栈    |
| `/api/resume/manage/stacks/{id}`       | PUT    | 更新技术栈    |
| `/api/resume/manage/stacks/{id}`       | DELETE | 删除技术栈    |
| `/api/resume/ai/polish`                | POST   | AI 文本润色（需 resume:manage 权限） |

### 角色管理（需 super_admin 角色）

| 接口                                  | 方法     | 说明          |
|-------------------------------------|--------|-------------|
| `/api/role/list`                    | GET    | 获取角色列表      |
| `/api/role/enabled`                 | GET    | 获取启用的角色列表   |
| `/api/role`                         | POST   | 创建角色        |
| `/api/role`                         | PUT    | 更新角色        |
| `/api/role/{id}`                    | GET    | 获取角色详情      |
| `/api/role/{id}`                    | DELETE | 删除角色        |
| `/api/role/{id}/menus`              | GET    | 获取角色关联的菜单ID |
| `/api/role/{id}/menus`              | PUT    | 更新角色菜单权限    |
| `/api/role/user/{userId}`           | GET    | 获取用户的角色列表   |
| `/api/role/assign/{userId}`         | PUT    | 更新用户角色      |

---

## 用户认证

### 认证方式

| 认证方式   | 说明                  |
|--------|---------------------|
| 用户名/密码 | 标准注册登录，密码 BCrypt 加密 |
| 微信扫码   | OAuth2.0 标准流程       |
| 钉钉扫码   | OAuth2.0 标准流程       |
| 支付宝扫码  | OAuth2.0 标准流程       |
| 访客模式   | 基于客户端指纹生成唯一 ID      |

### 访客指纹生成

未登录用户基于以下因子生成唯一访客 ID：

- **客户端 IP**：支持反向代理场景（X-Forwarded-For、X-Real-IP）
- **浏览器类型**：Chrome、Firefox、Safari、Edge、Opera 等（不含版本号）
- **设备类型**：PC、Mobile、Tablet

指纹算法：`SHA256(IP + browserType + deviceType)` 前 16 位，格式为 `guest-{fingerprint}`。

同一设备（同一 IP + 同一浏览器 + 同一设备类型）始终生成相同的访客 ID，保证会话连续性。

### 安全配置

`SecurityConfig` 基于 Spring Security 6 实现 RBAC 路由权限：

- **公开访问**：静态资源、AI 聊天 API、认证 API、简历展示 API、菜单列表 API
- **admin/super_admin**：AI 管理后台、菜单管理、用户管理、简历数据管理、系统设置页面
- **super_admin 专属**：角色管理 API
- **需登录**：获取当前用户信息、获取当前用户菜单
- **Session 管理**：同一用户最多 1 个并发 Session，新登录踢掉旧会话
- **Session 持久化**：Redis 存储（`bw:session` 命名空间）
- 禁用 CSRF、formLogin、httpBasic
- 未认证返回 `401 JSON`，权限不足返回 `403 JSON`

### 用户级别

| 级别     | 说明          |
|--------|-------------|
| admin  | 管理员，可访问管理后台 |
| vip    | 会员，享受更多权限   |
| normal | 普通用户，基础权限   |

---

## 前端页面

### 页面列表

| 页面     | 路径                      | 权限要求                  | 说明                         |
|--------|-------------------------|-----------------------|----------------------------|
| 主页     | `/` / `index.html`      | 公开                    | AI 聊天助手入口，右下角悬浮按钮打开聊天窗口    |
| 个人简历   | `/about.html`           | 公开                    | 个人简历展示页，支持深色/浅色主题切换、PDF 导出 |
| 登录注册   | `/login.html`           | 公开                    | 用户登录/注册页面，支持用户名密码和第三方登录    |
| 简历数据维护 | `/resume-manage.html`   | admin/super_admin     | 工作经历、项目、成果、技术栈的 CRUD 管理，在线编辑器 |
| 系统设置   | `/system-settings.html` | admin/super_admin     | 用户管理、角色管理、菜单管理等系统配置        |
| AI助手管理 | `/ai-manage.html`       | admin/super_admin     | AI 模型管理、会话历史、费用统计、用户会话查看   |
| AI 审计  | `/ai-audit.html`        | admin/super_admin     | AI 交互审计日志查看和统计             |
| 测试页    | `/test-session.html`    | 公开                    | 会话 API 独立测试页面              |

### 组件化架构

系统采用**组件化架构**，将功能模块拆分为独立的 HTML 模板 + JS 逻辑文件：

#### 组件结构

```
components/
├── experience-management.template.js  # 工作经历 HTML 模板
├── experience-management.js           # 工作经历业务逻辑
├── project-management.template.js     # 项目经历 HTML 模板
├── project-management.js              # 项目经历业务逻辑
├── achievement-management.template.js # 项目成果 HTML 模板
├── achievement-management.js          # 项目成果业务逻辑
├── tech-stack-management.template.js  # 技术栈 HTML 模板
├── tech-stack-management.js           # 技术栈业务逻辑
├── online-editor.template.js          # 在线编辑器 HTML 模板
├── online-editor.js                   # 在线编辑器业务逻辑（含 AI 润色）
├── user-management.template.js        # 用户管理 HTML 模板
├── user-management.js                 # 用户管理业务逻辑
├── role-management.template.js        # 角色管理 HTML 模板
├── role-management.js                 # 角色管理业务逻辑
├── menu-management.template.js        # 菜单管理 HTML 模板
├── menu-management.js                 # 菜单管理业务逻辑
├── model-management.template.js       # AI 模型管理 HTML 模板
├── model-management.js                # AI 模型管理业务逻辑
├── ai-assistant.template.js           # AI 助手 HTML 模板
├── knowledge-management.template.js   # 知识库管理 HTML 模板
├── knowledge-management.js            # 知识库管理业务逻辑
├── audit-log.template.js              # 审计日志 HTML 模板
├── audit-log.js                       # 审计日志业务逻辑
├── session-history.template.js        # 会话历史 HTML 模板
├── session-history.js                 # 会话历史业务逻辑
├── cost-statistics.template.js        # 费用统计 HTML 模板
├── cost-statistics.js                 # 费用统计业务逻辑
└── component-loader.js                # 组件加载器（统一注册和初始化）
```

#### 组件特性

- **HTML 与 JS 分离**：模板文件负责生成 HTML，JS 文件负责业务逻辑
- **动态加载**：通过 `component-loader.js` 统一注册和初始化组件
- **Tab 切换**：通过 `system-settings-lite.js` 管理 Tab 和内容容器
- **全局方法暴露**：组件方法暴露到 `window` 对象，供 HTML 事件调用
- **弹窗管理**：统一的 `openModal/closeModal` 机制，支持动态创建弹窗
- **API 封装**：统一的 `apiRequest` 函数，处理请求和错误

#### 组件规范

每个组件必须包含：
1. ✅ HTML 模板文件（`.template.js`）
2. ✅ JS 逻辑文件（`.js`）
3. ✅ 在 `system-settings-lite.js` 或 `ai-manage-lite.js` 中配置映射
4. ✅ 通过 `registerComponent()` 注册
5. ✅ 暴露必要的全局方法到 `window.xxxManagement`

### 前端资源文件

#### CSS 样式表

| 文件 | 说明 |
|------|------|
| `css/navbar.css` | 公共导航栏样式 |
| `css/chat.css` | AI 聊天组件样式 |
| `css/about.css` | 个人简历页面样式 |
| `css/login.css` | 登录注册页面样式 |
| `css/resume-manage.css` | 简历管理页面样式 |
| `css/resume-editor.css` | 在线编辑器样式（含 AI 润色按钮） |
| `css/system-settings.css` | 系统设置页面样式 |
| `css/ai-manage.css` | AI 管理页面样式（含费用统计） |

#### JavaScript 脚本

| 文件 | 说明 |
|------|------|
| `js/navbar.js` | 公共导航栏（含语言切换器） |
| `js/i18n.js` | 国际化框架（翻译、语言切换） |
| `js/chat.js` | AI 聊天组件逻辑 |
| `js/about.js` | 个人简历页面逻辑 |
| `js/auth.js` | 认证工具函数 |
| `js/auth-page.js` | 登录注册页面逻辑 |
| `js/resume-manage-lite.js` | 简历管理页面入口（Tab 切换 + 组件加载） |
| `js/resume-editor.js` | 在线编辑器（旧版，已被组件化替代） |
| `js/system-settings-lite.js` | 系统设置页面入口（Tab 切换 + 组件加载） |
| `js/ai-manage-lite.js` | AI 管理页面入口（Tab 切换 + 组件加载） |
| `js/knowledge.js` | 知识库管理逻辑 |

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

### 问题 6：Redis 反序列化失败

**症状**：日志报 `ClassCastException` 或 `SerializationException`

**原因**：Redis 序列化器变更后旧缓存数据不兼容

**解决**：
```bash
# 清空项目相关缓存
redis-cli FLUSHDB
# 或清空指定缓存域
redis-cli KEYS "bw:*" | xargs redis-cli DEL
```

### 问题 7：StackOverflowError（Redis pExpire 无限递归）

**症状**：应用启动后登录时抛出 `java.lang.StackOverflowError`，堆栈显示 `DefaultedRedisConnection.pExpire()` 无限递归

**原因**：`redisson-spring-boot-starter` 自动注册的 `RedissonConnectionFactory` 与 Spring Data Redis 3.5.5 的 `DefaultedRedisConnection.pExpire()` 默认方法存在递归 bug

**解决**：
1. 替换依赖：将 `redisson-spring-boot-starter` 改为独立 `redisson` 核心包
2. 手动配置：创建 `RedissonConfig.java` 手动初始化 `RedissonClient`
3. 职责分离：Lettuce 处理 Session/Cache/RedisTemplate，Redisson 仅用于分布式锁

详见 [FIX_SUMMARY.md](FIX_SUMMARY.md)

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
- **静态资源缓存**：开发阶段禁用浏览器缓存，确保修改即时生效

### 验证清单

- [ ] MySQL 数据库 `brief_wisdom` 已创建，执行 `init-20260629.sql` 完成初始化
- [ ] Redis 服务已启动（`localhost:6379`）
- [ ] 默认用户（`default-user/guest` 和 `admin/mouhin`）已插入
- [ ] RBAC 角色和权限分配已初始化
- [ ] AI 模型配置已初始化（qwen-plus 为默认激活模型）
- [ ] 应用启动无报错
- [ ] 访问 http://localhost:8090 页面正常显示
- [ ] 创建会话、发送消息、切换会话功能正常
- [ ] 会话列表无限滚动加载正常
- [ ] **流式输出正常**（打字机效果，无内容消失问题）
- [ ] **页面无抖动**（流式输出完成后，左侧会话列表局部更新，无刷新感）
- [ ] AI 回复正常返回（Markdown 渲染）
- [ ] SSE/WebSocket 实时同步正常（多设备测试）
- [ ] 访问 http://localhost:8090/about.html 个人简历页面正常
- [ ] 角色权限控制正常（普通用户无法访问管理页面）
- [ ] 知识库文档上传和搜索功能正常

---

## 扩展方向

### 短期规划（3-6个月）

| 方向 | 优先级 | 说明 |
|------|--------|------|
| ✅ **费用统计报表** | P0 | **已完成**：多维度费用统计（按模型/用户/日期），可视化图表，时间范围筛选 |
| ✅ **多语言国际化** | P1 | **已完成**：前后端 i18n 框架，中英文支持，语言切换器 |
| ✅ **AI 文案润色** | P1 | **已完成**：在线编辑器集成 AI 润色，STAR 法则优化，用户开关控制 |
| **AI 插件系统** | P1 | 支持 AI 调用外部工具（搜索引擎、API 调用、代码执行器），扩展能力边界 |
| **移动端适配** | P2 | 响应式设计优化，PWA 支持，离线缓存策略 |

### 中期规划（6-12个月）

| 方向 | 说明 |
|------|------|
| **团队协作功能** | 共享会话、团队知识库、权限分级管理 |
| **智能体工作流** | 可视化编排 AI Agent 工作流，支持多步骤任务自动化 |
| **数据分析看板** | 用户行为分析、热门问题统计、模型性能监控 |
| **语音交互** | 语音输入/输出，实时语音对话模式 |
| **图像理解** | 接入多模态模型，支持图片上传和分析 |

### 长期愿景（1-2年）

| 方向 | 说明 |
|------|------|
| **企业级部署** | 私有化部署方案，Kubernetes 容器化，高可用架构 |
| **自定义模型训练** | 基于领域数据微调模型，提供专属 AI 助手 |
| **开放平台** | API 网关，开发者生态，第三方应用市场 |
| **AI 安全审计** | 内容合规审查，操作日志追溯，风险预警机制 |

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

- **代码风格**：遵循 AGENTS.md 中的编码规范
- **提交信息**：使用清晰的 commit message，说明修改内容
- **分支管理**：feature 分支开发，完成后合并到 main 分支
- **测试**：新功能需包含单元测试或集成测试

---

## License

MIT License