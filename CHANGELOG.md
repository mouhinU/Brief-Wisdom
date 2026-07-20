# Brief-Wisdom 变更日志

> 本文件记录项目的修复历史与版本变更。格式基于 [Keep a Changelog](https://keepachangelog.com/)。

---

## [0.0.1-SNAPSHOT] - 2026-07-16

### 新增

**Spring AI 2.0 / Java 21 / Spring Boot 4.0.7 升级：**

- 从 Spring Boot 3.5.7 / Spring AI 1.0.0 / Java 17 升级至 Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21
- 启用 Java 21 虚拟线程（线程池命名 bw-vt-*）
- Jackson 2.x 兼容 Bean（Spring Boot 4 默认使用 Jackson 3.x）

**Redis Vector Store RAG：**

- Redis Vector Store（RediSearch）集成，用于知识库语义搜索
- LazyVectorStore 优雅降级（无 RediSearch 时仍可正常工作）
- NoOpEmbeddingModel 回退（未配置 AI 提供商时）
- Embedding：text-embedding-v3，1024 维，索引：brief-wisdom-knowledge
- 通过 CompletableFuture + briefWisdomExecutor 实现异步向量化
- 全量重建索引能力（KnowledgeVectorService.reindexAll）
- Markdown 批量导入（MarkdownImportService）
- 项目代码索引服务（ProjectCodeIndexService），为 RAG 提供代码上下文

**跨会话记忆（ChatMemory）：**

- ChatMemory 实体 + Repository + Service
- 正则自动提取：姓名、公司、角色、技术栈、风格、当前项目
- 3 个分类：preference（偏好）、fact（事实）、context（上下文）
- 每次对话时将记忆上下文注入系统提示词
- MemoryManagementTool 支持 AI 主动管理记忆

**AI 工具调用系统（Function Calling）：**

- 12 个工具通过 @Tool 注解 + MethodToolCallbackProvider（ToolConfig）注册
- 第一层（知识与代码）：KnowledgeSearchTool、CodeSearchTool
- 第二层（数据与分析）：ResumeAnalysisTool、SystemStatusTool、DatabaseQueryTool、DateTimeTool、CalculatorTool
- 第三层（集成）：MemoryManagementTool、WebFetchTool、TranslationTool、KnowledgeDocManagementTool、ReminderTool
- ToolContextProvider 接口（common 模块）实现依赖隔离
- ResumeDataProvider 接口（common 模块）实现依赖隔离

**认证：**

- 手机号 + 短信验证码登录（Redis 存储，5 分钟过期，自动注册）
- SSO Token 登录（HMAC-SHA256 签名，Redis 存储，可配置过期时间）

**AI 审计系统：**

- AiAuditService：输入拦截、输出过滤、风险检测日志记录
- 按类型和风险等级统计审计数据
- AiAuditController 支持分页查询和过滤

**内容安全增强：**

- Prompt 注入检测：10 种正则模式 + 启发式规则（文本泛滥、混合语言、嵌套指令）
- 输出 PII 过滤：身份证号、手机号、银行卡号
- ContentSecurityException 异常类，附带审计日志记录

**其他：**

- 流式对话支持终止功能（DELETE /chat/stream/stop）
- 对话提醒功能（chat_reminder 表，ReminderTool）
- Flyway V3：knowledge_document.link_desc VARCHAR→TEXT
- Flyway V4：chat_reminder 表创建
- 分布式限流：20 请求/秒 + 1,440,000 请求/天（Lua 脚本）
- ResultAutoWrapperAdvice 自动响应包装

### 变更

- 模块依赖隔离：brief-wisdom-ai 不再依赖 brief-wisdom-system 和 brief-wisdom-resume，改用 common
  模块中的接口（ToolContextProvider、ResumeDataProvider）
- 线程池：迁移至 Java 21 虚拟线程（ThreadPoolConfig）
- AiAgentService：构建 ChatOptions 时集成 ToolCallbacks 以支持函数调用

### 文档

- 文档全面重组（18 个文件 → 14 个文件）
- 新增文档：RAG 架构设计、ChatMemory 设计、数据库 Schema 指南
- 合并分析文档、AI 润色文档、修复记录为整合文件
- 所有文档更新为 Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21

---

## [0.0.1-SNAPSHOT] - 2026-07-10

### 安全修复

- **短信验证码安全加固** (`SmsServiceImpl.java`)：`java.util.Random` 替换为 `java.security.SecureRandom`
  ，防止验证码被预测；日志中验证码仅输出前 2 位（`code=13****`），避免日志泄露完整验证码
- **API 响应不再返回验证码明文** (`AuthController.java`)：`sendSmsCode` 接口响应中移除 `"code"` 字段，此前攻击者可直接从
  HTTP 响应中读取验证码，完全绕过短信验证
- **OAuth 登录 Session key 修复** (`OauthCallbackController.java`)：OAuth 回调原使用 `"AUTH_USER"` 作为 session key，而
  `UserContextHelper` 和其他 Controller 使用 `"SESSION_USER"`，导致钉钉/支付宝登录后 `getCurrentUserId()` 返回
  null，权限校验全部失效。修复为引用 `UserContextHelper.SESSION_USER_KEY` 常量
- **手机号自动注册用户名冲突** (`AuthServiceImpl.java`)：原用手机号后 4 位生成用户名（`user_1234`），不同手机号后 4
  位相同时会冲突。改为 `user_{手机号}_{UUID前6位}`，确保唯一性
- **配置安全加固** (`application.yml` + `application-dev.yml`)：DashScope / DeepSeek API 密钥从硬编码明文改为
  `${DASHSCOPE_API_KEY}` 环境变量；数据库密码改为 `${DB_PASSWORD}`，Redis 密码改为 `${REDIS_PASSWORD}`；DevTools、SQL
  日志等开发配置从 base config 移至 `application-dev.yml`；`profiles.include: dev` 改为
  `profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`
- **前端 XSS 防护** (`chat.js` + `navbar.js`)：引入 DOMPurify 库，所有 `marked.parse()` 输出经 `DOMPurify.sanitize()`
  过滤后再渲染到 DOM，防止 AI 回复中的恶意脚本执行；新增 `renderMarkdown()` 统一渲染函数，5 处 `innerHTML` 赋值全部替换
- **CORS 统一配置**：`SecurityConfig` 集中管理，移除分散 `@CrossOrigin`；新增 `application-prod.yml` 与
  `CORS_ALLOWED_ORIGINS` 环境变量
- **知识库接口权限控制**：`SecurityConfig` 限制 `/api/knowledge/**` 为 `admin` / `super_admin`
- **Markdown 路径穿越风险**：新增 `SafePathUtils`，`import-allowed-dirs/files` 白名单校验
- **全局异常 HTTP 状态码**：未知异常由 HTTP 200 改为 HTTP 500
- **SSE 错误序列化**：`AiAgentController` 改用 `ObjectMapper` 序列化 SSE 错误事件
- **Controller DTO 改造**：`ResumeManageController`、`AiModelController` 改用 Request DTO，不再直接接收持久化实体

### 性能优化

- **N+1 查询修复**：`AiManageServiceImpl` 改为批量聚合查询
- **SQL 索引补充**：Flyway `V2__add_performance_indexes.sql` 补充 `chat_session.user_id`、`chat_message.session_id` 等性能索引

### 基础设施

- **数据库迁移**：Flyway `V1__baseline.sql` + `V2__add_performance_indexes.sql`，版本化数据库变更
- **CI/CD**：`.github/workflows/ci.yml` 自动 compile + test
- **应用监控**：Actuator + Micrometer Prometheus 端点已接入；`/actuator/health` 公开，metrics 需管理员
- **Docker 安全**：MySQL/Redis 绑定 `127.0.0.1`，增加 `deploy.resources.limits`，默认 `prod` profile
- **统一 apiRequest**：新增 `static/js/api-request.js`，管理端组件（用户/知识库/简历等）已迁移
- **OpenAPI 注解补全**：`SseSyncController`、`WechatAuthController`、`OauthCallbackController`、`ResumeAiController`
- **前端 IDE 支持**：新增 `jsconfig.json`
- **单元测试**：新增 `AuthServiceImplTest`（9 个用例）

### 分布式锁修复（StackOverflowError）

**问题**：应用启动后登录时抛出 `java.lang.StackOverflowError`，堆栈显示 `DefaultedRedisConnection.pExpire()` 无限递归。

**根本原因**：项目使用 `redisson-spring-boot-starter` 3.40.2，该 starter 自动注册 `RedissonConnectionFactory` 作为 Spring
Boot 的 `RedisConnectionFactory`。`RedissonConnectionFactory` 创建的连接对象继承自 `DefaultedRedisConnection`，该接口的
`pExpire()` 默认方法存在无限递归 bug（spring-data-redis 3.5.5）。Spring Session Redis 在设置会话 TTL 时调用 `pExpire()`
，触发无限递归。

**解决方案**：让 Lettuce 处理 Redis 连接（用于 Session、Cache、RedisTemplate），Redisson 仅用于分布式锁。

1. pom.xml 替换依赖：`redisson-spring-boot-starter` → 独立 `redisson` 核心包
2. 新增 `RedissonConfig.java`：手动配置 `RedissonClient`
3. 职责分离：`LettuceConnectionFactory` 处理常规 Redis 操作，Redisson 独立运行

**修复效果**：Spring Boot 自动使用 Lettuce 正确实现 `pExpire()`；分布式锁不受影响；`DistributedLockService` 和
`DistributedLockAspect` 无需修改。

---

## 待处理建议

### 改进项（核心已完成）

- 完整 TypeScript 迁移仍待规划
- 单元测试 70% 覆盖率目标仍在推进中

---

**最后更新**: 2026-07-16
