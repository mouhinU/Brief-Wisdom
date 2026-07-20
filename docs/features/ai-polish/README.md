# AI 润色功能文档

> AI 润色组件为 Brief-Wisdom 简历管理模块提供智能文案优化能力，支持工作经历、项目经历、项目成果等多种字段类型的一键润色。

---

## 组件说明

AI 润色组件是一个独立、可复用的前端组件，为任何文本输入框提供 AI 智能润色功能。

### 核心特性

- 一键润色：点击按钮即可调用 AI 优化文本
- 智能对比：左右分栏展示原文和润色结果
- 灵活选择：用户可选择采纳或拒绝 AI 建议
- 响应式设计：适配桌面和移动端
- 易于集成：只需引入 3 个文件即可使用

### 组件文件

```
brief-wisdom-web/src/main/resources/static/
├── components/
│   ├── ai-polish.template.js    # 模板生成器（可选）
│   └── ai-polish.js             # 业务逻辑（必需）
└── css/
    └── ai-polish.css            # 样式文件（必需）
```

---

## 快速开始

### 1. 引入文件

在 HTML 页面的 `<head>` 中引入 CSS，在 `<body>` 底部引入 JS：

```html
<head>
    <link rel="stylesheet" href="/css/ai-polish.css">
</head>
<body>
    <script src="/components/ai-polish.js"></script>
</body>
```

### 2. 手动添加按钮

在 textarea 旁边添加 AI 润色按钮：

```html
<div class="form-field-with-ai">
    <textarea id="my-textarea" rows="4">需要润色的文本</textarea>
    <button type="button"
            class="ai-polish-btn"
            onclick="AiPolishComponent.polish('my-textarea', 'description', '公司名称')"
            title="AI润色">
        ✨ AI
    </button>
</div>
```

**参数说明**：

- `textareaId`：textarea 的 DOM ID
- `fieldType`：字段类型（`description` / `background` / `duty` / `achievement`）
- `context`：上下文信息（可选，如公司名、项目名），帮助 AI 更好地理解语境

### 3. 使用模板生成器（推荐）

引入 `ai-polish.template.js` 后，可使用辅助函数生成 HTML：

```javascript
const html = AiPolishComponent.generateTextareaWithPolish({
    id: 'my-textarea',
    value: '需要润色的文本',
    rows: 4,
    fieldType: 'description',
    context: '测试公司',
    label: '工作描述'
});
```

### 4. 批量初始化

页面上已有多个 `.ai-polish-wrapper` 元素时，可自动初始化：

```html
<div class="ai-polish-wrapper"
     data-textarea-id="textarea-1"
     data-field-type="description"
     data-context="公司A">
    <textarea id="textarea-1"></textarea>
    <button class="ai-polish-btn">✨ AI</button>
</div>

<script>
    AiPolishComponent.initPage();
</script>
```

---

## API 参考

### AiPolishComponent.polish(textareaId, fieldType, context)

触发 AI 润色的核心函数。

### AiPolishComponent.showComparisonModal(originalText, polishedText, targetTextarea)

手动显示对比弹窗（高级用法）。

### AiPolishComponent.showToast(message, type)

显示轻量级提示消息。`type` 可选 `success` / `info` / `warning` / `error`。

### AiPolishComponent.initPage()

批量初始化页面上的所有 AI 润色按钮。

### AiPolishComponent.generateTextareaWithPolish(options)

生成带 AI 润色按钮的 textarea HTML（需引入 `template.js`）。

---

## 自定义事件

组件会触发自定义事件，方便其他组件监听用户操作：

```javascript
// 用户采纳润色结果时触发
document.addEventListener('aiPolishAccepted', (e) => {
    console.log('Textarea ID:', e.detail.textareaId);
    console.log('原文:', e.detail.originalText);
    console.log('润色后:', e.detail.polishedText);
});

// 用户拒绝润色结果时触发
document.addEventListener('aiPolishRejected', (e) => {
    console.log('Textarea ID:', e.detail.textareaId);
});
```

---

## 当前覆盖范围

| 模块     | 页面                | 字段   | 字段 ID                      | 上下文  |
|--------|-------------------|------|----------------------------|------|
| 在线编辑器  | 简历数据管理 → 在线编辑     | 工作描述 | `editor-field-description` | 公司名称 |
| 在线编辑器  | 简历数据管理 → 在线编辑     | 项目背景 | `editor-proj-background`   | 项目名称 |
| 在线编辑器  | 简历数据管理 → 在线编辑     | 个人职责 | `editor-proj-duty`         | 项目名称 |
| 工作经历管理 | 简历数据管理 → 工作经历 TAB | 整体描述 | `exp-f-description`        | 职位名称 |
| 项目管理   | 简历数据管理 → 项目经历 TAB | 项目背景 | `proj-f-background`        | 项目名称 |
| 项目管理   | 简历数据管理 → 项目经历 TAB | 职责描述 | `proj-f-duty`              | 项目名称 |
| 成果管理   | 简历数据管理 → 项目成果管理   | 成果内容 | `ach-f-content`            | 项目名称 |

---

## 后端 API

```
POST /api/resume/ai/polish
Content-Type: application/json

{
  "text": "需要润色的文本",
  "fieldType": "description|background|duty|achievement",
  "context": "上下文信息（公司名/项目名）"
}
```

**响应格式**：

```json
{
  "success": true,
  "code": "0",
  "msg": "操作成功",
  "data": {
    "result": "润色后的文本"
  }
}
```

---

## 样式定制

覆盖以下 CSS 类可自定义样式：

```css
.ai-polish-btn {
    background: linear-gradient(135deg, #your-color1, #your-color2);
}
.ai-polish-modal {
    max-width: 1000px;
}
.ai-polish-toast {
    top: 80px;
    right: 40px;
}
```

---

## 注意事项

1. 必须引入 CSS 文件，否则按钮和弹窗没有样式
2. textarea 必须有 ID，组件通过 ID 定位元素
3. 字段类型要正确，不同的 `fieldType` 影响 AI 润色策略
4. 上下文信息可选，提供上下文可让 AI 给出更准确的建议
5. 确保每个 textarea 的 ID 唯一
6. 加载顺序：AI 组件必须在其他使用它的组件之前加载
7. 通过 `window.AiPolishComponent` 访问全局对象

---

## 常见问题

**Q: 按钮点击没反应？**
检查是否正确引入了 `ai-polish.js`，并在浏览器控制台查看是否有错误。

**Q: 弹窗样式不对？**
确认已引入 `ai-polish.css`，且没有被其他样式覆盖。

**Q: 如何修改 API 地址？**
编辑 `ai-polish.js` 中的 `fetch('/api/resume/ai/polish', ...)` 部分。

**Q: 提示"权限不足"？**
确保已登录系统且当前用户有 `resume:manage` 权限。

---

## 附录：集成记录

### v1.0.0 集成完成 (2026-07-05)

所有 AI 润色功能统一使用独立的 `AiPolishComponent` 组件，取代了旧版 `OnlineEditor.polishText()` 分散实现。

**迁移涉及文件**：

- `online-editor.js`：工作经历描述、项目背景、个人职责字段
- `experience-management.js`：工作经历 TAB 整体描述字段
- `project-management.js`：项目背景、职责描述字段
- `achievement-management.js`：项目成果内容字段

**迁移改进**：统一 CSS 类名（`ai-polish-btn`）、统一对比弹窗 UI、统一 Toast 提示、集中管理易于维护。

### 验证通过

- 工作经历 AI 润色：输入文本 → 点击按钮 → 弹出对比窗口 → 采纳/保留正常
- 项目经历 AI 润色：项目背景和个人职责字段均正常
- 项目成果 AI 润色：成果内容字段正常
- 样式一致性：所有按钮、弹窗、Toast 样式统一

---

**作者**: Brief-Wisdom
**最后更新**: 2026-07-05
