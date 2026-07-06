# AI 润色组件使用文档

## 📦 组件说明

AI 润色组件是一个独立、可复用的前端组件，为任何文本输入框提供 AI 智能润色功能。

### 核心特性

- ✨ **一键润色**：点击按钮即可调用 AI 优化文本
- 🎯 **智能对比**：左右分栏展示原文和润色结果
- 🔄 **灵活选择**：用户可选择采纳或拒绝 AI 建议
- 📱 **响应式设计**：适配桌面和移动端
- 🎨 **美观 UI**：渐变按钮、动画效果、Toast 提示
- 🔌 **易于集成**：只需引入 3 个文件即可使用

## 📁 组件文件

```
brief-wisdom-web/src/main/resources/static/
├── components/
│   ├── ai-polish.template.js    # 模板生成器（可选）
│   └── ai-polish.js             # 业务逻辑（必需）
└── css/
    └── ai-polish.css            # 样式文件（必需）
```

## 🚀 快速开始

### 1. 引入文件

在 HTML 页面的 `<head>` 中引入 CSS，在 `<body>` 底部引入 JS：

```html
<!DOCTYPE html>
<html>
<head>
    <!-- 引入 AI 润色组件样式 -->
    <link rel="stylesheet" href="/css/ai-polish.css">
</head>
<body>
    <!-- 你的页面内容 -->
    
    <!-- 引入 AI 润色组件脚本（放在 body 底部） -->
    <script src="/components/ai-polish.js"></script>
</body>
</html>
```

### 2. 使用方法一：手动添加按钮

在你的 textarea 旁边手动添加 AI 润色按钮：

```html
<div class="form-group">
    <label>描述</label>
    <div class="form-field-with-ai">
        <textarea id="my-textarea" rows="4">需要润色的文本</textarea>
        <button type="button" 
                class="ai-polish-btn" 
                onclick="AiPolishComponent.polish('my-textarea', 'description', '公司名称')" 
                title="AI润色">
            ✨ AI
        </button>
    </div>
</div>
```

**参数说明**：
- `textareaId`: textarea 的 ID（如 `'my-textarea'`）
- `fieldType`: 字段类型（`description` / `background` / `duty` / `achievement`）
- `context`: 上下文信息（可选，如公司名、项目名）

### 3. 使用方法二：使用模板生成器（推荐）

如果引入了 `ai-polish.template.js`，可以使用辅助函数生成 HTML：

```javascript
// 生成带 AI 润色按钮的 textarea
const html = AiPolishComponent.generateTextareaWithPolish({
    id: 'my-textarea',
    name: 'description',
    value: '需要润色的文本',
    rows: 4,
    placeholder: '请输入描述',
    required: true,
    fieldType: 'description',
    context: '测试公司',
    label: '工作描述'
});

// 插入到页面
document.getElementById('container').innerHTML = html;
```

### 4. 使用方法三：批量初始化

如果页面上已有多个 `.ai-polish-wrapper` 元素，可以自动初始化：

```html
<!-- HTML -->
<div class="ai-polish-wrapper" 
     data-textarea-id="textarea-1" 
     data-field-type="description" 
     data-context="公司A">
    <textarea id="textarea-1"></textarea>
    <button class="ai-polish-btn">✨ AI</button>
</div>

<div class="ai-polish-wrapper" 
     data-textarea-id="textarea-2" 
     data-field-type="background" 
     data-context="项目B">
    <textarea id="textarea-2"></textarea>
    <button class="ai-polish-btn">✨ AI</button>
</div>

<script>
// JavaScript - 自动绑定所有按钮
AiPolishComponent.initPage();
</script>
```

## 💡 使用示例

### 示例 1：工作经历编辑

```html
<div class="form-group">
    <label>工作描述</label>
    <div class="form-field-with-ai">
        <textarea id="exp-description" rows="4" placeholder="请输入工作描述">
负责开发和维护 Java 后端系统
        </textarea>
        <button type="button" 
                class="ai-polish-btn" 
                onclick="AiPolishComponent.polish('exp-description', 'description', '阿里巴巴')" 
                title="AI润色">
            ✨ AI
        </button>
    </div>
</div>
```

### 示例 2：项目背景编辑

```html
<div class="form-group">
    <label>项目背景</label>
    <div class="form-field-with-ai">
        <textarea id="proj-background" rows="3">电商平台重构项目</textarea>
        <button type="button" 
                class="ai-polish-btn" 
                onclick="AiPolishComponent.polish('proj-background', 'background', '电商中台')" 
                title="AI润色">
            ✨ AI
        </button>
    </div>
</div>
```

### 示例 3：个人职责编辑

```html
<div class="form-group">
    <label>个人职责</label>
    <div class="form-field-with-ai">
        <textarea id="proj-duty" rows="3">负责后端接口开发</textarea>
        <button type="button" 
                class="ai-polish-btn" 
                onclick="AiPolishComponent.polish('proj-duty', 'duty', '订单系统')" 
                title="AI润色">
            ✨ AI
        </button>
    </div>
</div>
```

### 示例 4：项目成果编辑

```html
<div class="form-group">
    <label>成果内容</label>
    <div class="form-field-with-ai">
        <textarea id="ach-content" rows="4">提升了系统性能</textarea>
        <button type="button" 
                class="ai-polish-btn" 
                onclick="AiPolishComponent.polish('ach-content', 'achievement', '性能优化项目')" 
                title="AI润色">
            ✨ AI
        </button>
    </div>
</div>
```

## 🔧 API 参考

### AiPolishComponent.polish(textareaId, fieldType, context)

触发 AI 润色的核心函数。

**参数**：
- `textareaId` (string): 目标 textarea 的 DOM ID
- `fieldType` (string): 字段类型
  - `description`: 工作经历描述
  - `background`: 项目背景
  - `duty`: 个人职责
  - `achievement`: 项目成果
- `context` (string, 可选): 上下文信息，帮助 AI 更好地理解语境

**示例**：
```javascript
AiPolishComponent.polish('my-textarea', 'description', '腾讯科技');
```

### AiPolishComponent.showComparisonModal(originalText, polishedText, targetTextarea)

手动显示对比弹窗（高级用法）。

**参数**：
- `originalText` (string): 原始文本
- `polishedText` (string): 润色后的文本
- `targetTextarea` (HTMLElement): 目标 textarea 元素

**示例**：
```javascript
const textarea = document.getElementById('my-textarea');
AiPolishComponent.showComparisonModal(
    '原文本',
    '润色后的文本',
    textarea
);
```

### AiPolishComponent.showToast(message, type)

显示轻量级提示消息。

**参数**：
- `message` (string): 提示消息
- `type` (string): 类型（`success` / `info` / `warning` / `error`）

**示例**：
```javascript
AiPolishComponent.showToast('操作成功', 'success');
AiPolishComponent.showToast('请注意', 'warning');
```

### AiPolishComponent.initPage()

批量初始化页面上的所有 AI 润色按钮。

**示例**：
```javascript
// 页面加载完成后调用
document.addEventListener('DOMContentLoaded', () => {
    AiPolishComponent.initPage();
});
```

### AiPolishComponent.generateTextareaWithPolish(options)

生成带 AI 润色按钮的 textarea HTML（需引入 template.js）。

**参数**：
```javascript
{
    id: string,           // textarea ID（必需）
    name: string,         // name 属性（默认同 id）
    value: string,        // 初始值
    rows: number,         // 行数（默认 4）
    placeholder: string,  // 占位符
    required: boolean,    // 是否必填
    fieldType: string,    // 字段类型（必需）
    context: string,      // 上下文信息
    label: string         // 标签文本
}
```

**示例**：
```javascript
const html = AiPolishComponent.generateTextareaWithPolish({
    id: 'desc',
    value: '文本内容',
    fieldType: 'description',
    context: '公司名',
    label: '工作描述'
});
```

## 🎯 自定义事件

组件会触发自定义事件，方便其他组件监听用户操作：

### aiPolishAccepted

当用户采纳 AI 润色结果时触发。

```javascript
document.addEventListener('aiPolishAccepted', (e) => {
    console.log('用户采纳了润色结果');
    console.log('Textarea ID:', e.detail.textareaId);
    console.log('原文:', e.detail.originalText);
    console.log('润色后:', e.detail.polishedText);
    
    // 可以在这里执行额外操作，如自动保存
});
```

### aiPolishRejected

当用户拒绝 AI 润色结果时触发。

```javascript
document.addEventListener('aiPolishRejected', (e) => {
    console.log('用户保留了原文');
    console.log('Textarea ID:', e.detail.textareaId);
});
```

## 🎨 样式定制

如果需要自定义样式，可以覆盖以下 CSS 类：

```css
/* 自定义按钮样式 */
.ai-polish-btn {
    background: linear-gradient(135deg, #your-color1, #your-color2);
}

/* 自定义弹窗宽度 */
.ai-polish-modal {
    max-width: 1000px;
}

/* 自定义 Toast 位置 */
.ai-polish-toast {
    top: 80px;
    right: 40px;
}
```

## ⚠️ 注意事项

1. **必须引入 CSS**：否则按钮和弹窗没有样式
2. **textarea 必须有 ID**：组件通过 ID 定位元素
3. **字段类型要正确**：不同的 `fieldType` 会影响 AI 的润色策略
4. **上下文信息可选**：提供上下文可以让 AI 给出更准确的建议
5. **避免重复 ID**：确保每个 textarea 的 ID 唯一

## 🐛 常见问题

### Q: 按钮点击没反应？
A: 检查是否正确引入了 `ai-polish.js`，并在浏览器控制台查看是否有错误。

### Q: 弹窗样式不对？
A: 确认已引入 `ai-polish.css`，且没有被其他样式覆盖。

### Q: 如何修改 API 地址？
A: 编辑 `ai-polish.js` 中的 `fetch('/api/resume/ai/polish', ...)` 部分。

### Q: 支持其他语言的 AI 服务吗？
A: 只要后端接口返回格式一致（`{data: {result: "xxx"}}`），就可以直接使用。

## 📝 更新日志

### v1.0.0 (2026-07-05)
- ✨ 初始版本发布
- ✨ 支持四种字段类型的 AI 润色
- ✨ 美观的对比弹窗
- ✨ Toast 提示功能
- ✨ 自定义事件支持
- ✨ 批量初始化功能

---

**作者**: Brief-Wisdom  
**最后更新**: 2026-07-05
