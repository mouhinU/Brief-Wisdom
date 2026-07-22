# 简历数据接入知识库方案

> 本文档描述将简历模块数据写入知识库、参与 RAG 向量检索的可行方案及对比分析。

---

## 一、当前架构概览

### 1.1 知识库模块

| 组件 | 说明 |
|------|------|
| `KnowledgeDocument` | 文档模型，支持 `INTERNAL`（富文本）、`FILE`（文件）、`LINK`（外部链接）三种类型 |
| `KnowledgeVectorService` | 向量化服务，将文档内容分块后存入 Redis Vector Store |
| `KnowledgeRagService` | RAG 检索服务，基于语义相似度从向量库中检索相关文档注入 AI 上下文 |
| `MarkdownImportService` | Markdown 文件批量导入，支持 upsert 去重 |
| `KnowledgeDocManagementTool` | AI 工具，允许 AI 助手直接创建/管理知识文档 |

### 1.2 简历模块

| 组件 | 说明 |
|------|------|
| `work_experience` | 工作经历表（title、job、description） |
| `project` | 项目表（name、lifecycle、background、duty） |
| `project_achievement` | 项目成果表（content） |
| `work_experience_stack` | 技术栈表（tech_name） |
| `ResumeManageService` | 简历 CRUD 管理服务 |
| `ResumeService` | 简历只读查询服务（含缓存） |
| `ResumeDataProvider` | 简历数据提供者接口，供 AI 工具模块调用 |
| `ResumeAnalysisTool` | AI 工具，分析简历内容给出优化建议 |

### 1.3 现状问题

- AI 可通过 `ResumeAnalysisTool` 直接查询简历数据，但**简历内容不参与知识库的向量检索（RAG）**
- 用户问"我之前做过什么项目"时，AI 需要主动调用工具，无法通过 RAG 自动命中
- 简历数据与知识库完全隔离，无法统一检索

---

## 二、方案设计

### 方案一：简历自动同步为知识库文档（推荐）

**思路**：在简历数据变更时，自动将简历内容序列化为 `KnowledgeDocument`（INTERNAL 类型），写入一个专门的「简历」知识库，并自动向量化。

**实现步骤**：

1. 创建一个固定的「简历知识库」（`KnowledgeBase`，name="我的简历"，可通过 Flyway 初始化）
2. 新增 `ResumeKnowledgeSyncService`，负责将简历数据序列化为结构化 Markdown
3. 在 `ResumeManageService` 的增删改方法中，通过 Spring 事件或直接调用触发同步
4. 同步逻辑调用 `KnowledgeService.upsertImportedMarkdown()` 写入知识库
5. 写入后自动触发 `KnowledgeVectorService.vectorizeAndStore()` 向量化

**序列化格式示例**：

```markdown
# 工作经历：XX公司 - 高级Java工程师

## 工作描述
负责核心业务系统的设计与开发...

## 技术栈
Spring Boot, MyBatis-Plus, Redis, MySQL, Spring AI

## 项目：智能客服系统
- **周期**：2024.01 - 2024.06
- **背景**：基于大模型的智能客服...
- **职责**：负责 RAG 检索模块设计...
- **成果**：
  - 检索准确率提升 40%
  - 响应时间降低至 200ms
```

**优点**：
- 改动最小，完全复用现有知识库 + 向量化 + RAG 链路
- 简历更新后自动同步，无需手动操作
- AI 对话中可通过 RAG 自动检索简历上下文
- 支持细粒度语义检索（如"我之前做的 RAG 项目"）

**缺点**：
- 简历数据存在两份（简历表 + 知识库文档），需要维护一致性
- 需要处理同步失败的重试机制

**涉及文件**：
- 新增：`brief-wisdom-resume/.../ResumeKnowledgeSyncService.java`
- 修改：`ResumeManageServiceImpl.java`（增删改后触发同步）
- 新增：Flyway 迁移脚本（初始化简历知识库）

---

### 方案二：简历数据直接注入 RAG 上下文

**思路**：不走知识库存储，在 RAG 检索时直接从简历模块拉取数据拼入 AI 上下文。

**实现步骤**：

1. 在 `KnowledgeRagService` 中注入 `ResumeDataProvider`
2. 新增简历相关性判断逻辑（关键词匹配或 Embedding 相似度）
3. 当用户问题涉及简历相关主题时，拉取简历数据拼入 system prompt

**优点**：
- 无数据冗余，始终使用最新简历数据
- 不需要额外的同步逻辑和存储

**缺点**：
- 每次对话都可能注入全量简历内容（浪费 token）
- 无法做细粒度的语义检索（如只检索某个项目的细节）
- 相关性判断逻辑难以精确，容易误触发或漏触发
- 与现有 RAG 架构不一致，增加维护复杂度

**涉及文件**：
- 修改：`KnowledgeRagService.java`
- 修改：`AiAgentService.java`（system prompt 构建逻辑）

---

### 方案三：新增 RESUME 文档类型 + 专用向量化管道

**思路**：扩展 `KnowledgeDocument` 的 `docType`，新增 `RESUME` 类型，`KnowledgeVectorService` 针对此类型有专用的文本提取和分块策略。

**实现步骤**：

1. `KnowledgeDocument.docType` 新增 `RESUME` 枚举值
2. 数据库 `knowledge_document` 表的 `doc_type` 字段支持新值
3. 新增 `ResumeKnowledgeSyncService`，监听简历变更事件
4. `KnowledgeVectorService.extractTextContent()` 增加 RESUME 类型处理分支
5. 可按维度分块：工作经历、项目经验、技术栈分别向量化
6. 前端知识库管理页面支持 RESUME 类型的展示

**优点**：
- 架构最清晰，简历作为一等公民参与知识库
- 可以对简历的不同维度分别向量化，检索精度最高
- 前端可针对性展示简历文档

**缺点**：
- 改动较大，涉及数据库 schema、向量化逻辑、前端展示
- 需要修改 `KnowledgeDocumentDTO` 的类型枚举和前端渲染
- 开发周期最长

**涉及文件**：
- 修改：`KnowledgeDocument.java`、`KnowledgeDocumentDTO.java`
- 修改：`KnowledgeVectorService.java`
- 新增：`ResumeKnowledgeSyncService.java`
- 修改：前端 `knowledge.js`（RESUME 类型渲染）
- 新增：Flyway 迁移脚本

---

### 方案四：手动导出简历为 Markdown → 导入知识库

**思路**：提供一个「导出简历到知识库」的按钮/接口，用户手动触发，将简历导出为 Markdown 格式导入知识库。

**实现步骤**：

1. 新增接口 `POST /api/resume/manage/export-to-knowledge`
2. 将简历数据序列化为 Markdown 文本
3. 调用 `KnowledgeService.upsertImportedMarkdown()` 写入指定知识库
4. 前端简历管理页面增加「同步到知识库」按钮

**优点**：
- 实现最简单，用户可控
- 不需要自动同步机制和事件监听

**缺点**：
- 简历更新后需要手动重新导出
- 容易遗忘同步，导致知识库中的简历过时
- 用户体验不佳

**涉及文件**：
- 新增：`ResumeExportController.java` 或在现有 Controller 中新增接口
- 修改：前端简历管理页面（增加按钮）

---

## 三、方案对比

| 维度 | 方案一（自动同步） | 方案二（RAG 注入） | 方案三（新文档类型） | 方案四（手动导出） |
|------|:-:|:-:|:-:|:-:|
| 改动量 | 小 | 中 | 大 | 最小 |
| 数据一致性 | 自动保证 | 始终最新 | 自动保证 | 需手动 |
| 语义检索精度 | 高 | 无 | 最高 | 高 |
| Token 消耗 | 按需检索 | 每次全量注入 | 按需检索 | 按需检索 |
| 可维护性 | 好 | 中 | 好 | 差 |
| 用户体验 | 无感知 | 无感知 | 无感知 | 需手动操作 |
| 开发周期 | 1-2 天 | 1 天 | 3-5 天 | 0.5 天 |

---

## 四、推荐方案

**推荐方案一（自动同步）**，理由：

1. **性价比最高**：改动小，完全复用现有知识库 + 向量化 + RAG 链路
2. **用户体验好**：简历更新后自动同步，用户无感知
3. **检索精度高**：简历内容被向量化后，AI 可通过语义检索精准命中
4. **架构一致**：不引入新的数据流路径，维护成本低

### 后续可扩展

- 如果检索精度不够，可升级为方案三（按维度分块向量化）
- 方案一和方案三不冲突，方案一是方案三的子集

---

## 五、方案一实现要点

### 5.1 同步触发时机

| 操作 | 触发 |
|------|------|
| 创建/更新/删除工作经历 | 全量重新生成简历文档 |
| 创建/更新/删除项目 | 全量重新生成简历文档 |
| 创建/更新/删除项目成果 | 全量重新生成简历文档 |
| 创建/更新/删除技术栈 | 全量重新生成简历文档 |

> 由于简历数据量小（通常 < 10 条工作经历），全量重新生成的开销可忽略。

### 5.2 去重策略

使用 `KnowledgeService.upsertImportedMarkdown()` 的 `sourcePath` 参数作为去重键：
- `sourcePath = "resume://work-experience/{id}"`（按工作经历拆分）
- 或 `sourcePath = "resume://full"`（整份简历一个文档）

### 5.3 知识库初始化

通过 Flyway 迁移脚本插入初始知识库记录：

```sql
INSERT INTO knowledge_base (name, description, sort_order, create_time, update_time)
VALUES ('我的简历', '简历数据自动同步，包含工作经历、项目经验、技术栈等', 0, NOW(), NOW());
```

### 5.4 注意事项

- 同步操作应异步执行，不阻塞简历 CRUD 主流程
- 同步失败仅记录日志，不影响简历操作
- 向量化失败不影响文档存储（现有机制已支持）
