# Brief-Wisdom 技术文档

> 本目录包含项目的技术文档、架构设计、功能说明和开发指南。

---

## 📁 文档结构

```
docs/
├── architecture/          # 架构设计文档
│   └── sync-architecture.md       # 实时同步架构(SSE/WebSocket)
│
├── features/              # 功能特性文档
│   └── ai-polish/                 # AI 润色功能
│       ├── AI_POLISH_COMPONENT.md           # 组件设计
│       ├── AI_POLISH_INTEGRATION_COMPLETE.md # 集成完成报告
│       ├── AI_POLISH_TAB_INTEGRATION.md     # Tab 集成说明
│       └── AI_POLISH_VERIFICATION.md        # 验证报告
│
├── guides/                # 开发指南
│   ├── developer-guide.md           # 开发者快速入门 ⭐
│   ├── LOG_CONFIGURATION.md         # 日志配置说明
│   └── api-reference.md             # API 参考文档(待补充)
│
└── analysis/              # 分析报告
    └── 项目能力分析与改进建议.md     # 项目能力评估与优化建议
```

---

## 📖 快速导航

### 👶 新手入门

1. **[开发者快速入门](guides/developer-guide.md)** - 环境准备、快速启动、常见问题
2. **[README.md](../README.md)** - 项目概览、核心功能、API 接口

### 🏗️ 架构设计

- **[实时同步架构](architecture/sync-architecture.md)** - SSE/WebSocket 方案选型、事件推送机制

### 🔧 功能开发

- **[AI 润色功能](features/ai-polish/)** - 组件设计、集成流程、验证报告
- **[日志配置](guides/LOG_CONFIGURATION.md)** - Logback 配置、日志分级、故障排查

### 📊 项目分析

- **[项目能力分析与改进建议](analysis/项目能力分析与改进建议.md)** - 当前能力总结、改进优先级、未来规划

### 🐛 故障修复

- **[FIX_SUMMARY.md](../FIX_SUMMARY.md)** - StackOverflowError 修复总结(Redisson 递归冲突)

---

## 🎯 推荐阅读顺序

### 新加入项目的开发者

1. [README.md](../README.md) - 了解项目全貌
2. [开发者快速入门](guides/developer-guide.md) - 环境搭建、快速启动
3. [AGENTS.md](../AGENTS.md) - 编码规范
4. [实时同步架构](architecture/sync-architecture.md) - 理解核心技术实现

### 需要开发新功能的开发者

1. [开发者快速入门](guides/developer-guide.md) - 熟悉项目结构
2. [AGENTS.md](../AGENTS.md) - 遵循编码规范
3. 查看相关模块的现有代码(如 `brief-wisdom-ai/`)
4. 参考类似功能的实现(如 AI 润色功能的集成流程)

### 进行性能优化的开发者

1. [项目能力分析与改进建议](analysis/项目能力分析与改进建议.md) - 了解优化方向
2. [日志配置](guides/LOG_CONFIGURATION.md) - 启用性能监控日志
3. 使用 Arthas 或 JProfiler 进行性能分析

---

## 📝 文档维护规范

### 新增文档

1. **确定分类**: 根据文档类型选择对应目录(`architecture/`、`features/`、`guides/`、`analysis/`)
2. **命名规范**: 使用英文小写+下划线,如 `knowledge-base-design.md`
3. **添加索引**: 在本文件中添加链接和简要说明
4. **更新 README**: 如果文档重要,在 [README.md](../README.md) 中添加引用

### 更新文档

1. **保持同步**: 代码变更后及时更新相关文档
2. **版本标记**: 重大变更时标注日期和版本号
3. **删除过时内容**: 避免保留已废弃的实现说明

### 文档格式

- 使用 Markdown 格式
- 标题层级清晰(H1 → H2 → H3)
- 代码块指定语言(如 `java`、`yaml`、`bash`)
- 表格对齐美观
- 关键信息使用 ⚠️、✅、❌ 等符号突出显示

---

## 🔍 搜索技巧

在 IDE 中搜索文档内容:

```bash
# 搜索特定关键词
grep -r "SSE" docs/

# 查找最近修改的文档
find docs/ -name "*.md" -mtime -7

# 统计文档行数
wc -l docs/**/*.md
```

---

## 📞 需要帮助?

- **技术问题**: 查阅 [开发者快速入门](guides/developer-guide.md) 的常见问题章节
- **架构疑问**: 联系项目负责人或查看 [实时同步架构](architecture/sync-architecture.md)
- **规范咨询**: 参考 [AGENTS.md](../AGENTS.md)

---

**最后更新**: 2026-07-06
