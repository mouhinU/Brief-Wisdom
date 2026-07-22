# Markdown 文件导入知识库使用指南

## 功能概述

本功能允许你将项目中的 Markdown (.md) 文件批量导入到知识库系统中，作为知识文档进行管理。导入后的文档可以作为 AI
助手的知识库内容，用于 RAG（检索增强生成）等场景。

## 支持的文件类型

- ✅ `.md` - Markdown 文件
- 📁 支持递归导入子目录
- 📄 自动将文件名（不含扩展名）作为文档标题
- 📝 文件内容原样保存为 Markdown 格式

## 使用方法

### 方式一：通过前端界面导入（推荐）

1. **进入知识库管理页面**
    - 访问 AI 助手管理页面
    - 切换到"知识库"标签页

2. **选择目标知识库**
    - 在左侧列表中选择要导入的知识库
    - 如果没有合适的知识库，可以先创建一个新的

3. **点击"📥 导入 MD"按钮**
    - 在右侧顶部工具栏找到该按钮

4. **选择导入方式**

   **选项 1：导入 docs 目录（推荐）**
    - 自动导入项目 `docs/` 目录下的所有 Markdown 文件
    - 包括所有子目录
    - 适合导入项目文档

   **选项 2：导入 AGENTS.md**
    - 仅导入项目根目录的 `AGENTS.md` 文件
    - 适合导入编码规范文档

   **选项 3：自定义目录**
    - 输入相对路径（相对于项目根目录）
    - 例如：`docs/guides`、`brief-wisdom-ai/src/main/java`
    - 可选择是否递归子目录

5. **点击"开始导入"**
    - 系统会自动处理所有符合条件的 .md 文件
    - 导入完成后会显示成功导入的文件数量

### 方式二：通过 API 接口导入

#### 1. 导入 docs 目录

```bash
curl -X POST "http://localhost:8090/api/knowledge/import/docs?baseId=1"
```

#### 2. 导入 AGENTS.md

```bash
curl -X POST "http://localhost:8090/api/knowledge/import/agents?baseId=1"
```

#### 3. 导入自定义目录

```bash
# 非递归导入
curl -X POST "http://localhost:8090/api/knowledge/import/markdown?baseId=1&sourceDir=docs/guides&recursive=false"

# 递归导入
curl -X POST "http://localhost:8090/api/knowledge/import/markdown?baseId=1&sourceDir=docs&recursive=true"
```

**参数说明：**

- `baseId`: 目标知识库 ID（必填）
- `sourceDir`: 源目录路径，相对于项目根目录（自定义导入时必填）
- `recursive`: 是否递归子目录，默认 true（可选）

## 导入效果

### 导入前

```
docs/
├── README.md
├── guides/
│   ├── LOG_CONFIGURATION.md
│   └── developer-guide.md
└── features/
    └── ai-polish/
        └── AI_POLISH_COMPONENT.md
```

### 导入后

知识库中会创建以下文档：

- 📝 README
- 📝 LOG_CONFIGURATION
- 📝 developer-guide
- 📝 AI_POLISH_COMPONENT

每个文档的内容都是原始 Markdown 文件的完整内容。

## 注意事项

### ⚠️ 重要提示

1. **重复导入**
    - 已导入的文件会按源路径自动**更新**内容
    - 未导入的文件会**新增**文档
    - 去重键为相对项目根目录的文件路径（如 `docs/guides/developer-guide.md`）
    - 早期未记录源路径的历史文档，首次重复导入时会按标题匹配并更新，同时补写源路径

2. **文件编码**
    - 确保 Markdown 文件使用 UTF-8 编码
    - 避免特殊字符导致乱码

3. **文件大小**
    - 单个文件建议不超过 1MB
    - 超大文件可能影响查询性能

4. **目录路径**
    - 路径相对于项目根目录
    - Windows 用户使用正斜杠 `/` 而非反斜杠 `\`
    - 示例：`docs/guides` ✅，`docs\guides` ❌

5. **权限要求**
    - 需要对目标知识库有写入权限
    - 需要能够读取源目录的文件

### 💡 最佳实践

1. **组织文档结构**
   ```
   knowledge-base/
   ├── 技术文档/     ← 导入 technical/ 目录
   ├── 产品文档/     ← 导入 product/ 目录
   └── 规范文档/     ← 导入 specs/ 目录
   ```

2. **定期同步**
    - 建立定期导入机制
    - 保持知识库与项目文档同步

3. **分类管理**
    - 不同类型的文档导入到不同的知识库
    - 便于后续管理和检索

4. **文档优化**
    - 导入前检查 Markdown 格式
    - 确保标题清晰、结构合理
    - 添加适当的标签便于搜索

## 故障排查

### 问题 1：导入失败，提示"源目录不存在"

**原因：** 路径错误或目录不存在

**解决：**

- 检查路径是否正确（相对于项目根目录）
- 确认目录确实存在
- 尝试使用绝对路径测试

### 问题 2：导入成功但文档数量为 0

**原因：** 目录下没有 .md 文件

**解决：**

- 确认目录下有 .md 文件
- 检查文件扩展名是否为小写 `.md`
- 尝试递归导入以包含子目录

### 问题 3：导入后文档内容为空

**原因：** 文件编码问题或读取权限不足

**解决：**

- 检查文件编码是否为 UTF-8
- 确认应用有读取文件的权限
- 查看后端日志获取详细错误信息

### 问题 4：导入速度很慢

**原因：** 文件数量过多或文件过大

**解决：**

- 分批导入不同目录
- 先导入重要文档，再逐步补充
- 考虑优化大文件内容

## 技术实现

### 后端架构

- **Service 层**: `MarkdownImportService`
    - 负责遍历目录、读取文件、创建文档
    - 支持递归和非递归模式

- **Controller 层**: `KnowledgeController`
    - 提供 REST API 接口
    - 记录导入日志

### 前端实现

- **组件**: `knowledge-management.js`
    - 提供友好的导入界面
    - 实时显示导入进度和结果

- **模板**: `knowledge-management.template.js`
    - 定义导入弹窗 UI

### 数据存储

- 导入的文档存储在 `knowledge_document` 表
- 文档类型标记为 `INTERNAL`（内部文档）
- 内容字段直接存储 Markdown 原文

## 未来扩展

可能的改进方向：

1. ✨ 支持 Markdown 转 HTML 渲染预览
2. 🔄 增量导入（只导入新增/修改的文件）
3. 📊 导入统计和报告
4. 🔍 智能提取文档元数据（标题、标签等）
5. 🎨 支持更多文档格式（PDF、Word 等）

## 相关文档

- [知识库管理功能说明](../README.md)
- [API 接口文档](http://localhost:8090/swagger-ui.html)（SpringDoc OpenAPI，需启动应用）
- [开发者指南](./developer-guide.md)

---

**最后更新**: 2026-07-06  
**作者**: Brief-Wisdom Team
