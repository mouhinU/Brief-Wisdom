## Brief-Wisdom AI 工具（Tool Calling）设计方案

**设计时间**: 2026-07-15
**技术基础**: Spring AI 2.0.0 `@Tool` 注解 + Function Calling
**当前状态**: 项目已集成 Spring AI 2.0.0，但尚未使用任何工具调用能力，所有上下文（RAG、记忆、代码）均通过手动拼接 System Prompt 注入

---

### 设计原则

1. **按需调用** — 让模型自行决定何时调用工具，避免每次都注入大量上下文浪费 Token
2. **渐进式迁移** — 优先将现有手动注入的上下文转为工具调用，再扩展新能力
3. **安全可控** — 工具调用需经过权限校验，敏感操作需用户确认
4. **可观测** — 所有工具调用记录到审计日志

---

### 第一梯队：核心工具（基于现有服务，优先级最高）

这一梯队的工具直接基于项目已有的 Service，实现成本低，收益最大。

#### 工具 1：知识库语义搜索 — `searchKnowledge`

**现状痛点**：当前每次对话都会执行 RAG 检索并注入 System Prompt，即使用户问的是"今天天气怎么样"也会消耗知识库检索的 Token。

**工具化收益**：模型仅在需要知识库内容时才调用，节省 Token，提高响应速度。

```java
@Tool(description = "从知识库中搜索与用户问题相关的文档内容。当用户的问题可能涉及项目知识、业务规则、技术文档时调用此工具。")
public String searchKnowledge(
    @ToolParam(description = "搜索查询文本") String query,
    @ToolParam(description = "返回结果数量，默认3", required = false) Integer topK
) {
    List<KnowledgeDocument> docs = knowledgeVectorService.searchBySimilarity(
        query, topK != null ? topK : 3);
    return knowledgeRagService.buildContextFromDocuments(docs);
}
```

**触发场景**：用户问"项目的部署流程是什么"、"Redis 缓存怎么配置的"、"知识库里有相关文档吗"

---

#### 工具 2：项目代码搜索 — `searchProjectCode`

**现状痛点**：代码上下文也是每次对话都注入，但大部分对话与代码无关。

```java
@Tool(description = "搜索项目源代码文件，获取代码结构、类名、包名等信息。当用户询问项目代码实现、文件位置、类结构时调用。")
public String searchProjectCode(
    @ToolParam(description = "搜索关键词，可以是类名、方法名、文件名") String keyword
) {
    List<CodeFileIndex> files = projectCodeIndexService.searchCodeFiles(keyword);
    // 格式化为可读文本
    return formatCodeSearchResults(files);
}
```

**触发场景**：用户问"KnowledgeService 在哪里实现的"、"项目有哪些 Controller"、"帮我找一下 Redis 配置相关的代码"

---

#### 工具 3：用户记忆管理 — `manageMemory`

**现状痛点**：记忆上下文每次都注入 System Prompt，随着记忆增多 Token 消耗线性增长。

```java
@Tool(description = "管理用户长期记忆。可以查看、保存或删除记忆条目。当用户说'记住这个'、'你还记得吗'、'忘掉XX'时调用。")
public String manageMemory(
    @ToolParam(description = "操作类型: list/save/delete") String action,
    @ToolParam(description = "记忆分类，如 preference/fact/task", required = false) String category,
    @ToolParam(description = "记忆内容或关键词", required = false) String content
) {
    String userId = UserContextHelper.getCurrentUserId();
    return switch (action) {
        case "list" -> chatMemoryService.listMemories(userId);
        case "save" -> chatMemoryService.saveMemory(userId, category, content);
        case "delete" -> chatMemoryService.deleteMemoryByKeyword(userId, content);
        default -> "未知操作: " + action;
    };
}
```

**触发场景**：用户说"记住我喜欢用 DeepSeek"、"你还记得我的技术栈吗"、"忘掉之前的偏好"

---

#### 工具 4：网页内容抓取 — `fetchWebPage`

**现状痛点**：`UrlFetchService` 已存在但仅在知识库导入时使用，AI 对话中无法主动获取网页内容。

```java
@Tool(description = "抓取指定 URL 的网页内容摘要。当用户提供一个链接并希望你分析其内容时调用。")
public String fetchWebPage(
    @ToolParam(description = "要抓取的网页 URL") String url
) {
    UrlMetadata metadata = urlFetchService.fetchUrlMetadata(url);
    return String.format("标题: %s\n描述: %s\n内容摘要: %s",
        metadata.getTitle(), metadata.getDescription(), metadata.getContent());
}
```

**触发场景**：用户说"帮我看看这个链接讲了什么 https://..."、"总结一下这个页面的内容"

---

### 第二梯队：增强工具（新增能力，中等优先级）

这一梯队需要新写实现逻辑，为 AI 助手增加全新能力。

#### 工具 5：日期时间感知 — `getCurrentDateTime`

**价值**：AI 模型本身不知道当前时间，用户问"今天是几号"、"现在几点了"时无法准确回答。

```java
@Tool(description = "获取当前日期和时间。当用户询问今天日期、当前时间、星期几时调用。")
public String getCurrentDateTime(
    @ToolParam(description = "时区，默认 Asia/Shanghai", required = false) String timezone
) {
    ZoneId zone = ZoneId.of(timezone != null ? timezone : "Asia/Shanghai");
    LocalDateTime now = LocalDateTime.now(zone);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss EEEE");
    return now.format(fmt);
}
```

---

#### 工具 6：数学计算 — `calculate`

**价值**：AI 模型在大数字计算、复杂公式上容易出错，提供精确计算能力。

```java
@Tool(description = "执行数学计算。当需要进行精确的数学运算、单位换算、百分比计算时调用。")
public String calculate(
    @ToolParam(description = "数学表达式，如 '(100 + 200) * 3 / 4'、'sqrt(144)'、'2^10'") String expression
) {
    // 使用 JavaScript 引擎或 exp4j 解析表达式
    return ExpressionEvaluator.evaluate(expression);
}
```

**触发场景**：用户问"100 的 15% 是多少"、"帮我算一下 (234 + 567) * 89"、"根号 144 等于多少"

---

#### 工具 7：简历分析 — `analyzeResume`

**价值**：项目有完整的简历管理模块，AI 可以主动分析简历内容并给出建议。

```java
@Tool(description = "分析简历内容，包括工作经历、项目经验、技术栈等，给出优化建议。当用户请求简历诊断、优化建议时调用。")
public String analyzeResume(
    @ToolParam(description = "分析维度: overall/work/project/skills/summary") String dimension
) {
    String userId = UserContextHelper.getCurrentUserId();
    // 从简历模块获取用户数据
    ResumeData resume = resumeService.getUserResume(userId);
    return switch (dimension) {
        case "overall" -> resumeAnalyzer.analyzeOverall(resume);
        case "work" -> resumeAnalyzer.analyzeWorkExperience(resume);
        case "project" -> resumeAnalyzer.analyzeProjects(resume);
        case "skills" -> resumeAnalyzer.analyzeSkills(resume);
        default -> "未知维度: " + dimension;
    };
}
```

---

#### 工具 8：系统状态查询 — `getSystemStatus`

**价值**：管理员可以通过 AI 助手快速了解系统运行状态。

```java
@Tool(description = "查询系统运行状态，包括用户数、会话数、模型使用情况、Redis 状态等。仅限管理员使用。")
public String getSystemStatus(
    @ToolParam(description = "查询类型: overview/users/ai/redis/storage", required = false) String type
) {
    // 权限校验
    if (!UserContextHelper.hasRole("super_admin", "admin")) {
        return "权限不足，仅管理员可查询系统状态";
    }
    return switch (type != null ? type : "overview") {
        case "users" -> systemStatusService.getUserStats();
        case "ai" -> systemStatusService.getAIUsageStats();
        case "redis" -> systemStatusService.getRedisStats();
        default -> systemStatusService.getOverview();
    };
}
```

---

### 第三梯队：扩展工具（锦上添花，低优先级）

#### 工具 9：文本翻译 — `translateText`

```java
@Tool(description = "将文本翻译为目标语言。当用户明确要求翻译某段文字时调用。")
public String translateText(
    @ToolParam(description = "要翻译的文本") String text,
    @ToolParam(description = "目标语言代码，如 en/zh/ja/ko") String targetLang
)
```

**说明**：可复用现有的 ChatModel 做翻译，或接入专业翻译 API。

---

#### 工具 10：知识库文档管理 — `manageKnowledgeDoc`

```java
@Tool(description = "管理知识库文档，可以创建、搜索、删除知识条目。当用户要求将某段内容保存到知识库时调用。")
public String manageKnowledgeDoc(
    @ToolParam(description = "操作: create/search/delete") String action,
    @ToolParam(description = "文档标题") String title,
    @ToolParam(description = "文档内容或搜索关键词") String content,
    @ToolParam(description = "目标知识库ID", required = false) Long baseId
)
```

**触发场景**：用户说"把这个对话要点保存到知识库"、"帮我创建一条知识记录"

---

#### 工具 11：定时提醒 — `createReminder`

```java
@Tool(description = "为用户创建提醒事项。当用户说'提醒我XX'、'X点后记得XX'时调用。")
public String createReminder(
    @ToolParam(description = "提醒内容") String reminderText,
    @ToolParam(description = "提醒时间，ISO-8601 格式或相对时间如 '30分钟后'") String remindAt
)
```

---

#### 工具 12：SQL 查询助手 — `queryDatabase`（仅限开发/管理员）

```java
@Tool(description = "执行只读 SQL 查询，用于数据分析和排查问题。仅限 super_admin 使用，仅允许 SELECT 语句。")
public String queryDatabase(
    @ToolParam(description = "SELECT SQL 语句（不允许 INSERT/UPDATE/DELETE）") String sql,
    @ToolParam(description = "最大返回行数，默认10", required = false) Integer maxRows
)
```

**安全约束**：
- 仅允许 SELECT 语句，正则校验禁止 INSERT/UPDATE/DELETE/DROP/ALTER
- 强制 LIMIT 上限（最大 100 行）
- 查询超时 5 秒自动终止
- 所有查询记录到审计日志

---

### 实现架构

```
用户消息
    │
    ▼
AiAgentService.chatStreamWithSession()
    │
    ├── 构建 Prompt（仅包含基础 System Prompt + 会话上下文）
    │
    ├── 注册可用工具列表（ToolCallbackProvider）
    │   ├── KnowledgeTools      → searchKnowledge
    │   ├── CodeTools           → searchProjectCode
    │   ├── MemoryTools         → manageMemory
    │   ├── WebTools            → fetchWebPage
    │   ├── DateTimeTools       → getCurrentDateTime
    │   ├── CalculatorTools     → calculate
    │   ├── ResumeTools         → analyzeResume
    │   └── SystemTools         → getSystemStatus
    │
    ├── chatModel.stream(prompt)  ← 模型自行决定是否调用工具
    │   │
    │   ├── [模型决定调用工具] → 执行工具 → 结果返回模型 → 继续生成
    │   │
    │   └── [模型直接回答] → 流式输出给用户
    │
    └── 工具调用记录 → 审计日志
```

### Spring AI 2.0 集成方式

```java
// 方式一：@Tool 注解 + ToolCallbackProvider（推荐）
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider briefWisdomTools(
            KnowledgeVectorService knowledgeVectorService,
            KnowledgeRagService knowledgeRagService,
            ProjectCodeIndexService codeIndexService,
            ChatMemoryService chatMemoryService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                    new KnowledgeTools(knowledgeVectorService, knowledgeRagService),
                    new CodeTools(codeIndexService),
                    new MemoryTools(chatMemoryService),
                    new DateTimeCalculatorTools()
                )
                .build();
    }
}

// 方式二：在 ChatOptions 中指定工具
OpenAiChatOptions options = OpenAiChatOptions.builder()
        .model(modelName)
        .toolCallbacks(toolCallbackProvider.getToolCallbacks())
        .build();
```

### 实施路线图

| 阶段 | 工具 | 工作量 | 预期效果 |
|------|------|--------|----------|
| **Phase 1** | searchKnowledge + searchProjectCode + getCurrentDateTime | 2-3 天 | 替代手动 Prompt 注入，节省 Token 30%+ |
| **Phase 2** | manageMemory + fetchWebPage + calculate | 2-3 天 | 新增记忆管理、网页抓取、精确计算能力 |
| **Phase 3** | analyzeResume + getSystemStatus | 3-4 天 | 简历诊断、系统运维助手 |
| **Phase 4** | translateText + manageKnowledgeDoc + createReminder + queryDatabase | 5-7 天 | 完整工具生态 |

### 需要你确认的事项

1. 以上 12 个工具中，你觉得哪些最实用？想优先实现哪几个？
2. SQL 查询助手（工具 12）是否需要？安全风险较高但运维价值大。
3. 是否需要我直接开始实现第一梯队的工具？
