# 组件化架构设计规范

## 概述

本项目采用**真正的组件化架构**，每个功能模块都包含三个独立的部分：

1. **HTML 模板** (`.template.js`) - 组件的 DOM 结构
2. **CSS 样式** (可选) - 组件的专属样式（如果已在全局 CSS 中则不需要）
3. **JS 逻辑** (`.js`) - 组件的业务逻辑

## 组件目录结构

```
brief-wisdom-web/src/main/resources/static/components/
├── component-loader.js                    # 组件加载器（核心）
├── user-management.template.js            # 用户管理 - HTML 模板
├── user-management.js                     # 用户管理 - JS 逻辑
├── menu-management.template.js            # 菜单管理 - HTML 模板（待创建）
├── menu-management.js                     # 菜单管理 - JS 逻辑
├── role-management.template.js            # 角色管理 - HTML 模板（待创建）
├── role-management.js                     # 角色管理 - JS 逻辑
├── model-management.template.js           # 模型管理 - HTML 模板（待创建）
├── model-management.js                    # 模型管理 - JS 逻辑
├── session-history.template.js            # 会话历史 - HTML 模板（待创建）
├── session-history.js                     # 会话历史 - JS 逻辑
└── knowledge-management.template.js       # 知识库 - HTML 模板（待创建）
    └── knowledge-management.js            # 知识库 - JS 逻辑
```

## 组件文件规范

### 1. HTML 模板文件 (`*.template.js`)

**职责**：生成组件的完整 HTML 结构

**命名规范**：`{component-name}.template.js`

**示例**：
```javascript
/**
 * 用户管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
(function() {
    'use strict';

    /**
     * 生成用户管理的完整 HTML 结构
     * @returns {string} HTML 字符串
     */
    function generateHTML() {
        return `
            <section class="settings-section">
                <!-- 组件内容 -->
            </section>
        `;
    }

    /**
     * 渲染组件到指定容器
     * @param {string|HTMLElement} container - 容器选择器或 DOM 元素
     */
    function render(container) {
        const target = typeof container === 'string' 
            ? document.querySelector(container) 
            : container;
        
        if (!target) {
            console.error('[UserManagement.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[UserManagement.Template] 模板已渲染到:', target);
    }

    // 暴露接口
    window.UserManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[UserManagement.Template] 模板生成器已注册');
})();
```

### 2. JS 逻辑文件 (`*.js`)

**职责**：处理组件的业务逻辑、数据加载、事件绑定等

**命名规范**：`{component-name}.js`

**关键要求**：
- 使用 IIFE（立即执行函数表达式）封装，避免全局污染
- 在初始化时检查并使用模板生成器自动渲染 HTML
- 通过 `window.{ComponentName}` 暴露公共接口
- 调用 `registerComponent()` 自动注册到组件系统

**示例**：
```javascript
(function() {
    'use strict';

    const API_BASE = '/api/user';

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        const containerId = options.containerId || 'user-tab-content';
        console.log('[UserManagement] 初始化，容器:', containerId);

        // 如果提供了容器且模板生成器可用，先渲染 HTML 模板
        const container = document.getElementById(containerId);
        if (container && window.UserManagementTemplate) {
            console.log('[UserManagement] 使用模板生成器渲染 HTML');
            UserManagementTemplate.render(container);
        }

        // 加载数据
        await loadData();
    }

    // 暴露全局接口
    window.UserManagement = {
        init,
        // ... 其他公共方法
    };

    // 自动注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('user-management', window.UserManagement);
    }

    console.log('[UserManagement] 组件已注册');
})();
```

### 3. CSS 样式文件（可选）

**原则**：
- **优先使用全局 CSS**：如果样式已在 `system-settings.css` 或 `ai-manage.css` 中定义，不需要单独创建
- **组件专属样式**：只有在需要特殊样式时才创建独立的 `.css` 文件
- **命名空间**：使用组件名前缀避免冲突（如 `.user-management-container`）

## 页面中使用组件

### 方式一：最小化 HTML（推荐）

**HTML 页面只需提供空容器**：
```html
<div id="user-tab-content" class="settings-tab-content">
    <!-- 组件会自动渲染到这里 -->
</div>

<!-- 引入组件 -->
<script src="components/component-loader.js"></script>
<script src="components/user-management.template.js"></script>
<script src="components/user-management.js"></script>
```

**优点**：
- ✅ HTML 简洁，无重复代码
- ✅ 一处修改，所有页面生效
- ✅ 真正的组件复用

### 方式二：预定义 HTML（兼容旧代码）

如果 HTML 中已经硬编码了组件结构，组件会直接使用现有 DOM，不会重复渲染。

## 组件加载流程

```
1. 页面加载 → 加载 component-loader.js
2. 加载 *.template.js → 注册模板生成器到 window
3. 加载 *.js → 注册组件到 componentRegistry
4. Tab 切换 → 调用 loadAndInitComponents()
5. 组件初始化 → 检查模板生成器并渲染 HTML
6. 加载数据 → 调用 API 获取数据并渲染
```

## 迁移指南

### 将现有功能迁移到组件化架构

#### 步骤 1：提取 HTML 模板

从 HTML 页面中复制组件的 HTML 结构，创建 `{component-name}.template.js`：

```javascript
function generateHTML() {
    return `
        <!-- 从这里粘贴 HTML -->
    `;
}
```

#### 步骤 2：更新 JS 逻辑

在 `init()` 函数中添加模板渲染逻辑：

```javascript
async function init(options = {}) {
    const container = document.getElementById(options.containerId || 'xxx-tab-content');
    if (container && window.XxxTemplate) {
        XxxTemplate.render(container);
    }
    // ... 原有逻辑
}
```

#### 步骤 3：清理 HTML 页面

删除 HTML 页面中的硬编码组件结构，只保留空容器：

```html
<!-- 之前：几百行硬编码 HTML -->
<div id="xxx-tab-content">
    <section>...</section>
    <table>...</table>
    <!-- ... -->
</div>

<!-- 之后：只有空容器 -->
<div id="xxx-tab-content" class="settings-tab-content"></div>
```

#### 步骤 4：更新页面脚本

确保页面引入了模板文件和组件文件：

```html
<script src="components/component-loader.js"></script>
<script src="components/xxx-management.template.js"></script>
<script src="components/xxx-management.js"></script>
```

## 已完成组件清单

| 组件名称 | 模板文件 | JS 文件 | 状态 |
|---------|---------|--------|------|
| 用户管理 | user-management.template.js | user-management.js | ✅ 已完成 |
| 菜单管理 | ❌ 待创建 | menu-management.js | ⚠️ 部分完成 |
| 角色管理 | ❌ 待创建 | role-management.js | ⚠️ 部分完成 |
| 知识库 | ❌ 待创建 | knowledge-management.js | ⚠️ 部分完成 |
| 模型管理 | ❌ 待创建 | model-management.js | ⚠️ 部分完成 |
| 会话历史 | ❌ 待创建 | session-history.js | ⚠️ 部分完成 |

## 待创建组件清单

需要为以下组件创建模板文件：
- [ ] menu-management.template.js
- [ ] role-management.template.js
- [ ] knowledge-management.template.js
- [ ] model-management.template.js
- [ ] session-history.template.js

## 最佳实践

1. **单一职责**：每个组件只负责一个功能模块
2. **自包含**：组件应该能独立工作，不依赖页面的其他部分
3. **可配置**：通过 `options` 参数支持自定义行为
4. **生命周期**：提供 `init()` 和 `destroy()` 方法
5. **错误处理**：所有异步操作都要有错误处理和用户提示
6. **日志输出**：关键步骤添加控制台日志，方便调试

## 常见问题

### Q: 为什么需要单独的模板文件？

A: 将 HTML 结构抽取成模板文件可以：
- 避免在每个页面中复制相同的 HTML
- 一处修改，所有使用该组件的页面自动生效
- 更容易维护和测试

### Q: CSS 样式如何处理？

A: 
- 通用样式（如表格、按钮、弹窗）放在全局 CSS 文件中
- 组件专属样式可以放在组件的 `.css` 文件中（如果需要）
- 大多数情况下，现有的全局 CSS 已经足够

### Q: 如何在不支持 JavaScript 的环境中降级？

A: 可以在 HTML 中提供静态内容作为降级方案：

```html
<div id="user-tab-content" class="settings-tab-content">
    <noscript>
        <p>请启用 JavaScript 以使用用户管理功能</p>
    </noscript>
</div>
```

---

**最后更新**: 2026-07-02  
**作者**: Brief-Wisdom
