# 前端组件化架构

> **说明**: 本目录包含系统的前端组件,采用 HTML 模板 + JS 逻辑分离的组件化架构。

---

## 📦 组件列表

### 系统管理组件

| 组件名  | 文件                   | 功能                |
|------|----------------------|-------------------|
| 用户管理 | `user-management.js` | 用户 CRUD、级别修改、角色分配 |
| 角色管理 | `role-management.js` | 角色 CRUD、菜单权限分配    |
| 菜单管理 | `menu-management.js` | 树形菜单配置、显示控制       |

### AI 管理组件

| 组件名     | 文件                    | 功能                 |
|---------|-----------------------|--------------------|
| AI 模型管理 | `model-management.js` | 模型 CRUD、启用/禁用、激活切换 |
| 会话历史    | `session-history.js`  | 按用户/级别查询会话         |
| 费用统计    | `cost-statistics.js`  | 多维度费用分析、可视化图表      |
| 审计日志    | `audit-log.js`        | AI 交互审计、统计分析       |

### 简历管理组件

| 组件名   | 文件                          | 功能          |
|-------|-----------------------------|-------------|
| 工作经历  | `experience-management.js`  | 工作经历 CRUD   |
| 项目经历  | `project-management.js`     | 项目 CRUD     |
| 项目成果  | `achievement-management.js` | 成果 CRUD     |
| 技术栈   | `tech-stack-management.js`  | 技术栈 CRUD    |
| 在线编辑器 | `online-editor.js`          | 分步式编辑、AI 润色 |

### 其他组件

| 组件名   | 文件                         | 功能            |
|-------|----------------------------|---------------|
| AI 助手 | `ai-assistant.template.js` | AI 聊天界面模板     |
| 知识库管理 | `knowledge-management.js`  | 知识库 CRUD、文档上传 |
| 组件加载器 | `component-loader.js`      | 统一注册和初始化组件    |

---

## 🔧 组件加载器

所有组件通过 `component-loader.js` 进行统一管理:

```javascript
// 注册组件
registerComponent('user-management', UserManagement);

// 初始化组件
initComponent('user-management', { containerId: 'user-tab-content' });

// 批量加载并初始化
await loadAndInitComponents([
    { name: 'user-management' },
    { name: 'role-management' }
]);
```

---

## 📝 使用方法

### 在页面中使用组件

#### 步骤 1:引入组件加载器

```html
<script src="components/component-loader.js"></script>
```

#### 步骤 2:准备容器 HTML

```html
<div id="user-tab-content">
    <!-- 组件会自动渲染到这里 -->
</div>
```

#### 步骤 3:加载并初始化组件

```javascript
document.addEventListener('DOMContentLoaded', async () => {
    await loadAndInitComponents([{ name: 'user-management' }]);
});
```

---

## 🎯 组件优势

1. **高复用性** - 同一组件可在多个页面/菜单下使用
2. **低耦合** - 组件之间互不依赖,可独立开发和维护
3. **易扩展** - 新增功能只需修改对应组件,不影响其他部分
4. **按需加载** - 只在需要时加载组件,提升性能
5. **HTML 与 JS 分离** - 模板文件负责生成 HTML,JS 文件负责业务逻辑

---

## 📚 详细文档

完整的组件架构设计和使用示例请参考:

- **[docs/architecture/component-architecture.md](../../../docs/architecture/component-architecture.md)** - 组件化架构设计
- **[docs/guides/developer-guide.md](../../../docs/guides/developer-guide.md)** - 开发者快速入门

---

## ⚠️ 注意事项

1. 确保页面中包含组件所需的 HTML 结构(表格、弹窗等)
2. 组件依赖全局函数:`showToast`, `showConfirmDialog`, `hasPermission`(由 navbar.js 提供)
3. 组件会自动应用权限控制,无需手动处理
4. 如需自定义样式,请在 CSS 文件中覆盖相应类名

---

**最后更新**: 2026-07-06
