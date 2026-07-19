# Brief-Wisdom 技术文档

> 本目录包含项目的技术文档、架构设计、功能说明和开发指南。

**技术栈**：Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21 / Redis Vector Store (RediSearch)

---

## 文档结构

```
docs/
├── README.md                          # 文档索引（本文件）
│
├── architecture/                      # 架构设计文档
│   ├── sync-architecture.md           # 实时同步架构(SSE/WebSocket)
│   ├── rag-architecture.md            # RAG 向量检索架构(Redis Vector Store)
│   └── chat-memory.md                 # 跨会话记忆功能设计
│
├── features/                          # 功能特性文档
│   └── ai-polish/
│       └── README.md                  # AI 润色完整文档
│
├── guides/                            # 开发指南
│   ├── developer-guide.md             # 开发者快速入门
│   ├── database-schema.md             # 数据库表结构与配置
│   ├── LOG_CONFIGURATION.md           # 日志配置说明
│   └── MARKDOWN_IMPORT_GUIDE.md       # Markdown 导入指南
│
└── analysis/
    ├── README.md                      # 项目能力总结与未来规划
    └── technical-business-recommendations.md  # 技术业务建议报告
```

---

## 快速导航

### 新手入门

1. [开发者快速入门](guides/developer-guide.md) — 环境准备、快速启动、常见问题
2. [README.md](../README.md) — 项目概览、核心功能、技术栈
3. [AGENTS.md](../AGENTS.md) — 编码规范
4. [变更日志](../CHANGELOG.md) — 修复历史与版本变更

### 架构设计

- [实时同步架构](architecture/sync-architecture.md) — SSE/WebSocket 方案选型、事件推送机制
- [RAG 向量检索架构](architecture/rag-architecture.md) — Redis Vector Store RAG 设计、LazyVectorStore 降级
- [跨会话记忆](architecture/chat-memory.md) — ChatMemory 功能设计、自动提取、AI 工具调用

### 功能开发

- [AI 润色功能](features/ai-polish/README.md) — 组件设计、API 参考、集成记录
- [日志配置](guides/LOG_CONFIGURATION.md) — Logback 配置、日志分级、故障排查
- [数据库表结构](guides/database-schema.md) — 表结构详情、MyBatis-Plus 配置、逻辑删除
- [Markdown 导入](guides/MARKDOWN_IMPORT_GUIDE.md) — Markdown 文件导入功能说明

### 项目分析

- [项目能力总结与规划](analysis/README.md) — 能力评估、已完成建设、未来规划、AI 插件设计蓝图
- [技术业务建议报告](analysis/technical-business-recommendations.md) — 10 项技术建议 + 5 项业务建议，含优先级评估

---

## 推荐阅读顺序

### 新加入项目的开发者

1. [README.md](../README.md) — 了解项目全貌
2. [开发者快速入门](guides/developer-guide.md) — 环境搭建、快速启动
3. [AGENTS.md](../AGENTS.md) — 编码规范
4. [实时同步架构](architecture/sync-architecture.md) — 理解核心技术实现

### 需要开发新功能的开发者

1. [开发者快速入门](guides/developer-guide.md) — 熟悉项目结构
2. [AGENTS.md](../AGENTS.md) — 遵循编码规范
3. 查看相关模块的现有代码（如 `brief-wisdom-ai/`）
4. 参考类似功能的实现（如 RAG 架构、跨会话记忆）

### 进行性能优化的开发者

1. [项目能力总结与规划](analysis/README.md) — 了解优化方向
2. [日志配置](guides/LOG_CONFIGURATION.md) — 启用性能监控日志
3. [数据库表结构](guides/database-schema.md) — 了解表结构和索引

---

## 文档维护规范

### 新增文档

1. **确定分类**：根据文档类型选择对应目录（`architecture/`、`features/`、`guides/`、`analysis/`）
2. **命名规范**：使用英文小写+下划线，如 `knowledge-base-design.md`
3. **添加索引**：在本文件中添加链接和简要说明
4. **更新 README**：如果文档重要，在 [README.md](../README.md) 中添加引用

### 更新文档

1. **保持同步**：代码变更后及时更新相关文档
2. **版本标记**：重大变更时标注日期和版本号
3. **删除过时内容**：避免保留已废弃的实现说明

### 文档格式

- 使用 Markdown 格式
- 标题层级清晰（H1 → H2 → H3）
- 代码块指定语言（如 `java`、`yaml`、`bash`）
- 表格对齐美观

---

**最后更新**: 2026-07-16
