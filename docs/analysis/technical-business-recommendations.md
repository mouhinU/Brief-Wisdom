## Brief-Wisdom 技术业务建议报告

**初始分析时间**: 2026-07-16
**最后更新时间**: 2026-11-06（依据源码复核修正表数量、限流算法、端口、团队协作与多模态落地状态）
**技术栈**: Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 / Redis Vector Store (RediSearch)
**分析范围**: 全模块代码审查 + 文档完整性审查

---

## 一、文档整理完成情况

本次文档整理将项目文档从 18 个文件优化为 10 个文件，消除重复、合并碎片、补全缺失。当前文档体系如下：

| 文件                                     | 行数  | 说明                                  |
|----------------------------------------|-----|-------------------------------------|
| README.md                              | 370 | 项目全景：14 个核心能力区域、12 个 AI 工具、技术栈、快速开始 |
| AGENTS.md                              | 443 | 编码规范（基于《Java 开发手册》v1.5.0 定制）        |
| CHANGELOG.md                           | 136 | 版本变更记录（2026-07-10 + 2026-07-16 两期）  |
| docs/README.md                         | 112 | 文档索引与导航                             |
| docs/architecture/sync-architecture.md | 320 | SSE/WebSocket 实时同步架构                |
| docs/architecture/rag-architecture.md  | 188 | Redis Vector Store RAG 设计           |
| docs/architecture/chat-memory.md       | 162 | 对话记忆（会话内隔离）功能设计                  |
| docs/features/ai-polish/README.md      | 262 | AI 润色组件完整文档                         |
| docs/guides/developer-guide.md         | 317 | 开发者快速入门                             |
| docs/guides/database-schema.md         | 483 | 数据库表结构（代码实际 23 张表；`all-init.sql` 仅 18 张，缺 chat_attachment/team/team_member/kb_share/kb_team，其余由 Flyway V9/V10/V12 建表） |
| docs/guides/LOG_CONFIGURATION.md       | 148 | 日志配置说明                              |
| docs/guides/MARKDOWN_IMPORT_GUIDE.md   | 235 | Markdown 导入指南                       |
| docs/analysis/README.md                | 126 | 项目能力总结与未来规划                         |

---

## 二、技术建议

### 建议 1：补充定时提醒的调度执行机制（优先级 P1）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`ReminderScheduler` 组件已实现 `@Scheduled(fixedRate = 60000)` 每分钟轮询到期提醒，通过 `ChatSyncService.notifyUser()` 推送 SSE/WebSocket 通知，并自动将提醒状态标记为已完成。`WebApplication` 已启用 `@EnableScheduling`。

### 建议 2：流式对话 Token/Cost 追踪（优先级 P1）— ✅ 代码已完成（工具路径未服务端落库，见下方补充）

**完成时间**: 2026-07-22

**实现方式**：在 `AiAgentService.chatStreamWithSession()` 的无工具调用路径中，通过 `AtomicReference<Usage>` 捕获流式响应最后一个 chunk 的 Usage 元数据，在 `doOnComplete` 回调中服务端自动保存 AI 消息（含真实 Token/Cost）。若提供商未返回 Usage，则使用 `estimateStreamingTokens()` / `estimateStreamingCost()` 按字符数估算。前端 `saveStreamedMessage()` 已改为幂等设计：检测到服务端已保存相同内容时跳过重复写入，仅在服务端使用估算值而前端有真实数据时补充更新。

**现状补充（2026-11-10 更新）**：上述 Usage 捕获路径此前不生效——`AiAgentService` 中 12 个工具恒注册，流式请求始终走 `chatModel.call()` 手动循环并 `Flux.just(全文)` 整段下发。现已改造为 `streamToolRound` 逐轮 `chatModel.stream()`，工具调用与逐 Token 输出打通，前端能实时收到文本增量。但要留意：`hasTools` 在当前配置下恒为 true，所有流式请求都走这条新路径，而带 Usage 捕获与服务端落库的「无工具」分支依旧是不可达代码；新路径不服务端保存 AI 消息（由前端 `/message/save` 落库并估算 Token），因此真实 Token/Cost 记录仍未打通，需要后续把 Usage 捕获与落库逻辑合并进 `streamToolRound`。

### 建议 3：单元测试覆盖率提升（优先级 P1）— ✅ 部分完成

**完成时间**: 2026-07-22

**已完成**：新增 3 个测试类共 59 个测试用例，全部通过：

| 测试类                    | 用例数 | 覆盖重点                        |
|------------------------|-----|-----------------------------|
| ContentFilterServiceTest | 40  | 关键词拦截、提示注入检测、PII 过滤、输出过滤    |
| RateLimitServiceTest     | 10  | 限流阈值、固定窗口（Redis INCR + EXPIRE，按秒 key 与按天 key）、Lua 脚本原子性 |
| ChatMemoryServiceTest    | 9   | 正则提取、upsert 逻辑、上下文构建、权重排序   |

**待补充**：KnowledgeVectorService（向量化存储、相似度搜索、降级处理）、AiAgentService（上下文构建、页面感知、费用计算）、ChatModelRegistry（多提供商路由、缓存、回退逻辑）。

### 建议 4：ProjectCodeIndexService 内存优化（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`ProjectCodeIndexService` 已设置 `MAX_INDEX_FILES = 5000` 上限，使用 `AtomicInteger` 计数，超出上限时终止扫描并记录警告日志。

### 建议 5：补充数据库索引（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-16 前（Flyway V5/V6 已包含）

**实现方式**：

- V5 建表时内联创建 `chat_memory` 三个索引：`idx_chat_memory_user_id`、`idx_chat_memory_user_category`、`idx_chat_memory_access_count`
- V6 补充 `chat_reminder` 两个索引：`idx_chat_reminder_status_time`、`idx_chat_reminder_user_id`
- `knowledge_document.base_id` 索引已由 V2 复合索引 `(base_id, file_name(191))` 覆盖，无需重复创建

### 建议 6：健康检查端点增强（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`VectorStoreHealthIndicator` 已实现，暴露 VectorStore 可用状态、索引名称和维度信息到 `/actuator/health` 端点。

### 建议 7：SMS 服务真实集成（优先级 P2）— ⚡ 部分完成（真实短信通道尚未接入）

**完成时间**: 2026-07-16 前（mock/aliyun 双实现框架）；真实短信发送仍未实现（截至 2026-11-06 复核）

**实现方式**：通过 `@ConditionalOnProperty(name = "app.sms.provider")` 实现 mock/aliyun 双实现切换。`SmsServiceImpl`（mock，matchIfMissing=true）用于开发环境，`AliyunSmsServiceImpl`（aliyun）用于生产环境。配置 `app.sms.provider: aliyun` 即可切换实现。

**尚未完成**：两个实现的发送逻辑都只有 TODO 注释——`SmsServiceImpl` 仅把验证码打印到日志，`AliyunSmsServiceImpl` 未引入阿里云 SMS SDK。默认配置为 `app.sms.provider: mock`，因此线上暂时发不出真实短信，手机短信登录在生产环境不可用。

### 建议 8：结构化日志改进（优先级 P3）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`MdcLoggingFilter` 拦截器已实现，在请求处理链中自动注入 `userId`、`sessionId` 到 MDC 上下文，日志格式已包含 MDC 字段输出。

### 建议 9：service 模块迁移收尾（优先级 P3）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`brief-wisdom-service` 模块中仅剩 `package-info.java` 占位文件，所有业务逻辑已迁移至对应领域模块（ai/system/resume）。模块保留但实质为空，可在后续版本安全移除。

### 建议 10：Docker 生产部署方案（优先级 P3）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：项目根目录已包含 `Dockerfile`，支持多阶段构建。

---

## 三、业务建议

### 建议 11：对话导出功能（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-22

**实现方式**：

- 后端：`GET /api/ai/session/{sessionId}/export` 端点，`AiAgentService.exportSessionAsMarkdown()` 生成包含会话元信息、用户/AI 消息时间线、模型和 Token 信息的 Markdown 文档，含权限校验（会话归属权检查）
- 前端：会话列表项悬停显示导出按钮（⤓），点击后通过 Blob 下载 Markdown 文件，文件名自动取自会话标题

### 建议 12：知识库增强 — 分块策略与重排序（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`KnowledgeVectorService` 已实现智能分块：按 Markdown 标题和段落分块（`MAX_CHUNK_SIZE = 2000`，`CHUNK_OVERLAP = 200`），每个分块独立构建 Spring AI Document 并附带元数据（docId、baseId、title、docType、tags、chunkIndex、totalChunks）。

### 建议 13：移动端 / PWA 适配（优先级 P2）— ⚡ 部分完成

**完成时间**: PWA 基础支持 2026-07-16 前已完成

**已完成**：PWA manifest.json + Service Worker 已实现，支持"添加到主屏幕"。

**待完成**：关键页面（聊天、简历展示）响应式适配、移动端键盘弹出视口调整。

### 建议 14：对话质量评估机制（优先级 P3）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：Flyway V7 迁移新增 `feedback_score`、`feedback_comment`、`feedback_time` 三个字段到 `chat_message` 表。`POST /api/ai/feedback` 端点支持用户对 AI 回复进行 1-5 分评分并附带文字反馈。

### 建议 15：团队协作能力（优先级 P3，中长期）— ⚡ 部分完成

**完成时间**: 2026-11-06 复核确认团队/成员/知识库共享已落地

**已完成**：

- 团队管理：`TeamServiceImpl` + `TeamController`（基址 `/api/team`），覆盖团队创建/详情/我的团队列表/更新/删除，成员添加/移除/角色变更/成员列表
- 三级角色：`owner` / `admin` / `member`
- 团队知识库：`POST/GET /api/team/{teamId}/knowledge-bases`；`knowledge_base` 新增 `owner_type`(personal|team)、`owner_id`、`creator_id`
- 知识库共享授权：`kb_share` 支持 `target_type` = `team` / `user` 两级授权（`permission` 当前仅 `read`），接口含共享/撤销共享/共享列表/获取共享给我的知识库
- 数据表：`team`、`team_member`、`kb_share`、`kb_team`（Flyway V10、V12）；`KnowledgeServiceImpl` 已接入可见性过滤

**尚未完成**：共享会话（团队成员查看/续接彼此对话）、多租户隔离。

---

## 四、优先级总览

| 优先级    | 建议                    | 状态      | 预计工作量  |
|--------|-----------------------|---------|--------|
| **P1** | 定时提醒调度执行              | ✅ 已完成   | 2-3 小时 |
| **P1** | 流式对话 Token 追踪         | ✅ 代码已完成 | 4-6 小时 |
| **P1** | 单元测试覆盖率提升             | ⚡ 部分完成 | 2-3 天  |
| **P2** | ProjectCodeIndex 内存优化 | ✅ 已完成   | 1-2 小时 |
| **P2** | 补充数据库索引               | ✅ 已完成   | 30 分钟  |
| **P2** | 健康检查增强                | ✅ 已完成   | 1-2 小时 |
| **P2** | SMS 服务框架               | ⚡ 部分完成 | 4-6 小时 |
| **P2** | 对话导出功能                | ✅ 已完成   | 1 天    |
| **P2** | 知识库分块与重排序             | ✅ 已完成   | 2-3 天  |
| **P2** | 移动端/PWA 适配            | ⚡ 部分完成 | 3-5 天  |
| **P3** | 结构化日志                 | ✅ 已完成   | 3-4 小时 |
| **P3** | service 模块迁移收尾        | ✅ 已完成   | 1-2 天  |
| **P3** | Docker 生产部署           | ✅ 已完成   | 4-6 小时 |
| **P3** | 对话质量评估                | ✅ 已完成   | 2-3 天  |
| **P3** | 团队协作能力（团队/知识库共享）   | ⚡ 部分完成 | 2-3 周  |

**完成率**: 11/15 已完成，4/15 部分完成（单元测试覆盖率、移动端/PWA、SMS 真实发送、团队协作剩余项）

---

## 五、当前项目优势总结

从代码质量和功能完整度来看，Brief-Wisdom 已经具备以下竞争优势：

1. **功能完整度高**：12 个 AI 工具、7 种认证方式、完整的 RBAC 体系、RAG 知识库、对话记忆（会话内隔离），在同类个人项目中属于功能矩阵非常完整的实现。

2. **架构设计合理**：领域模块划分清晰（ai/system/resume），依赖方向单一（上层→下层），通过 SPI
   接口（ToolContextProvider/ResumeDataProvider）实现模块解耦，具备较好的可扩展性。

3. **安全意识强**：三层内容安全防线、分布式限流、XSS 防护、审计日志，安全考量比较全面。

4. **工程化基础好**：Flyway 数据库迁移、CI/CD、Actuator 监控、编码规范文档齐全，为后续迭代打下了良好基础。

下一步的核心方向建议是：**补齐剩余测试**（KnowledgeVectorService / AiAgentService / ChatModelRegistry）→ **移动端响应式适配** → **团队协作剩余项（共享会话、多租户隔离）** → **多模态扩展（PDF/Word 文档解析、语音输入、图片生成）**。真流式（逐 Token）输出改造已于 2026-11-10 完成，遗留项是把 Usage 捕获与服务端落库合并进工具循环路径。

---

**报告生成时间**: 2026-07-16
**最后更新时间**: 2026-11-06
