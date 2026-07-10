# AI 插件/工具调用（Function Calling）设计与实现建议

本文档为 `系统分析与Skill建议.md` 的补充，聚焦于在 Brief-Wisdom 中实现 AI 插件/工具调用能力（Function Calling / Tooling）。目标是提供一份可执行的设计蓝图，从 MVP 到生产化的迭代路线与风险控制。

## 一、目标与原则

- 目标：允许智能体在对话中安全、可审计地调用内部或外部工具（搜索、知识库检索、API、受控代码执行等），并将结果回填到对话流。
- 原则：最小权限、显式能力、可审计、隔离运行、速率与成本控制。

## 二、体系结构概览

- Plugin Registry：集中管理插件元数据（数据库表 `ai_plugin`），包括 manifest、权限、速率限制、启用标志。
- Invocation Broker：统一调用中介层（Spring Service），负责鉴权、参数校验、速率限流、审计记录、异常处理。
- Runtime Executors：针对不同调用类型（HTTP、内部 method、sandboxed code）实现不同 executor。
- Audit & Monitoring：调用日志、耗时、失败率、cost 统计。

## 三、Plugin Manifest（示例）

参见上面 `系统分析` 文档中的 JSON 示例。Manifest 必须声明输入/输出 schema、权限 scopes、调用 handler、速率限制等。

## 四、安全与合规

- 鉴权与授权：plugin scope 基于 RBAC，敏感写操作需管理员审批。
- 参数白名单：严格按照 manifest schema 接收字段。
- 输出脱敏：敏感字段默认脱敏，审计中仅保留脱敏/散列信息。
- 沙箱执行：代码执行类插件在隔离环境运行，限制网络访问与资源。
- 限流与配额：防止滥用外部 API 或资源浪费。

## 五、后端 API（建议）

- `POST /api/ai/plugins` — 上传/注册插件（管理员）
- `GET /api/ai/plugins` — 列表插件
- `POST /api/ai/plugins/{id}/invoke` — 直接调用（内部或前端触发）
- `POST /api/ai/session/{sessionId}/toolcall` — 智能体在会话中触发工具调用（建议）

## 六、开发者体验

- 插件以目录形式存在于 `.cursor/plugins/{pluginId}/`，包含 `manifest.json`、`SKILL.md`、`test/`。
- 提供本地模拟器 `ai-plugin-mock` 便于开发与自动化测试。

## 七、测试与验证

- 单元：manifest 校验器、参数校验、限流器。
- 集成：模拟插件 server 做端到端测试。
- 安全：注入测试、速率极限测试、敏感数据流测试。

## 八、迭代计划

- MVP（2 周）：只支持只读内部工具（KB 检索、公开 API）；manifest 存库，broker 调用；无沙箱执行。
- V2（1-2 个月）：加入用户确认模式、审计表、前端确认 UI。
- V3（3-6 个月）：支持沙箱代码执行、插件市场、签名与版本管理、成本计费。

## 九、风险与缓解

- 敏感数据泄露 → 脱敏 + 审计 + 审批。
- 未授权写操作 → RBAC + 审批流。
- 成本暴涨 → 速率/配额 + 成本报警。

---

更多细节可整合回 `系统分析与Skill建议.md` 的第 4 节或行动清单中，若需要我可以将此文件中的段落同步合并回主文档。
