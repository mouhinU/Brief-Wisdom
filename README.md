# Brief-Wisdom

> 基于 Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 构建的 AI 智能对话平台，集成多模型管理、RAG 知识库、跨会话记忆、AI 工具调用、RBAC 权限体系、多端实时同步、多方式认证、简历管理、安全合规等完整能力矩阵。

---

## 项目简介

Brief-Wisdom 是一个全功能 AI 智能对话平台，围绕"对话即服务"理念构建，核心能力涵盖：

- **AI 智能对话**：多模型管理（DashScope / DeepSeek / OpenAI / Anthropic）、SSE 流式输出、页面上下文感知、跨会话记忆、Token 统计与费用计算
- **RAG 知识库**：Redis Vector Store (RediSearch) 向量化存储、三种文档类型、异步向量化、AI 按需语义检索
- **AI 工具调用**：12 个 Function Calling 工具，覆盖知识检索、数据分析、集成自动化三大领域
- **跨会话记忆**：正则自动提取 + AI 主动记忆，持久化用户偏好与上下文
- **多方式认证**：用户名密码、手机短信、微信扫码、钉钉扫码、支付宝扫码、SSO 令牌、访客模式
- **RBAC 权限控制**：三级角色、树形菜单、注解级权限、路由级保护
- **多端实时同步**：SSE / WebSocket 可配置切换，统一事件协议，多设备同时在线
- **安全合规**：三层防线（提示词→输入过滤→输出过滤）、Redis 分布式限流、XSS 防护
- **简历管理**：工作经历 / 项目 / 成果 / 技术栈 CRUD，AI 润色（STAR 方法论），PDF 导出
- **费用统计**：多维度 AI 使用费用统计，按模型 / 用户 / 日期分析
- **AI 审计系统**：输入拦截、输出过滤、风险检测全链路审计日志
- **国际化（i18n）**：中英文支持，Cookie 语言切换
- **Redis 缓存架构**：全局前缀 `bw:`，按业务域划分 TTL，分布式锁支持

---

## 模块架构

```
Brief-Wisdom/
├── brief-wisdom-web/           # Web 入口（Controller + 配置 + 前端资源）
├── brief-wisdom-ai/            # AI 领域模块（对话、知识库、模型管理、工具调用）
├── brief-wisdom-system/        # 系统模块（用户/角色/菜单/认证/OAuth）
├── brief-wisdom-resume/        # 简历模块（CRUD + AI 润色）
├── brief-wisdom-persistence/   # 数据持久化（Entity + Mapper + Repository）
├── brief-wisdom-api/           # API 接口定义
├── brief-wisdom-service/       # 通用服务层（逐步迁移至领域模块）
└── brief-wisdom-common/        # 公共模块（DTO + 常量 + 注解 + SPI 接口）
```

**依赖关系**：`web` → `ai` / `system` / `resume` / `api` → `service` → `persistence` → `common`

AI 模块通过 `ToolContextProvider` / `ResumeDataProvider` 接口（定义在 common 模块）实现依赖隔离，避免对业务模块的直接耦合。

---

## 技术栈

| 组件 | 版本 / 说明 |
|------|------------|
| Java | 21（Virtual Threads） |
| Spring Boot | 4.0.7 |
| Spring AI | 2.0.0 |
| Spring Security | 7 |
| Spring Session | Redis 存储（`bw:session` 命名空间） |
| Spring Cache | Redis 实现，按业务域划分缓存域 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+（RediSearch 向量检索）+ Jedis 7.4.1 连接池 |
| 分布式锁 | Redisson 3.40.2（`@DistributedLock` AOP 切面） |
| 实时通信 | SSE / WebSocket（`app.sync.transport` 可配置） |
| 前端 | 原生 HTML/CSS/JS（组件化架构，14+ 组件） |
| 构建工具 | Maven |
| API 文档 | SpringDoc OpenAPI 2.8.6（`/swagger-ui.html`） |
| DB 迁移 | Flyway（V1~V4） |
| 监控 | Actuator + Micrometer Prometheus |
| JSON 兼容 | Jackson 2.x（Spring Boot 4 默认 Jackson 3.x，已做兼容处理） |

---

## 快速开始

### 环境准备

- JDK 21+
- MySQL 8.0+
- Redis 6.0+（推荐启用 RediSearch 模块以支持知识库向量检索）
- Maven（或使用项目自带 `mvnw`）

### 初始化数据库

```bash
mysql -u root -p < brief-wisdom-web/src/main/resources/init-20260629.sql
```

> `init-20260629.sql` 为一体化初始化脚本，包含所有表结构、RBAC 权限、菜单树、AI 模型配置、简历示例数据。项目同时使用 Flyway 管理增量迁移（V1 基线 → V2 性能索引 → V3 链接描述字段 → V4 提醒表）。

### 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填写 AI API Key、第三方登录凭证等敏感配置
```

### 启动应用

```bash
./mvnw spring-boot:run -pl brief-wisdom-web
```

访问：http://localhost:8090

默认账号：`default-user/guest`（访客）、`admin/mouhin`（超级管理员）

---

## 核心功能

### 一、AI 智能对话

| 能力 | 说明 |
|------|------|
| 多模型管理 | 通过 `ChatModelRegistry` 统一管理 DashScope（通义千问）、DeepSeek、OpenAI、Anthropic（Claude） |
| 流式输出 | SSE 流式推送（打字机效果），支持流式对话终止 |
| 页面上下文感知 | AI 角色根据当前页面自动切换（`/`、`/about.html`、`/resume-manage.html`、`/system-settings.html`、`/ai-manage.html`） |
| 多会话管理 | 多会话并行，首次消息自动生成会话标题 |
| Token 统计 | 逐消息 Token 计数与费用计算（按百万 Token 定价） |
| 上下文窗口 | 最近 10 条消息 + 跨会话页面上下文（最多 3 个会话、每会话 5 条） |

### 二、RAG 知识库

| 能力 | 说明 |
|------|------|
| 向量存储 | Redis Vector Store (RediSearch)，`LazyVectorStore` 优雅降级 |
| 文档类型 | INTERNAL（富文本）、FILE（文件上传）、LINK（外部 URL） |
| 异步向量化 | 文档创建/更新时通过 `briefWisdomExecutor`（虚拟线程）异步向量化 |
| Embedding | text-embedding-v3，1024 维 |
| AI 按需检索 | `KnowledgeSearchTool` 供 AI 对话中主动调用语义搜索 |
| 文档管理 | `KnowledgeDocManagementTool` 供 AI 创建文档、列出知识库 |
| Markdown 导入 | `MarkdownImportService` 支持从文件系统批量导入 |
| 代码索引 | `ProjectCodeIndexService` 启动时构建内存代码索引，提供代码上下文 |

### 三、跨会话记忆（ChatMemory）

| 能力 | 说明 |
|------|------|
| 正则自动提取 | 自动识别并提取用户姓名、公司、角色、技术栈、风格偏好、当前项目 |
| AI 主动记忆 | 通过 `MemoryManagementTool` 支持列出 / 保存 / 删除记忆 |
| 记忆分类 | preference（偏好）、fact（事实）、context（上下文） |
| 上下文注入 | 每次对话自动将相关记忆注入系统提示词 |

### 四、安全与合规

| 层级 | 机制 |
|------|------|
| 第一层：系统提示词 | 伦理合规引导，约束 AI 行为边界 |
| 第二层：输入过滤 | 关键词黑名单拦截 + 提示词注入检测（10 条正则模式） |
| 第三层：输出过滤 | PII 敏感信息替换（身份证号、手机号、银行卡号） |
| 分布式限流 | Redis Lua 脚本滑动窗口：20 次/秒 + 1,440,000 次/天 |
| XSS 防护 | 前端 DOMPurify 过滤 |
| CORS | 集中配置跨域策略 |
| 审计日志 | `ContentSecurityException` 触发审计记录 |

### 五、认证体系（5 + 2 种方式）

| 方式 | 说明 |
|------|------|
| 用户名 + 密码 | BCrypt 加密存储 |
| 手机 + 短信验证码 | Redis 存储，5 分钟过期，频率限制 |
| 微信扫码登录 | 开放平台 OAuth |
| 钉钉扫码登录 | OAuth 2.0 |
| 支付宝扫码登录 | RSA2 签名认证 |
| SSO 令牌登录 | HMAC-SHA256 签名，Redis -backed |
| 访客模式 | 基于 IP + 浏览器 + 设备指纹的确定性 ID 生成 |

### 六、RBAC 权限体系

| 能力 | 说明 |
|------|------|
| 预设角色 | super_admin、admin、normal 三级 |
| 树形菜单 | 目录 → 菜单 → 按钮三级结构 |
| 注解控制 | `@RequiresPermission` 方法级权限校验 |
| 路由保护 | Spring Security 路由级权限拦截 |
| 权限解析 | super_admin 返回 null（无限制），其他角色收集所分配菜单的权限字符串 |
| 菜单过滤 | 按用户角色动态过滤可见菜单树 |

### 七、多端实时同步

| 能力 | 说明 |
|------|------|
| 传输协议 | SSE / WebSocket 可配置（`app.sync.transport`） |
| 事件协议 | 统一 JSON 事件（`session_created` / `session_deleted` / `message_added`） |
| 多设备在线 | 同一用户多设备同时在线并实时同步 |
| 断线重连 | SSE 浏览器内置重连；WebSocket 3 秒定时器重连 |

### 八、AI 审计系统

| 能力 | 说明 |
|------|------|
| 审计记录 | `AiAuditService` 记录输入拦截、输出过滤、风险检测 |
| 统计分析 | 按类型和风险等级统计审计数据 |
| 分页查看 | 支持分页、筛选查看审计日志 |
| 管理接口 | `AiAuditController` 提供审计数据查询 API |

### 九、简历管理

| 能力 | 说明 |
|------|------|
| 数据模型 | WorkExperience、Project、ProjectAchievement、WorkExperienceStack 四个实体 |
| CRUD 管理 | 完整增删改查 + 在线编辑器 |
| AI 润色 | 单字段润色、全简历润色、优化建议（STAR 方法论） |
| 展示页面 | 深色/浅色主题切换、PDF 导出 |
| 缓存 | Redis 缓存，30 分钟 TTL |

### 十、费用统计

| 能力 | 说明 |
|------|------|
| 逐消息追踪 | 每条消息记录 Token 用量与费用 |
| 多维分析 | 总体汇总、按模型、按用户、按日期、按日期+模型 |
| 批量查询 | 批量 SQL 查询，避免 N+1 问题 |

### 十一、Redis 缓存架构

| 缓存域 | TTL | 说明 |
|--------|-----|------|
| 菜单树 | 10 min | `bw:menu:*` |
| 用户角色/权限 | 5 min | `bw:user:role:*` / `bw:user:perm:*` |
| 简历数据 | 30 min | `bw:resume:*` |
| AI 模型 | 15 min | `bw:ai:model:*` |
| AI 会话 | 5 min | `bw:ai:session:*` |
| Session | 持久化 | `bw:session` 命名空间 |

全局前缀 `bw:`，Spring Cache 注解管理，Redisson 分布式锁（`@DistributedLock` AOP 切面）。

### 十二、国际化（i18n）

| 层级 | 方案 |
|------|------|
| 后端 | Spring `MessageSource`（`i18n/messages*.properties`） |
| 前端 | `i18n.js` + `zh-CN.json` / `en-US.json` |
| 语言切换 | Cookie 存储语言偏好 |

### 十三、前端架构

| 能力 | 说明 |
|------|------|
| 页面数量 | 7 个功能页面 + 1 个测试页面 |
| 组件体系 | 14+ JS 组件（`.js` + `.template.js` 配对） |
| 组件加载 | `component-loader.js` 统一注册和初始化 |
| AI 助手 | 浮动 Widget，全局可用 |
| 安全 | DOMPurify XSS 防护 |

### 十四、基础设施

| 能力 | 说明 |
|------|------|
| DB 迁移 | Flyway：V1（基线）→ V2（性能索引）→ V3（link_desc 改 TEXT）→ V4（chat_reminder 表） |
| 虚拟线程 | Java 21 Virtual Threads（`bw-vt-*` 命名） |
| 监控 | Actuator + Prometheus 指标暴露 |
| API 文档 | SpringDoc OpenAPI（`/swagger-ui.html`） |
| 异常处理 | `GlobalExceptionHandler` 处理 10+ 异常类型 |
| 响应包装 | `ResultAutoWrapperAdvice` 自动统一响应格式 |
| JSON 兼容 | Jackson 2.x 兼容（Spring Boot 4 默认 Jackson 3.x） |

---

## AI 工具调用（Function Calling）

共 12 个工具，分三个层级，通过 `ToolConfig` 使用 `MethodToolCallbackProvider` 统一注册。

### Tier 1 — 知识与代码

| # | 工具 | 说明 |
|---|------|------|
| 1 | `KnowledgeSearchTool` | 知识库语义向量搜索 |
| 2 | `CodeSearchTool` | 项目源码搜索 + 模块概览 |
| 3 | `ProjectCodeIndexService` | 启动时构建内存代码索引，为代码搜索提供支撑 |

### Tier 2 — 数据分析

| # | 工具 | 说明 |
|---|------|------|
| 4 | `ResumeAnalysisTool` | 简历四维度分析 |
| 5 | `SystemStatusTool` | 管理员专属系统状态（用户/会话/Redis） |
| 6 | `DatabaseQueryTool` | 超级管理员只读 SQL 查询 |
| 7 | `DateTimeTool` | 当前日期时间 + 日期差值计算 |
| 8 | `CalculatorTool` | 数学运算 + 百分比计算 |

### Tier 3 — 集成与自动化

| # | 工具 | 说明 |
|---|------|------|
| 9 | `MemoryManagementTool` | 用户长期记忆 CRUD |
| 10 | `WebFetchTool` | 网页元信息抓取（标题 + 描述） |
| 11 | `TranslationTool` | 多语言翻译（13 种语言） |
| 12 | `KnowledgeDocManagementTool` | 创建文档 + 列出知识库 |
| 13 | `ReminderTool` | 创建/列出提醒，支持自然语言时间 |

工具层通过 `ToolContextProvider` 和 `ResumeDataProvider` 接口（定义在 common 模块）实现与业务模块的依赖隔离。

---

## 前端页面

| 页面 | 路径 | 权限要求 | 说明 |
|------|------|---------|------|
| 主页 | `/` / `index.html` | 公开 | AI 聊天助手入口 |
| 个人简历 | `/about.html` | 公开 | 简历展示页，支持主题切换、PDF 导出 |
| 登录注册 | `/login.html` | 公开 | 支持用户名密码和第三方登录 |
| 简历数据维护 | `/resume-manage.html` | admin / super_admin | CRUD 管理 + 在线编辑器 |
| 系统设置 | `/system-settings.html` | admin / super_admin | 用户/角色/菜单管理 |
| AI 助手管理 | `/ai-manage.html` | admin / super_admin | 模型管理、费用统计 |
| AI 审计 | `/ai-audit.html` | admin / super_admin | 审计日志查看和统计 |

前端采用组件化架构，HTML 模板（`.template.js`）与 JS 逻辑（`.js`）分离，通过 `component-loader.js` 统一注册和初始化。

---

## 编译与运行

```bash
./mvnw clean compile                                # 编译
./mvnw spring-boot:run -pl brief-wisdom-web          # 运行
./mvnw clean package                                 # 打包
```

开发模式支持 DevTools 热重启、LiveReload、SQL 日志输出。

---

## 文档索引

### 新手入门

- [开发者快速入门](docs/guides/developer-guide.md) — 环境准备、快速启动、常见问题
- [AGENTS.md](AGENTS.md) — 编码规范
- [变更日志](CHANGELOG.md) — 修复历史与版本变更

### 架构设计

- [实时同步架构](docs/architecture/sync-architecture.md) — SSE / WebSocket 方案选型与实现
- [RAG 向量检索架构](docs/architecture/rag-architecture.md) — Redis Vector Store RAG 设计
- [跨会话记忆](docs/architecture/chat-memory.md) — ChatMemory 功能设计

### 功能特性

- [AI 润色功能](docs/features/ai-polish/) — 组件设计、API 参考、集成记录

### 开发指南

- [数据库表结构与配置](docs/guides/database-schema.md) — 表结构、MyBatis-Plus 配置、逻辑删除
- [日志配置](docs/guides/LOG_CONFIGURATION.md) — Logback 配置、日志分级
- [Markdown 导入指南](docs/guides/MARKDOWN_IMPORT_GUIDE.md) — Markdown 文件导入功能

### 项目分析

- [项目能力总结与规划](docs/analysis/README.md) — 能力评估、未来规划、AI 插件设计蓝图

---

## 扩展方向

### 短期（3-6 个月）

知识库增强（分块策略优化、重排序机制）、移动端 / PWA 适配、对话质量评估体系。

### 中期（6-12 个月）

团队协作（共享会话 / 团队知识库）、智能体工作流（可视化编排）、数据分析看板、语音交互、多模态支持。

### 长期（1-2 年）

企业级部署（K8s / 私有化）、自定义模型训练（LoRA）、开放平台 API、AI 安全审计增强。

---

## License

MIT License
