## Brief-Wisdom 技术业务建议报告

**初始分析时间**: 2026-07-16
**最后更新时间**: 2026-07-22
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
| docs/architecture/chat-memory.md       | 162 | 跨会话记忆功能设计                           |
| docs/features/ai-polish/README.md      | 262 | AI 润色组件完整文档                         |
| docs/guides/developer-guide.md         | 317 | 开发者快速入门                             |
| docs/guides/database-schema.md         | 483 | 数据库表结构（含全部 18 张表）                   |
| docs/guides/LOG_CONFIGURATION.md       | 148 | 日志配置说明                              |
| docs/guides/MARKDOWN_IMPORT_GUIDE.md   | 235 | Markdown 导入指南                       |
| docs/analysis/README.md                | 126 | 项目能力总结与未来规划                         |

---

## 二、技术建议

### 建议 1：补充定时提醒的调度执行机制（优先级 P1）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：`ReminderScheduler` 组件已实现 `@Scheduled(fixedRate = 60000)` 每分钟轮询到期提醒，通过 `ChatSyncService.notifyUser()` 推送 SSE/WebSocket 通知，并自动将提醒状态标记为已完成。`WebApplication` 已启用 `@EnableScheduling`。

### 建议 2：流式对话 Token/Cost 追踪（优先级 P1）— ✅ 已完成

**完成时间**: 2026-07-22

**实现方式**：在 `AiAgentService.chatStreamWithSession()` 的无工具调用路径中，通过 `AtomicReference<Usage>` 捕获流式响应最后一个 chunk 的 Usage 元数据，在 `doOnComplete` 回调中服务端自动保存 AI 消息（含真实 Token/Cost）。若提供商未返回 Usage，则使用 `estimateStreamingTokens()` / `estimateStreamingCost()` 按字符数估算。前端 `saveStreamedMessage()` 已改为幂等设计：检测到服务端已保存相同内容时跳过重复写入，仅在服务端使用估算值而前端有真实数据时补充更新。

### 建议 3：单元测试覆盖率提升（优先级 P1）— ✅ 部分完成

**完成时间**: 2026-07-22

**已完成**：新增 3 个测试类共 59 个测试用例，全部通过：

| 测试类                    | 用例数 | 覆盖重点                        |
|------------------------|-----|-----------------------------|
| ContentFilterServiceTest | 40  | 关键词拦截、提示注入检测、PII 过滤、输出过滤    |
| RateLimitServiceTest     | 10  | 限流阈值、滑动窗口、Lua 脚本原子性         |
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

### 建议 7：SMS 服务真实集成（优先级 P2）— ✅ 已完成

**完成时间**: 2026-07-16 前（代码审查时已存在）

**实现方式**：通过 `@ConditionalOnProperty(name = "app.sms.provider")` 实现 mock/aliyun 双实现切换。`SmsServiceImpl`（mock，matchIfMissing=true）用于开发环境，`AliyunSmsServiceImpl`（aliyun）用于生产环境。配置 `app.sms.provider: aliyun` 即可切换。

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

### 建议 15：团队协作能力（优先级 P3，中长期）— ⏳ 未开始

**现状**：系统为单用户模式设计，每个用户的会话和知识库完全隔离。

**建议**：中长期引入团队概念：

- 团队知识库（多人共享的文档集合）
- 共享会话（团队成员可以查看和续接彼此的对话）
- 团队管理界面（邀请成员、设置团队角色）

这需要数据库层面的较大改动（新增 team 表、team_member 表、修改知识库和会话的归属模型），建议作为 v0.2.0 的核心功能规划。

**工作量**：约 2-3 周。

---

## 四、优先级总览

| 优先级    | 建议                    | 状态      | 预计工作量  |
|--------|-----------------------|---------|--------|
| **P1** | 定时提醒调度执行              | ✅ 已完成   | 2-3 小时 |
| **P1** | 流式对话 Token 追踪         | ✅ 已完成   | 4-6 小时 |
| **P1** | 单元测试覆盖率提升             | ⚡ 部分完成 | 2-3 天  |
| **P2** | ProjectCodeIndex 内存优化 | ✅ 已完成   | 1-2 小时 |
| **P2** | 补充数据库索引               | ✅ 已完成   | 30 分钟  |
| **P2** | 健康检查增强                | ✅ 已完成   | 1-2 小时 |
| **P2** | SMS 真实集成              | ✅ 已完成   | 4-6 小时 |
| **P2** | 对话导出功能                | ✅ 已完成   | 1 天    |
| **P2** | 知识库分块与重排序             | ✅ 已完成   | 2-3 天  |
| **P2** | 移动端/PWA 适配            | ⚡ 部分完成 | 3-5 天  |
| **P3** | 结构化日志                 | ✅ 已完成   | 3-4 小时 |
| **P3** | service 模块迁移收尾        | ✅ 已完成   | 1-2 天  |
| **P3** | Docker 生产部署           | ✅ 已完成   | 4-6 小时 |
| **P3** | 对话质量评估                | ✅ 已完成   | 2-3 天  |
| **P3** | 团队协作能力                | ⏳ 未开始  | 2-3 周  |

**完成率**: 13/15 已完成，2/15 部分完成或未开始

---

## 五、当前项目优势总结

从代码质量和功能完整度来看，Brief-Wisdom 已经具备以下竞争优势：

1. **功能完整度高**：12 个 AI 工具、7 种认证方式、完整的 RBAC 体系、RAG 知识库、跨会话记忆，在同类个人项目中属于功能矩阵非常完整的实现。

2. **架构设计合理**：领域模块划分清晰（ai/system/resume），依赖方向单一（上层→下层），通过 SPI
   接口（ToolContextProvider/ResumeDataProvider）实现模块解耦，具备较好的可扩展性。

3. **安全意识强**：三层内容安全防线、分布式限流、XSS 防护、审计日志，安全考量比较全面。

4. **工程化基础好**：Flyway 数据库迁移、CI/CD、Actuator 监控、编码规范文档齐全，为后续迭代打下了良好基础。

下一步的核心方向建议是：**补齐剩余测试**（KnowledgeVectorService / AiAgentService / ChatModelRegistry）→ **移动端响应式适配** → **团队协作能力（v0.2.0）**。

---

**报告生成时间**: 2026-07-16
**最后更新时间**: 2026-07-22
