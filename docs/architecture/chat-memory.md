# 跨会话记忆功能设计

> Brief-Wisdom 实现了跨会话记忆（Chat Memory）功能，使 AI 能够记住用户的偏好、事实和项目上下文，在不同会话之间保持连贯的个性化体验。

**技术栈**：Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21

---

## 功能概述

跨会话记忆允许 AI 在多次对话中记住用户的关键信息，例如姓名、公司、技术栈偏好、当前项目等。这些信息在每次对话时自动注入到系统提示词中，使
AI 能够提供更个性化的回答。

记忆的来源有两种：

1. **自动提取**：系统通过正则表达式从用户消息中自动识别和提取关键信息
2. **AI 主动记忆**：AI 智能体通过 `MemoryManagementTool` 工具主动保存和删除记忆

---

## 架构设计

```
用户消息
    │
    ├──→ AiAgentService.chatWithSession()
    │         │
    │         ├── chatMemoryService.buildMemoryContext(userId)
    │         │       │
    │         │       └── 加载所有记忆 → 格式化 → 注入系统提示词
    │         │
    │         ├── chatMemoryService.extractMemoriesFromMessage(userId, message, sessionId)
    │         │       │
    │         │       └── 正则匹配 → 自动保存记忆（异步）
    │         │
    │         └── 正常对话流程（携带记忆上下文）
    │
    └──→ MemoryManagementTool (@Tool)
              │
              ├── listMemories()     → "你还记得我吗？"
              ├── saveMemory(...)    → "记住我喜欢xxx"
              └── deleteMemory(...)  → "忘记xxx"
```

---

## 数据模型

### ChatMemory 实体

位于 `brief-wisdom-persistence` 模块，映射到 `chat_memory` 表。

| 字段              | 类型       | 说明                                     |
|-----------------|----------|----------------------------------------|
| id              | BIGINT   | 自增主键                                   |
| userId          | String   | 用户 ID                                  |
| category        | String   | 记忆类别：`preference` / `fact` / `context` |
| memoryKey       | String   | 记忆键（如 `name`、`tech_stack`、`company`）   |
| memoryValue     | String   | 记忆值                                    |
| sourceSessionId | String   | 来源会话 ID                                |
| accessCount     | Integer  | 访问次数（默认 0）                             |
| create_time     | DATETIME | 创建时间                                   |
| update_time     | DATETIME | 更新时间                                   |

### 记忆类别

| 类别           | 说明     | 示例       |
|--------------|--------|----------|
| `fact`       | 用户事实信息 | 姓名、公司、角色 |
| `preference` | 用户偏好   | 技术栈、编码风格 |
| `context`    | 当前上下文  | 正在做的项目   |

---

## 核心组件

### ChatMemoryRepository

位于 `brief-wisdom-persistence` 模块，Bean 名称为 `userChatMemoryRepository`。

提供以下数据访问方法：

- `findByUserId(userId)`：按用户查询所有记忆，按访问次数降序排列
- `findByUserIdAndCategory(userId, category)`：按类别筛选
- `findByUserIdAndKey(userId, memoryKey)`：按记忆键精确查找
- `save()` / `update()` / `deleteById()` / `deleteByUserId()`：基本 CRUD
- `incrementAccessCount(id)`：递增访问次数

### ChatMemoryService

位于 `brief-wisdom-ai` 模块，是记忆功能的核心服务。

**saveMemory(userId, category, key, value, sessionId)**：按 `userId + memoryKey` 进行 upsert 操作。若已存在则更新值和类别，若不存在则创建新记录。

**buildMemoryContext(userId)**：加载用户所有记忆，格式化为注入系统提示词的上下文：

```
--- 用户记忆 ---
以下是你记住的关于该用户的信息，请在回答时参考：
- name: 张三
- tech_stack: Java/Spring
--- 记忆结束 ---
```

**extractMemoriesFromMessage(userId, userMessage, sessionId)**：通过正则表达式从用户消息中自动提取记忆。支持的匹配模式：

| 模式                 | 示例           | 提取结果                             |
|--------------------|--------------|----------------------------------|
| `我叫XXX`            | "我叫张三"       | fact / name / 张三                 |
| `我在XXX公司/企业/单位/工作` | "我在阿里巴巴工作"   | fact / company / 阿里巴巴            |
| `我是XXX开发/工程师/...`  | "我是Java开发"   | fact / role / Java开发             |
| `我用XXX语言/框架/技术`    | "我用Spring框架" | preference / tech_stack / Spring |
| `我喜欢XXX风格/方式/模式`   | "我喜欢函数式风格"   | preference / style / 函数式风格       |
| `我在做XXX项目/系统/平台`   | "我在做订单系统"    | context / current_project / 订单系统 |

**listMemories() / deleteMemory() / clearMemories() / recordAccess()**：记忆管理操作。

### MemoryManagementTool

位于 `brief-wisdom-ai` 模块，使用 `@Tool` 注解，允许 AI 智能体主动管理记忆。

提供三个工具方法：

- `listMemories()`：当用户问"你还记得我吗"等触发
- `saveMemory(category, key, value)`：当用户说"记住这个"等触发
- `deleteMemory(key)`：当用户说"忘记xxx"等触发

通过 `ToolContextProvider` 获取当前用户 ID。

---

## 在对话中的调用方式

在 `AiAgentService.chatWithSession()` 和 `chatStreamWithSession()` 中：

```java
// 1. 注入记忆上下文到系统提示词
String memoryContext = chatMemoryService.buildMemoryContext(userId);
if (!memoryContext.isBlank()) {
    systemPrompt += memoryContext;
}

// 2. 自动从用户消息中提取记忆（非阻塞）
chatMemoryService.extractMemoriesFromMessage(userId, message, sessionId);
```

记忆上下文在每次聊天时注入系统提示词，自动提取在每条用户消息后异步执行。

---

## 关键文件清单

| 文件                          | 模块          | 职责                  |
|-----------------------------|-------------|---------------------|
| `ChatMemory.java`           | persistence | 记忆实体                |
| `ChatMemoryMapper.java`     | persistence | MyBatis-Plus Mapper |
| `ChatMemoryRepository.java` | persistence | 数据访问层               |
| `ChatMemoryService.java`    | ai          | 记忆核心服务              |
| `MemoryManagementTool.java` | ai          | AI 可调用的记忆管理工具       |

---

**最后更新**: 2026-07-15
