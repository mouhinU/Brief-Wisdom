# 前端组件化使用说明

## 📦 组件列表

当前已抽取的组件：

1. **用户管理组件** - `components/user-management.js`
2. **菜单管理组件** - `components/menu-management.js`
3. **角色管理组件** - `components/role-management.js`

## 🔧 组件加载器

所有组件通过 `components/component-loader.js` 进行统一管理，提供以下功能：

- `registerComponent(name, component)` - 注册组件
- `initComponent(name, options)` - 初始化组件
- `loadComponentScript(componentName)` - 动态加载组件脚本
- `loadAndInitComponents(components)` - 批量加载并初始化组件

## 📝 使用方法

### 方法一：在现有页面中使用（推荐）

#### 步骤 1：引入组件加载器

```html
<!-- 在页面底部引入 -->
<script src="components/component-loader.js"></script>
```

#### 步骤 2：准备容器 HTML

```html
<!-- 用户管理 -->
<div id="user-tab-content">
    <section class="settings-section">
        <div class="section-header">
            <h2>用户管理</h2>
            <div class="user-filter-bar">
                <select id="user-level-filter" onchange="UserManagement.loadUsers()">
                    <option value="">全部级别</option>
                    <option value="admin">管理员</option>
                    <option value="vip">会员</option>
                    <option value="normal">普通用户</option>
                </select>
                <input type="text" id="user-search-input" placeholder="搜索用户名/昵称"
                       onkeydown="if(event.key==='Enter')UserManagement.loadUsers()">
                <button class="btn btn-primary btn-sm" onclick="UserManagement.loadUsers()">搜索</button>
            </div>
        </div>
        <div class="table-wrapper">
            <table class="data-table">
                <thead>
                <tr>
                    <th>用户名</th>
                    <th>昵称</th>
                    <th>级别</th>
                    <th>注册时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody id="user-table-body"></tbody>
            </table>
        </div>
        <!-- 分页 -->
        <div class="pagination-bar" id="user-pagination"></div>
    </section>
    
    <!-- 用户级别修改弹窗 -->
    <div id="user-level-modal" class="modal" style="display:none;">
        <!-- ... 弹窗内容 ... -->
    </div>
    
    <!-- 用户角色分配弹窗 -->
    <div id="user-role-modal" class="modal" style="display:none;">
        <!-- ... 弹窗内容 ... -->
    </div>
</div>
```

#### 步骤 3：加载并初始化组件

```javascript
// 在页面 JS 中
document.addEventListener('DOMContentLoaded', async () => {
    // 加载用户管理组件
    await loadAndInitComponents([{ name: 'user-management' }]);
});
```

### 方法二：创建新页面使用组件

#### 示例：创建独立的用户管理页面

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户管理</title>
    <link rel="stylesheet" href="css/navbar.css">
    <link rel="stylesheet" href="css/system-settings.css">
</head>
<body>
<div class="container">
    <header><h1>用户管理</h1></header>
    
    <!-- 用户管理内容区域 -->
    <div id="user-tab-content">
        <!-- 同上，包含表格、筛选器等 -->
    </div>
    
    <!-- 弹窗 -->
    <div id="user-level-modal" class="modal" style="display:none;">
        <!-- ... -->
    </div>
    <div id="user-role-modal" class="modal" style="display:none;">
        <!-- ... -->
    </div>
</div>

<script src="js/navbar.js"></script>
<script src="components/component-loader.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', async () => {
        await loadAndInitComponents([{ name: 'user-management' }]);
    });
</script>
</body>
</html>
```

## 🔄 将角色管理放到其他菜单下

假设要将"角色管理"从"系统设置"移到新的"权限管理"菜单下：

### 步骤 1：在数据库中添加新菜单

```sql
-- 添加"权限管理"目录
INSERT INTO sys_menu (parent_id, name, url, type, sort_order, is_visible) 
VALUES (0, '权限管理', NULL, 0, 10, 1);

-- 获取刚插入的 ID（假设为 100）
-- 添加"角色管理"子菜单
INSERT INTO sys_menu (parent_id, name, url, type, sort_order, is_visible, require_login) 
VALUES (100, '角色管理', '/role-manage.html', 1, 1, 1, 1);
```

### 步骤 2：创建 role-manage.html 页面

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>角色管理</title>
    <link rel="stylesheet" href="css/navbar.css">
    <link rel="stylesheet" href="css/system-settings.css">
</head>
<body>
<div class="container">
    <header><h1>角色管理</h1></header>
    
    <div id="role-tab-content">
        <section class="settings-section">
            <div class="section-header">
                <h2>角色管理</h2>
                <button class="btn btn-primary" onclick="RoleManagement.showRoleForm()">+ 新增角色</button>
            </div>
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>角色名称</th>
                        <th>角色标识</th>
                        <th>描述</th>
                        <th>状态</th>
                        <th>创建时间</th>
                        <th>操作</th>
                    </tr>
                    </thead>
                    <tbody id="role-table-body"></tbody>
                </table>
            </div>
        </section>
    </div>
    
    <!-- 角色表单弹窗 -->
    <div id="role-modal" class="modal" style="display:none;">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="role-modal-title">新增角色</h3>
                <button class="modal-close" onclick="RoleManagement.closeRoleModal()">×</button>
            </div>
            <form id="role-form" class="modal-form" onsubmit="RoleManagement.saveRole(event)">
                <input type="hidden" id="role-id">
                <div class="form-group">
                    <label>角色名称 *</label>
                    <input type="text" id="role-name" required placeholder="例如：超级管理员">
                </div>
                <div class="form-group">
                    <label>角色标识 *</label>
                    <input type="text" id="role-key" required placeholder="例如：super_admin">
                </div>
                <div class="form-group">
                    <label>描述</label>
                    <input type="text" id="role-desc" placeholder="角色描述">
                </div>
                <div class="form-group">
                    <label>状态</label>
                    <select id="role-status">
                        <option value="1">启用</option>
                        <option value="0">禁用</option>
                    </select>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" onclick="RoleManagement.closeRoleModal()">取消</button>
                    <button type="submit" class="btn btn-primary">保存</button>
                </div>
            </form>
        </div>
    </div>
    
    <!-- 角色菜单权限弹窗 -->
    <div id="menu-perm-modal" class="modal" style="display:none;">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="menu-perm-title">分配菜单权限</h3>
                <button class="modal-close" onclick="RoleManagement.closeMenuPermModal()">×</button>
            </div>
            <div class="modal-form">
                <div class="menu-tree-actions">
                    <button type="button" class="btn btn-sm btn-secondary" onclick="RoleManagement.checkAllMenus(true)">全选</button>
                    <button type="button" class="btn btn-sm btn-secondary" onclick="RoleManagement.checkAllMenus(false)">取消全选</button>
                </div>
                <div id="menu-perm-tree" class="menu-perm-tree"></div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" onclick="RoleManagement.closeMenuPermModal()">取消</button>
                    <button type="button" class="btn btn-primary" onclick="RoleManagement.saveMenuPermissions()">保存</button>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="js/navbar.js"></script>
<script src="components/component-loader.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', async () => {
        await loadAndInitComponents([{ name: 'role-management' }]);
    });
</script>
</body>
</html>
```

### 步骤 3：完成！

现在"角色管理"可以在两个地方访问：
- ✅ 系统设置 → 角色管理（Tab）
- ✅ 权限管理 → 角色管理（独立页面）

两个地方使用的是**同一个组件**，功能完全一致！

## 🎯 组件优势

1. **高复用性** - 同一组件可在多个页面/菜单下使用
2. **低耦合** - 组件之间互不依赖，可独立开发和维护
3. **易扩展** - 新增功能只需修改对应组件，不影响其他部分
4. **按需加载** - 只在需要时加载组件，提升性能
5. **统一接口** - 所有组件遵循相同的注册和初始化规范

## 📚 组件 API

每个组件都暴露了公共方法供外部调用：

### UserManagement
- `loadUsers(page)` - 加载用户列表
- `showUserLevelModal(id, level)` - 显示修改级别弹窗
- `resetUserPassword(id, username)` - 重置密码
- `deleteUser(id, username)` - 删除用户
- `showUserRoleModal(userId, username)` - 显示角色分配弹窗

### MenuManagement
- `loadMenus()` - 加载菜单列表
- `showMenuForm(menu?)` - 显示新增/编辑表单
- `saveMenu(e)` - 保存菜单
- `deleteMenu(id)` - 删除菜单
- `toggleVisible(id)` - 切换显示状态

### RoleManagement
- `loadRoles()` - 加载角色列表
- `showRoleForm(role?)` - 显示新增/编辑表单
- `saveRole(e)` - 保存角色
- `deleteRole(id, name)` - 删除角色
- `showMenuPermModal(roleId, roleName)` - 显示菜单权限分配
- `checkAllMenus(checked)` - 全选/取消全选菜单

## ⚠️ 注意事项

1. 确保页面中包含组件所需的 HTML 结构（表格、弹窗等）
2. 组件依赖全局函数：`showToast`, `showConfirmDialog`, `hasPermission`（由 navbar.js 提供）
3. 组件会自动应用权限控制，无需手动处理
4. 如需自定义样式，请在 CSS 文件中覆盖相应类名
