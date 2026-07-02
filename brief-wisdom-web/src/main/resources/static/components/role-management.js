/**
 * 角色管理组件
 * 可独立使用，也可嵌入到任意页面
 */

(function() {
    'use strict';

    const ROLE_API = '/api/role';
    const MENU_API = '/api/menu';

    // 当前操作的角色ID
    let currentPermRoleId = null;

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        const containerId = options.containerId || 'role-tab-content';
        console.log('[RoleManagement] 初始化，容器:', containerId);

        // 如果提供了容器且模板生成器可用，先渲染 HTML 模板
        const container = document.getElementById(containerId);
        if (container && window.RoleManagementTemplate) {
            console.log('[RoleManagement] 使用模板生成器渲染 HTML');
            RoleManagementTemplate.render(container);
        }

        await loadRoles();
        
        setTimeout(() => {
            applyPermissions();
        }, 500);
    }

    /**
     * 加载角色列表
     */
    async function loadRoles() {
        try {
            const res = await fetch(`${ROLE_API}/list`);
            const result = await res.json();
            if (!result.success) {
                showToast('加载角色失败: ' + result.error, 'error');
                return;
            }
            renderRoleTable(result.data);
        } catch (err) {
            console.error('加载角色异常:', err);
            showToast('加载角色异常', 'error');
        }
    }

    /**
     * 渲染角色表格
     */
    function renderRoleTable(roles) {
        const tbody = document.getElementById('role-table-body');
        if (!tbody) return;

        if (!roles || roles.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-state">暂无角色数据</td></tr>';
            return;
        }

        tbody.innerHTML = roles.map(r => {
            const statusBadge = r.status === 1
                ? '<span class="badge badge-show">启用</span>'
                : '<span class="badge badge-hide">禁用</span>';
            const time = r.createTime ? formatDateTime(r.createTime) : '-';
            const canManage = typeof hasPermission === 'function' && hasPermission('role:manage');
            
            const actionBtns = canManage ? `
                <button class="btn btn-edit" onclick='RoleManagement.showRoleForm(${JSON.stringify(r)})'>编辑</button>
                <button class="btn btn-role" onclick="RoleManagement.showMenuPermModal(${r.id}, '${escapeHtml(r.roleName)}')">菜单权限</button>
                <button class="btn btn-delete" onclick="RoleManagement.deleteRole(${r.id}, '${escapeHtml(r.roleName)}')">删除</button>
            ` : '<span class="no-perm-hint">无操作权限</span>';

            return `
                <tr>
                    <td><strong>${escapeHtml(r.roleName)}</strong></td>
                    <td><code>${escapeHtml(r.roleKey)}</code></td>
                    <td>${escapeHtml(r.description) || '-'}</td>
                    <td>${statusBadge}</td>
                    <td>${time}</td>
                    <td>
                        <div class="actions">
                            ${actionBtns}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    /**
     * 显示新增/编辑角色表单
     */
    function showRoleForm(role) {
        const modal = document.getElementById('role-modal');
        if (!modal) return;

        if (role) {
            document.getElementById('role-modal-title').textContent = '编辑角色';
            document.getElementById('role-id').value = role.id;
            document.getElementById('role-name').value = role.roleName;
            document.getElementById('role-key').value = role.roleKey;
            document.getElementById('role-desc').value = role.description || '';
            document.getElementById('role-status').value = String(role.status);
        } else {
            document.getElementById('role-modal-title').textContent = '新增角色';
            document.getElementById('role-id').value = '';
            document.getElementById('role-name').value = '';
            document.getElementById('role-key').value = '';
            document.getElementById('role-desc').value = '';
            document.getElementById('role-status').value = '1';
        }

        modal.style.display = 'flex';
    }

    /**
     * 关闭角色弹窗
     */
    function closeRoleModal() {
        const modal = document.getElementById('role-modal');
        if (modal) modal.style.display = 'none';
    }

    /**
     * 保存角色
     */
    async function saveRole(e) {
        e.preventDefault();
        const id = document.getElementById('role-id').value;
        const payload = {
            roleName: document.getElementById('role-name').value.trim(),
            roleKey: document.getElementById('role-key').value.trim(),
            description: document.getElementById('role-desc').value.trim(),
            status: parseInt(document.getElementById('role-status').value)
        };

        try {
            const isEdit = !!id;
            const method = isEdit ? 'PUT' : 'POST';
            if (isEdit) payload.id = parseInt(id);

            const res = await fetch(ROLE_API, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (!result.success) {
                showToast('保存失败: ' + result.error, 'error');
                return;
            }
            closeRoleModal();
            loadRoles();
            showToast('保存成功', 'success');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 删除角色
     */
    async function deleteRole(id, name) {
        if (typeof showConfirmDialog === 'function') {
            if (!await showConfirmDialog(`确定要删除角色"${name}"吗？`, '🗑️')) return;
        }

        try {
            const res = await fetch(`${ROLE_API}/${id}`, { method: 'DELETE' });
            const result = await res.json();
            if (!result.success) {
                showToast('删除失败: ' + result.error, 'error');
                return;
            }
            loadRoles();
            showToast('删除成功', 'success');
        } catch (err) {
            showToast('删除异常: ' + err.message, 'error');
        }
    }

    /**
     * 显示菜单权限分配弹窗
     */
    async function showMenuPermModal(roleId, roleName) {
        currentPermRoleId = roleId;
        const modal = document.getElementById('menu-perm-modal');
        if (!modal) return;

        document.getElementById('menu-perm-title').textContent = `分配菜单权限 - ${roleName}`;

        try {
            // 并行加载菜单树和角色已选菜单
            const [treeRes, menuIdsRes] = await Promise.all([
                fetch(`${MENU_API}/all/tree`),
                fetch(`${ROLE_API}/${roleId}/menus`)
            ]);
            const treeResult = await treeRes.json();
            const menuIdsResult = await menuIdsRes.json();

            if (!treeResult.success || !menuIdsResult.success) {
                showToast('加载菜单数据失败', 'error');
                return;
            }

            const checkedIds = new Set(menuIdsResult.data);
            renderMenuPermTree(treeResult.data, checkedIds);
            modal.style.display = 'flex';
        } catch (err) {
            showToast('加载菜单数据异常', 'error');
        }
    }

    /**
     * 渲染菜单权限树
     */
    function renderMenuPermTree(nodes, checkedIds, container) {
        const root = container || document.getElementById('menu-perm-tree');
        if (!root) return;
        
        root.innerHTML = '';
        renderMenuPermNodes(nodes, checkedIds, root, 0);
    }

    /**
     * 递归渲染菜单节点
     */
    function renderMenuPermNodes(nodes, checkedIds, container, depth) {
        if (!nodes || nodes.length === 0) return;
        
        nodes.forEach(node => {
            const indent = depth * 24;
            const checked = checkedIds.has(node.id) ? 'checked' : '';
            const typeLabel = node.type === 0 ? '[目录]' : node.type === 2 ? '[按钮]' : '';

            const item = document.createElement('div');
            item.className = 'menu-perm-item';
            item.style.paddingLeft = indent + 'px';
            item.innerHTML = `
                <label class="menu-perm-label">
                    <input type="checkbox" class="menu-perm-cb" value="${node.id}" ${checked}>
                    <span class="menu-perm-icon">${node.icon || ''}</span>
                    <span class="menu-perm-name">${escapeHtml(node.name)}</span>
                    ${typeLabel ? `<span class="menu-perm-type">${typeLabel}</span>` : ''}
                </label>
            `;
            container.appendChild(item);

            if (node.children && node.children.length > 0) {
                renderMenuPermNodes(node.children, checkedIds, container, depth + 1);
            }
        });
    }

    /**
     * 全选/取消全选菜单
     */
    function checkAllMenus(checked) {
        document.querySelectorAll('.menu-perm-cb').forEach(cb => {
            cb.checked = checked;
        });
    }

    /**
     * 关闭菜单权限弹窗
     */
    function closeMenuPermModal() {
        const modal = document.getElementById('menu-perm-modal');
        if (modal) modal.style.display = 'none';
        currentPermRoleId = null;
    }

    /**
     * 保存菜单权限
     */
    async function saveMenuPermissions() {
        if (!currentPermRoleId) return;
        
        const checkedIds = Array.from(
            document.querySelectorAll('.menu-perm-cb:checked')
        ).map(cb => parseInt(cb.value));

        try {
            const res = await fetch(`${ROLE_API}/${currentPermRoleId}/menus`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(checkedIds)
            });
            const result = await res.json();
            if (!result.success) {
                showToast('保存失败: ' + result.error, 'error');
                return;
            }
            closeMenuPermModal();
            showToast('菜单权限已保存', 'success');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 应用权限控制
     */
    function applyPermissions() {
        if (typeof hasPermission !== 'function') return;

        if (!hasPermission('role:manage')) {
            document.querySelectorAll('#role-tab-content .btn-primary').forEach(btn => btn.style.display = 'none');
            document.querySelectorAll('#role-tab-content .btn-edit, #role-tab-content .btn-role, #role-tab-content .btn-delete').forEach(btn => btn.style.display = 'none');
        }
    }

    /**
     * HTML 转义
     */
    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * 格式化日期时间
     */
    function formatDateTime(timeInput) {
        if (!timeInput) return '-';
        let date;
        if (typeof timeInput === 'string') {
            date = new Date(timeInput);
        } else if (Array.isArray(timeInput) && timeInput.length >= 5) {
            const [year, month, day, hour, minute] = timeInput;
            date = new Date(year, month - 1, day, hour, minute);
        } else {
            return '-';
        }
        if (isNaN(date.getTime())) return '-';
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        const h = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');
        return `${y}-${m}-${d} ${h}:${min}`;
    }

    /**
     * 销毁组件
     */
    function destroy() {
        console.log('[RoleManagement] 组件销毁');
        currentPermRoleId = null;
    }

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('role-management', {
            init,
            destroy,
            loadRoles,
            showRoleForm,
            closeRoleModal,
            saveRole,
            deleteRole,
            showMenuPermModal,
            closeMenuPermModal,
            saveMenuPermissions,
            checkAllMenus
        });
    }

    // 暴露到全局
    window.RoleManagement = {
        loadRoles,
        showRoleForm,
        closeRoleModal,
        saveRole,
        deleteRole,
        showMenuPermModal,
        closeMenuPermModal,
        saveMenuPermissions,
        checkAllMenus
    };

})();
