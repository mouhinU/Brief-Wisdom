# Brief-Wisdom 系统分析与 Skill 建议

> **生成时间**: 2026-07-08  
> **项目版本**: 0.0.1-SNAPSHOT  
> **技术栈**: Spring Boot 3.5.7 + Spring AI 1.0.0 + Java 17 + MyBatis-Plus + Redis

---

## 一、系统现状概览

Brief-Wisdom 是一个**功能较完整的企业级 AI 对话平台**，技术栈为 Spring Boot 3.5.7 + Spring AI + MyBatis-Plus + Redis + 原生 JS 前端。

| 维度 | 现状 |
|------|------|
| **AI 能力** | 多模型、SSE 流式、RAG 知识库、页面上下文感知、文案润色 |
| **权限** | RBAC 三级角色 + `@RequiresPermission` + Spring Security 路由控制 |
| **实时同步** | SSE / WebSocket 可切换，多端会话同步 |
| **业务模块** | 简历 CRUD、知识库、费用统计、审计日志、OAuth 登录 |
| **安全加固** | 验证码 SecureRandom、XSS 防护、CORS 统一、密钥环境变量化（见 [修复历史及建议.md](../../修复历史及建议.md)） |
| **文档** | README 详尽、SpringDoc OpenAPI 已集成、AGENTS.md 编码规范 |
| **项目 Skill** | 已创建 4 个项目 Skill（见第五节） |

整体架构清晰，核心功能可用，但**工程化、安全细节、前端一致性**仍有明显改进空间。

### 核心优势

1. **完善的 AI 对话平台架构** — 多模型管理、流式输出、页面上下文感知、会话历史
2. **企业级权限体系** — RBAC 三级角色、树形菜单、细粒度 `@RequiresPermission`
3. **高性能缓存架构** — Redis 分布式缓存、Spring Session、Redisson 分布式锁
4. **实时同步机制** — SSE / WebSocket 可配置切换，多端设备同步
5. **前端性能优化** — 纯内存局部更新、组件化架构、无限滚动分页
6. **安全合规体系** — 三层防线（提示词 → 输入过滤 → 输出过滤）、Redis 限流
7. **丰富业务功能** — 简历管理、AI 润色、知识库、费用统计、审计日志、i18n

---

## 二、已实施的修复（参考）

详见 [修复历史及建议.md](../../修复历史及建议.md)，摘要如下：

| # | 修复项 | 说明 |
|---|--------|------|
| 1 | 短信验证码安全 | `SecureRandom` 替代 `Random`；日志仅输出前 2 位 |
| 2 | API 不返回验证码 | `sendSmsCode` 响应移除明文 code |
| 3 | OAuth Session key | 统一使用 `UserContextHelper.SESSION_USER_KEY` |
| 4 | 手机号注册冲突 | 用户名改为 `user_{手机号}_{UUID前6位}` |
| 5 | 配置安全 | API Key / DB / Redis 密码改为环境变量 |
| 6 | 前端 XSS | `DOMPurify.sanitize()` 过滤 Markdown 渲染 |

### 已修复的「紧急项」（原文档建议，现已落地）

- ✅ **CORS 统一配置** — `SecurityConfig` 集中管理，移除分散 `@CrossOrigin`
- ✅ **知识库权限** — `/api/knowledge/**` 需 `admin` / `super_admin`
- ✅ **N+1 查询** — `AiManageServiceImpl` 已改为批量聚合查询
- ✅ **限流升级** — `RateLimitService` 已使用 Redis 分布式限流（README 文档待同步）
- ✅ **SpringDoc OpenAPI** — 已集成，访问 `/swagger-ui.html`
- ✅ **BizException 体系** — 模块化异常 + `BizExceptionEnums` 错误码

---

## 三、待优化项（按优先级）

### 🔴 紧急 — 安全与稳定性

| 项 | 说明 | 状态 |
|----|------|------|
| **Markdown 路径穿越** | `SafePathUtils` + 白名单目录/文件校验 | ✅ 已修复 |
| **全局异常 HTTP 200** | 未知异常改为 HTTP 500 | ✅ 已修复 |
| **SSE 错误 JSON 拼接** | `ObjectMapper` 序列化 SSE 错误事件 | ✅ 已修复 |
| **Controller 直接收 Entity** | `ResumeManageController`、`AiModelController` 改用 Request DTO | ✅ 已修复 |
| **CSRF / CORS 生产配置** | 新增 `application-prod.yml` + `CORS_ALLOWED_ORIGINS` | ✅ 已修复 |

### 🟡 重要 — 工程质量

| 项 | 说明 | 状态 |
|----|------|------|
| **单元测试覆盖不足** | 新增 `AuthServiceImplTest` 等；Controller 集成测试与 70% 目标仍待推进 | 🟡 部分完成 |
| **无 CI/CD** | `.github/workflows/ci.yml`：compile + test | ✅ 已完成 |
| **无应用监控** | Actuator + Micrometer Prometheus；`/actuator/health` 公开，metrics 需管理员 | ✅ 已完成 |
| **数据库迁移** | Flyway `V1__baseline.sql`、`V2__add_performance_indexes.sql` | ✅ 已完成 |
| **数据库索引** | `chat_session.user_id`、`chat_message.session_id` 等性能索引 | ✅ 已完成 |
| **Docker 安全** | MySQL/Redis 绑定 `127.0.0.1`，`deploy.resources.limits`，默认 `prod` profile | ✅ 已完成 |
| **限流文档过时** | README 已更新为 Redis 分布式限流说明 | ✅ 已完成 |

### 🟢 改进 — 前端与体验

| 项 | 说明 | 状态 |
|----|------|------|
| **fetch 缺少 `resp.ok` 检查** | 统一 `api-request.js`；管理端组件已迁移 | ✅ 已完成 |
| **API 错误处理不统一** | 全局 `apiRequest()` 统一解析 Result 与错误信息 | ✅ 已完成 |
| **前端无 TypeScript** | 新增 `jsconfig.json` 改善 IDE 提示；完整 TS 迁移仍待规划 | 🟡 部分完成 |
| **OpenAPI 注解不完整** | 补全 `SseSyncController`、`WechatAuthController`、`OauthCallbackController`、`ResumeAiController` | ✅ 已完成 |

---

## 四、可新增功能（按路线图）

### 短期（3–6 个月）

| 功能 | 优先级 | 说明 |
|------|--------|------|
| **AI 插件/工具调用** | P1 | Function Calling：搜索、API、代码执行，扩展 Agent 能力 |
| **移动端 / PWA** | P2 | 响应式优化、离线缓存、安装到桌面 |
| **知识库增强** | P1 | Milvus 向量检索已有，可加强分块策略、重排序、对话 RAG 开关 |
| **API 文档完善** | P1 | SpringDoc 已有，补全注解 + 在线调试示例 |
| **`.env.example` 完善** | P1 | 已有模板，补充 Milvus、OAuth、CORS 等变量说明 |

### 中期（6–12 个月）

| 功能 | 说明 |
|------|------|
| **团队协作** | 共享会话、团队知识库、多租户隔离 |
| **智能体工作流** | 可视化编排多步 Agent（类似 LangGraph） |
| **数据分析看板** | 用户行为、热门问题、模型性能监控 |
| **语音交互** | Web Speech API / 阿里云语音 |
| **多模态** | 图片上传 + Qwen-VL / GPT-4V |

### 长期（1–2 年）

| 功能 | 说明 |
|------|------|
| **企业级部署** | K8s 容器化、高可用架构、私有化方案 |
| **自定义模型训练** | 领域数据微调（LoRA） |
| **开放平台** | API 网关、开发者生态 |
| **AI 安全审计** | 内容合规审查、操作追溯、风险预警 |

---

## 五、建议行动清单

```
本周
├── Markdown 路径校验 + SSE 错误序列化 ✅
├── 全局异常兜底改为 HTTP 500 ✅
└── 为 Resume/AiModel 等接口补充 Request DTO ✅

本月
├── 引入 Flyway + 补数据库索引 ✅
├── GitHub Actions：compile + test ✅
├── 集成 Actuator + Prometheus ✅
└── 统一前端 apiRequest + resp.ok 检查 ✅

本季度
├── 核心 Service 单元测试至 70%（进行中，已补 AuthService）
├── 完善项目 Skill（见下文）✅
└── AI 插件系统原型（Function Calling）
```

---

## 六、Cursor Skill 建议

### 6.1 已创建的项目 Skill

项目 Skill 位于 `.cursor/skills/`，Agent 在匹配相关任务时会自动加载。

| Skill | 路径 | 适用场景 |
|-------|------|----------|
| **brief-wisdom-java** | `.cursor/skills/brief-wisdom-java/` | Controller、Service、Repository、DTO、Maven 模块 |
| **brief-wisdom-frontend** | `.cursor/skills/brief-wisdom-frontend/` | `static/` 下 JS/HTML/CSS、组件、页面入口 |
| **brief-wisdom-ai** | `.cursor/skills/brief-wisdom-ai/` | AI 对话、知识库、RAG、流式输出、模型管理 |
| **brief-wisdom-security** | `.cursor/skills/brief-wisdom-security/` | 认证、权限、文件导入、API 暴露、安全审查 |

**使用方式**：

- 自动：编辑相关文件时 Agent 根据 `description` 自动匹配
- 手动：对话中说「按 brief-wisdom-java 规范实现 xxx」

### 6.2 推荐使用的 Cursor 内置 Skill

内置 Skill 位于 `~/.cursor/skills-cursor/`，通过 `/命令` 或 Agent 自动匹配触发。

#### 强烈推荐（与 Brief-Wisdom 高度匹配）

| Skill | 触发方式 | 适用场景 |
|-------|----------|----------|
| **create-rule** | 创建 `.cursor/rules/` | 将 `AGENTS.md` 拆成 Java/前端/安全等文件级规则 |
| **review-security** | `/review-security` | OAuth、Session、知识库、SMS 等安全审查 |
| **review-bugbot** | `/review-bugbot` | 功能开发后的逻辑/边界 Bug 审查 |
| **create-skill** | 创建项目 Skill | 扩展 brief-wisdom-* 系列 Skill |
| **split-to-prs** | 大改动拆分 PR | 多模块改动拆成可审查的小 PR |
| **babysit** | PR 合并前维护 | 处理 Review 评论、CI 失败、冲突 |

#### 按需使用

| Skill | 适用场景 |
|-------|----------|
| **create-hook** | Git pre-commit 跑测试、格式化 |
| **loop** | 定时跑 `./mvnw test`、监控 CI |
| **automate** | Cursor Automation：PR 创建后自动跑测试 |
| **canvas** | 费用统计、架构图、安全审计结果可视化 |
| **migrate-to-skills** | 旧 `.cursor/rules/*.mdc` 迁移为 Skill 格式 |
| **update-cursor-settings** | 统一 IDE 格式（4 空格、UTF-8 等） |
| **statusline** | CLI 显示当前分支、模块 |
| **sdk** | 用 Cursor SDK 做 CI/自动化 Agent |
| **onboard** | 新成员 `/onboard` 快速上手 Cursor |

### 6.3 建议后续补充的项目 Skill

| Skill | 说明 |
|-------|------|
| **brief-wisdom-testing** | 单元测试规范、Mock 策略、覆盖率目标 |
| **brief-wisdom-db** | Flyway 迁移、索引命名、SQL 脚本规范 |

### 6.4 Skill 与 Rules 的关系

| 类型 | 路径 | 作用 |
|------|------|------|
| **AGENTS.md** | 项目根目录 | 完整 Java 编码规范（权威来源） |
| **项目 Skill** | `.cursor/skills/` | 领域开发工作流 + 项目特有模式 |
| **Cursor Rules** | `.cursor/rules/*.mdc` | 文件级自动应用规则（建议用 create-rule 从 AGENTS.md 拆分） |

---

## 七、总结

**Brief-Wisdom 已具备生产可用的 AI 对话平台核心能力**，近期安全加固（验证码、XSS、CORS、知识库权限）和 N+1 优化已落地。

当前最紧迫的是：

1. **剩余安全项** — 路径穿越、异常 HTTP 状态码、Entity 直传
2. **工程化** — 测试、CI、Flyway、监控
3. **前端 API 层统一** — `apiRequest` + `resp.ok` 检查

Skill 方面，已创建 4 个项目 Skill；建议配合 **create-rule** 将 `AGENTS.md` 落地到 `.cursor/rules/`，并使用 **review-security** / **review-bugbot** 在改动后做审查。

---

## 附录：相关文档索引

| 文档 | 说明 |
|------|------|
| [README.md](../../README.md) | 项目概览、API、架构 |
| [AGENTS.md](../../AGENTS.md) | 编码规范 |
| [修复历史及建议.md](../../修复历史及建议.md) | 已修复项 + 待处理建议 |
| [项目能力分析与改进建议.md](./项目能力分析与改进建议.md) | 2026-07-05 能力评估 |
| [.cursor/skills/](../../.cursor/skills/) | 项目 Skill 目录 |

---

**最后更新**: 2026-07-08
