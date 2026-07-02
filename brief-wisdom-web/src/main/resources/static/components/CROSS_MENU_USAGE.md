# 组件跨菜单复用指南

## 概述

本项目的功能组件（用户管理、菜单管理、角色管理、知识库、模型管理、会话历史）已经设计为**完全独立的组件**，可以在任何菜单下复用。

## 已完成的组件列表

| 组件名称 | 文件名 | 状态 |
|---------|--------|------|
| 用户管理 | user-management.js | ✅ 已完成 |
| 菜单管理 | menu-management.js | ✅ 已完成 |
| 角色管理 | role-management.js | ✅ 已完成 |
| 知识库 | knowledge-management.js | ✅ 已完成 |
| 模型管理 | model-management.js | ✅ 已完成 |
| 会话历史 | session-history.js | ✅ 已完成 |

## 使用方式

### 方式一：在 system-settings.html 中使用（推荐）

这是最简单的方式，只需在数据库中配置菜单结构即可。

#### 步骤：

1. **在数据库中创建父级菜单**
   ```sql
   -- 例如：创建一个"系统管理"菜单作为父级
   INSERT INTO sys_menu (name, url, parent_id, type, sort_order, visible) 
   VALUES ('系统管理', '', 0, 0, 10, 1);
   
   -- 假设新创建的父级菜单 ID 为 20
   ```

2. **添加子菜单项**
   ```sql
   -- 将现有功能作为子菜单添加到新的父级菜单下
   INSERT INTO sys_menu (name, url, parent_id, type, sort_order, visible) VALUES
   ('用户管理', '', 20, 1, 1, 1),
   ('角色管理', '', 20, 1, 2, 1),
   ('菜单管理', '', 20, 1, 3, 1),
   ('模型管理', '', 20, 1, 4, 1),
   ('会话历史', '', 20, 1, 5, 1),
   ('知识库', '', 20, 1, 6, 1);
   ```

3. **刷新页面**
   - 访问 `system-settings.html`
   - 系统会自动根据菜单结构渲染 Tab
   - 点击 Tab 时会自动加载对应的组件

### 方式二：在新的 HTML 页面中使用

如果您想在一个全新的页面中使用这些组件：

#### 步骤：

1. **创建新的 HTML 页面**
   ```html
   <!DOCTYPE html>
   <html lang="zh-CN">
   <head>
       <meta charset="UTF-8">
       <title>我的管理页面</title>
       <link rel="stylesheet" href="css/navbar.css">
       <link rel="stylesheet" href="css/system-settings.css">
       <link rel="stylesheet" href="css/ai-manage.css">
   </head>
   <body>
   <div class="container">
       <header>
           <h1>我的管理页面</h1>
       </header>
       
       <!-- Tab 导航（由 initPageTabs 动态渲染） -->
       <nav class="settings-tabs"></nav>
       
       <!-- Tab 内容容器 -->
       <div id="user-tab-content" class="settings-tab-content">
           <!-- 用户管理组件会在这里初始化 -->
       </div>
       
       <div id="role-tab-content" class="settings-tab-content">
           <!-- 角色管理组件会在这里初始化 -->
       </div>
       
       <!-- 根据需要添加更多 Tab 容器 -->
   </div>
   
   <!-- 引入脚本 -->
   <script src="js/navbar.js"></script>
   <script src="components/component-loader.js"></script>
   <script src="js/system-settings-lite.js"></script>
   </body>
   </html>
   ```

2. **在数据库中配置菜单**
   ```sql
   -- 创建父级菜单，指向新的 HTML 页面
   INSERT INTO sys_menu (name, url, parent_id, type, sort_order, visible) 
   VALUES ('我的管理', 'my-page.html', 0, 0, 20, 1);
   
   -- 添加子菜单
   INSERT INTO sys_menu (name, url, parent_id, type, sort_order, visible) VALUES
   ('用户管理', '', LAST_INSERT_ID(), 1, 1, 1),
   ('角色管理', '', LAST_INSERT_ID(), 1, 2, 1);
   ```

3. **修改 system-settings-lite.js 中的 pageUrls**
   ```javascript
   initPageTabs({
       pageUrls: ['system-settings.html', 'my-page.html'], // 添加新页面
       // ... 其他配置
   });
   ```

## 组件映射配置

在 `system-settings-lite.js` 中配置了菜单名称到组件的映射：

```javascript
// 组件名称映射表（菜单名称 → 组件文件名）
const COMPONENT_NAME_MAP = {
    '菜单管理': 'menu-management',
    '用户管理': 'user-management',
    '角色管理': 'role-management',
    '知识库': 'knowledge-management',
    '模型管理': 'model-management',
    '会话历史': 'session-history'
};

// Tab ID 映射表（菜单名称 → Tab 内容容器 ID）
const TAB_ID_MAP = {
    '菜单管理': 'menu-tab-content',
    '用户管理': 'user-tab-content',
    '角色管理': 'role-tab-content',
    '知识库': 'knowledge-tab-content',
    '模型管理': 'models-tab-content',
    '会话历史': 'sessions-tab-content'
};
```

**如果需要添加新的组件映射**，只需在这两个表中添加对应的条目即可。

## 注意事项

1. **Tab 内容容器必须存在**
   - HTML 中必须有对应的 `<div id="xxx-tab-content" class="settings-tab-content">`
   - 组件会在该容器中初始化

2. **CSS 样式依赖**
   - 确保引入了必要的 CSS 文件：
     - `css/navbar.css` - 导航栏样式
     - `css/system-settings.css` - 系统设置样式
     - `css/ai-manage.css` - AI 管理和会话历史样式

3. **脚本加载顺序**
   ```html
   <script src="js/navbar.js"></script>                    <!-- 必须先加载 -->
   <script src="components/component-loader.js"></script>  <!-- 组件加载器 -->
   <script src="js/system-settings-lite.js"></script>      <!-- Tab 切换逻辑 -->
   ```

4. **数据库菜单配置**
   - 父级菜单的 `url` 字段指向 HTML 页面（如 `system-settings.html`）
   - 子菜单的 `url` 字段可以为空（因为是通过 Tab 切换，不是跳转）
   - 子菜单的 `type` 应为 `1`（菜单类型）

## 示例：创建一个新的"数据管理"菜单

假设您想创建一个"数据管理"菜单，包含"用户管理"和"角色管理"：

```sql
-- 1. 创建父级菜单
INSERT INTO sys_menu (name, url, parent_id, type, sort_order, icon, visible, require_login) 
VALUES ('数据管理', 'system-settings.html', 0, 0, 30, '📊', 1, 1);

SET @parent_id = LAST_INSERT_ID();

-- 2. 添加子菜单
INSERT INTO sys_menu (name, url, parent_id, type, sort_order, visible) VALUES
('用户管理', '', @parent_id, 1, 1, 1),
('角色管理', '', @parent_id, 1, 2, 1);
```

然后访问 `system-settings.html`，就会看到"数据管理"菜单，点击后显示包含"用户管理"和"角色管理"两个 Tab 的页面。

## 常见问题

### Q: 为什么点击 Tab 没有反应？

A: 检查以下几点：
1. 控制台是否有报错
2. 数据库菜单配置是否正确
3. HTML 中是否有对应的 Tab 内容容器
4. 是否强制刷新了浏览器（Ctrl+Shift+R）

### Q: 组件加载了但样式不对？

A: 确保引入了所有必要的 CSS 文件，特别是 `css/ai-manage.css`（包含会话历史的三栏布局样式）。

### Q: 如何让组件在新窗口中打开？

A: 这不是组件的设计目标。组件的目的是在同一页面内通过 Tab 切换来复用，而不是在新窗口中打开。如果需要独立页面，应该创建新的 HTML 文件。

## 技术实现

- **组件自动注册**：每个组件都会调用 `registerComponent()` 注册到全局
- **异步加载**：使用 `loadAndInitComponents()` 动态加载组件脚本并初始化
- **Tab 切换**：`initPageTabs()` 根据菜单数据动态渲染 Tab 按钮和内容
- **API 兼容**：所有组件都兼容 `{ success: true, data: {...} }` 和直接返回数据两种格式

---

**最后更新**: 2026-07-02  
**作者**: Brief-Wisdom
