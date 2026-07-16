# Brief-Wisdom 项目分析

> 本文件归档了项目的系统分析与能力评估，保留有价值的能力总结和未来规划。

**技术栈**：Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 / Redis Vector Store (RediSearch)

---

## 一、项目能力总结

Brief-Wisdom 是一个**功能较完整的企业级 AI 对话平台**，架构清晰、功能完善。

### 核心优势

1. **完善的 AI 对话平台架构** — 多模型管理（DashScope/DeepSeek/OpenAI/Anthropic）、流式输出（SSE 打字机效果）、页面上下文感知、会话历史管理、跨会话记忆（ChatMemory）
2. **企业级权限体系** — RBAC 三级角色（super_admin/admin/normal）、树形菜单（目录→菜单→按钮）、`@RequiresPermission` 细粒度权限控制、动态菜单加载
3. **高性能缓存架构** — Redis 分布式缓存（Spring Cache + 自定义 TTL）、Spring Session 持久化（`bw:session` 命名空间）、Redisson 分布式锁（避免 pExpire 递归冲突）
4. **实时同步机制** — SSE / WebSocket 可配置切换，多端设备同时在线同步，事件推送覆盖所有设备
5. **RAG 知识库** — Redis Vector Store (RediSearch) 向量化存储、语义检索、LazyVectorStore 优雅降级、AI 可调用的 KnowledgeSearchTool
6. **前端性能优化** — 纯内存缓存局部更新（零网络请求）、精准 DOM 更新、组件化架构、无限滚动分页
7. **安全合规体系** — 三层防线（提示词→输入过滤→输出过滤）、Redis 分布式限流、XSS 防护、CORS 统一配置
8. **丰富业务功能** — 简历管理、AI 文案润色、知识库、费用统计、审计日志、i18n 国际化、AI 工具调用（12 个 Tool）

### 已完成的工程化建设

| 项目 | 状态 | 说明 |
|------|------|------|
| CORS 统一配置 | ✅ | SecurityConfig 集中管理 |
| 知识库权限控制 | ✅ | /api/knowledge/** 需 admin/super_admin |
| N+1 查询修复 | ✅ | AiManageServiceImpl 批量聚合查询 |
| SpringDoc OpenAPI | ✅ | 已集成，访问 /swagger-ui.html |
| BizException 体系 | ✅ | 模块化异常 + BizExceptionEnums 错误码 |
| Flyway 数据库迁移 | ✅ | V1__baseline.sql + V2__add_performance_indexes.sql |
| SQL 性能索引 | ✅ | chat_session.user_id、chat_message.session_id 等 |
| CI/CD | ✅ | .github/workflows/ci.yml 自动 compile + test |
| Actuator + Prometheus | ✅ | 健康检查与 metrics 端点已接入 |
| Docker 安全 | ✅ | MySQL/Redis 绑定 127.0.0.1，资源限制 |
| 统一 apiRequest | ✅ | 管理端组件已迁移 |
| OpenAPI 注解补全 | ✅ | 所有 Controller 已补全 |

---

## 二、项目 Skill

项目 Skill 位于 `.cursor/skills/`，Agent 在匹配相关任务时会自动加载。

| Skill | 路径 | 适用场景 |
|-------|------|---------|
| brief-wisdom-java | `.cursor/skills/brief-wisdom-java/` | Controller、Service、Repository、DTO、Maven 模块 |
| brief-wisdom-frontend | `.cursor/skills/brief-wisdom-frontend/` | static/ 下 JS/HTML/CSS、组件、页面入口 |
| brief-wisdom-ai | `.cursor/skills/brief-wisdom-ai/` | AI 对话、知识库、RAG、流式输出、模型管理 |
| brief-wisdom-security | `.cursor/skills/brief-wisdom-security/` | 认证、权限、文件导入、API 暴露、安全审查 |

---

## 三、未来规划

### 短期（3-6 个月）

| 方向 | 优先级 | 说明 |
|------|--------|------|
| AI 插件/工具调用 | P1 | Function Calling：搜索、API、知识库、受控执行，扩展 Agent 能力 |
| 知识库增强 | P1 | 加强分块策略、重排序、对话 RAG 开关 |
| 移动端 / PWA | P2 | 响应式优化、离线缓存、安装到桌面 |
| API 文档完善 | P1 | SpringDoc 已有，补全注解 + 在线调试示例 |

### 中期（6-12 个月）

| 方向 | 说明 |
|------|------|
| 团队协作 | 共享会话、团队知识库、多租户隔离 |
| 智能体工作流 | 可视化编排多步 Agent（类似 LangGraph） |
| 数据分析看板 | 用户行为、热门问题、模型性能监控 |
| 语音交互 | Web Speech API / 阿里云语音 |
| 多模态 | 图片上传 + Qwen-VL / GPT-4V |

### 长期（1-2 年）

| 方向 | 说明 |
|------|------|
| 企业级部署 | K8s 容器化、高可用架构、私有化方案 |
| 自定义模型训练 | 领域数据微调（LoRA） |
| 开放平台 | API 网关、开发者生态 |
| AI 安全审计 | 内容合规审查、操作追溯、风险预警 |

---

## 四、AI 插件/工具调用设计蓝图

> 以下为 Function Calling 能力的初步设计，尚未实施，作为未来规划保留。

### 体系结构概览

- **Plugin Registry**：集中管理插件元数据（数据库表 `ai_plugin`），包括 manifest、权限、速率限制、启用标志
- **Invocation Broker**：统一调用中介层，负责鉴权、参数校验、速率限流、审计记录、异常处理
- **Runtime Executors**：针对不同调用类型（HTTP、内部 method、sandboxed code）实现不同 executor
- **Audit & Monitoring**：调用日志、耗时、失败率、cost 统计

### 安全与合规

- 鉴权与授权：plugin scope 基于 RBAC，敏感写操作需管理员审批
- 参数白名单：严格按照 manifest schema 接收字段
- 输出脱敏：敏感字段默认脱敏
- 沙箱执行：代码执行类插件在隔离环境运行
- 限流与配额：防止滥用外部 API 或资源浪费

### 迭代路线

- **MVP（2 周）**：只支持只读内部工具（KB 检索、公开 API）；manifest 存库，broker 调用
- **V2（1-2 个月）**：加入用户确认模式、审计表、前端确认 UI
- **V3（3-6 个月）**：支持沙箱代码执行、插件市场、签名与版本管理、成本计费

---

## 五、技术债务清单

| 类别 | 问题描述 | 影响范围 | 修复难度 |
|------|---------|---------|---------|
| 测试 | 单元测试覆盖率不足 | 全项目 | 中 |
| 前端 | 无 TypeScript 类型安全 | 前端开发 | 高 |
| 代码 | 部分方法超过 80 行 | 可维护性 | 低 |

---

**原始分析文档生成时间**: 2026-07-05 至 2026-07-08
**归档整理时间**: 2026-07-15
