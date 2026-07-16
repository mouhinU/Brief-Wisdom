# RAG 向量检索架构

> Brief-Wisdom 基于 Redis Vector Store (RediSearch) 实现知识库文档的向量化存储与语义检索，为 AI 对话提供 RAG（Retrieval-Augmented Generation）增强能力。

**技术栈**：Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 / Redis Vector Store (RediSearch)

---

## 架构概览

RAG 系统横跨三个模块：`brief-wisdom-web`（配置层）、`brief-wisdom-ai`（服务层）、`brief-wisdom-persistence`（数据层）。整体采用"懒加载 + 优雅降级"设计，即使 Redis 未安装 RediSearch 模块，系统也能正常启动和运行，只是向量检索功能不可用。

```
                        ┌──────────────────────┐
                        │   AiAgentService     │
                        │  (chat 入口)          │
                        └──────┬───────────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
          ┌─────────▼────────┐  ┌────────▼──────────┐
          │ KnowledgeRag     │  │ KnowledgeSearch   │
          │ Service          │  │ Tool (@Tool)      │
          │ (RAG 编排)       │  │ (AI 按需调用)      │
          └─────────┬────────┘  └────────┬──────────┘
                    │                    │
          ┌─────────▼────────────────────▼──────────┐
          │       KnowledgeVectorService             │
          │  (向量化、存储、检索、重建索引)            │
          └─────────────────┬───────────────────────┘
                            │
          ┌─────────────────▼───────────────────────┐
          │          LazyVectorStore                 │
          │  (优雅降级代理，RediSearch 不可用时 no-op) │
          └─────────────────┬───────────────────────┘
                            │
          ┌─────────────────▼───────────────────────┐
          │      RedisVectorStore (Spring AI)        │
          │  索引: brief-wisdom-knowledge             │
          │  前缀: bw:doc:                            │
          │  向量维度: 1024                            │
          └─────────────────────────────────────────┘
```

---

## 核心组件

### VectorStoreConfig（配置层）

位于 `brief-wisdom-web` 模块，负责组装 VectorStore 和 EmbeddingModel。

**EmbeddingModel**：使用默认 AI 提供商的 API Key 构建 `OpenAiEmbeddingModel`，模型为 `text-embedding-v3`，向量维度 1024。若未配置提供商，回退到 `NoOpEmbeddingModel`（返回空向量）。

**VectorStore**：返回 `LazyVectorStore` 包装器。实际的 `RedisVectorStore` 通过 `Supplier<VectorStore>` 懒加载创建，使用 Jedis `RedisClient` 连接 Redis，配置索引名 `brief-wisdom-knowledge`、键前缀 `bw:doc:`，启用 `initializeSchema(true)` 自动建索引。若 RediSearch 不可用，捕获异常并返回 null。

### LazyVectorStore（优雅降级代理）

实现 `org.springframework.ai.vectorstore.VectorStore` 接口，使用 `AtomicReference<VectorStore>` 持有实际代理和 `AtomicReference<Boolean>` 跟踪初始化状态。

核心行为：

- `add()` / `delete()` / `similaritySearch()` 调用前先触发一次性初始化
- 若初始化失败（RediSearch 不可用），`delegate` 保持 null，所有操作变为 no-op
- `isAvailable()` 暴露底层存储是否初始化成功

这种设计确保即使 Redis 不支持 RediSearch 模块，应用也能正常启动和运行。

### NoOpEmbeddingModel（回退实现）

当未配置 AI 提供商 API Key 时使用。实现 `EmbeddingModel` 接口，返回空 `EmbeddingResponse` 和零长度 `float[]`。

### KnowledgeVectorService（向量操作服务）

位于 `brief-wisdom-ai` 模块，是向量操作的核心服务。注入 `VectorStore` 和 `KnowledgeDocumentRepository`。

**主要方法**：

- `vectorizeAndStore(KnowledgeDocument)`：提取文本内容（INTERNAL 文档去除 HTML、LINK 使用 linkDesc、FILE 使用 fileName+fileType），截断至 4000 字符，构建 Spring AI `Document` 并附带元数据（docId、baseId、title、docType、tags），先删除旧向量再存储新向量
- `deleteFromVectorStore(Long)`：按文档 ID 删除向量
- `searchBySimilarity(String query, int topK)`：调用 `vectorStore.similaritySearch()`，相似度阈值 0.3，默认返回 top 5，从元数据提取 docId 后批量从数据库获取完整文档，保持向量相似度排序
- `reindexAll()`：遍历所有知识文档记录并重新向量化

### KnowledgeRagService（RAG 编排服务）

编排 RAG 检索流程。注入 `KnowledgeVectorService` 和 `ProjectCodeIndexService`。

**主要方法**：

- `retrieveRelevantDocuments(String userMessage)`：委托给 `knowledgeVectorService.searchBySimilarity()`，最多返回 5 个文档
- `buildContextFromDocuments(List<KnowledgeDocument>)`：将检索到的文档格式化为上下文字符串（最大 3000 字符），附加到系统提示词中。同时追加项目代码上下文

### KnowledgeSearchTool（AI 可调用的向量搜索工具）

使用 `@Tool` 注解，允许 AI 智能体按需调用知识库检索，而非每次对话都注入 RAG 上下文。委托给 `KnowledgeVectorService` 和 `KnowledgeRagService`。

### KnowledgeDocManagementTool（AI 可调用的文档管理工具）

使用 `@Tool` 注解，提供两个能力：`createKnowledgeDoc()`（创建文档并自动向量化）和 `listKnowledgeBases()`（列出知识库）。

### ProjectCodeIndexService（项目代码索引服务）

启动时（`@PostConstruct`）构建项目源文件的内存索引。扫描 `.java`、`.yml`、`.xml`、`.md`、`.sql`、`.properties` 文件，提取类名、包名和注释。提供 `searchCodeFiles(keyword)` 方法，按相关度评分（文件名匹配、类名匹配等）。由 `KnowledgeRagService` 调用，将项目结构信息追加到 RAG 上下文中。

---

## RAG 在对话中的调用方式

在 `AiAgentService.chatWithSession()` 和 `chatStreamWithSession()` 中：

```java
// 1. 检索相关文档
var relevantDocs = knowledgeRagService.retrieveRelevantDocuments(message);

// 2. 构建上下文
String ragContext = knowledgeRagService.buildContextFromDocuments(relevantDocs);

// 3. 注入系统提示词
if (!ragContext.isBlank()) {
    systemPrompt += ragContext;
}
```

RAG 上下文在每次聊天消息时注入到系统提示词中。

---

## 文档生命周期与自动向量化

在 `KnowledgeServiceImpl` 中，文档的 CRUD 操作会自动触发向量化：

- **创建/更新文档**：通过 `CompletableFuture.runAsync()` 在 `briefWisdomExecutor` 线程池中异步执行向量化，不阻塞主流程
- **删除文档**：同步调用 `knowledgeVectorService.deleteFromVectorStore()` 清理向量
- **导入 Markdown**：`upsertImportedMarkdown()` 同样触发异步向量化

---

## 配置说明

### Redis Vector Store 配置

```yaml
# VectorStoreConfig 中的配置参数
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

向量存储参数：

| 参数 | 值 | 说明 |
|------|-----|------|
| indexName | `brief-wisdom-knowledge` | RediSearch 索引名 |
| prefix | `bw:doc:` | Redis 键前缀 |
| dimensions | 1024 | 向量维度 |
| similarityThreshold | 0.3 | 最低相似度阈值 |
| defaultTopK | 5 | 默认返回文档数 |

### 降级机制

| 场景 | 行为 |
|------|------|
| Redis 未启动 | `LazyVectorStore` 初始化失败，所有向量操作 no-op |
| Redis 运行但无 RediSearch | 索引创建失败，同上 |
| AI 提供商未配置 | 使用 `NoOpEmbeddingModel`，向量化返回空向量 |
| 正常情况 | 向量化存储和语义检索正常工作 |

---

## 关键文件清单

| 文件 | 模块 | 职责 |
|------|------|------|
| `VectorStoreConfig.java` | web | 配置 EmbeddingModel 和 VectorStore Bean |
| `LazyVectorStore.java` | web | 优雅降级代理 |
| `NoOpEmbeddingModel.java` | web | 回退 Embedding 实现 |
| `KnowledgeVectorService.java` | ai | 向量操作核心服务 |
| `KnowledgeRagService.java` | ai | RAG 编排服务 |
| `KnowledgeSearchTool.java` | ai | AI 可调用的搜索工具 |
| `KnowledgeDocManagementTool.java` | ai | AI 可调用的文档管理工具 |
| `KnowledgeServiceImpl.java` | ai | 文档生命周期管理（含自动向量化） |
| `ProjectCodeIndexService.java` | ai | 项目代码内存索引 |

---

**最后更新**: 2026-07-15
