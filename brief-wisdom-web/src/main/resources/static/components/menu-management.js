/**
 * 菜单管理组件
 * 可独立使用，也可嵌入到任意页面
 */

(function() {
    'use strict';

    const MENU_API = '/api/menu';

    // 缓存菜单树数据
    let allMenuTreeCache = [];

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[MenuManagement] 初始化');

        const container = document.getElementById(options.containerId || 'menu-tab-content');
        if (container && window.MenuManagementTemplate) {
            MenuManagementTemplate.render(container);
        }

        await loadMenus();
        
        setTimeout(() => {
            applyPermissions();
        }, 500);
    }

    /**
     * 加载菜单列表
     */
    async function loadMenus() {
        try {
            const res = await fetch(`${MENU_API}/all`);
            const result = await res.json();
            if (!result.success) {
                showToast('加载菜单失败: ' + result.error, 'error');
                return;
            }
            renderMenuTable(result.data);
        } catch (err) {
            console.error('加载菜单异常:', err);
            showToast('加载菜单异常', 'error');
        }
    }

    /**
     * 渲染菜单表格
     */
    function renderMenuTable(menus) {
        const tbody = document.getElementById('menu-table-body');
        if (!tbody) return;

        if (!menus || menus.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty-state">暂无菜单数据</td></tr>';
            return;
        }

        // 构建 id -> name 映射
        const nameMap = {};
        menus.forEach(m => { nameMap[m.id] = m.name; });

        const typeLabels = { 0: '目录', 1: '菜单', 2: '按钮' };
        const typeBadgeClass = { 0: 'type-dir', 1: 'type-menu', 2: 'type-btn' };

        tbody.innerHTML = menus.map(m => {
            const type = m.type ?? 1;
            const parentName = m.parentId && m.parentId !== 0 ? (nameMap[m.parentId] || '顶级') : '顶级';
            const canManage = typeof hasPermission === 'function' && hasPermission('menu:manage');
            
            const toggleVisBtn = canManage ? `<button class="btn btn-toggle-vis" onclick="MenuManagement.toggleVisible(${m.id})">${m.isVisible === 1 ? '隐藏' : '显示'}</button>` : '';
            const editBtn = canManage ? `<button class="btn btn-edit" onclick='MenuManagement.showMenuForm(${JSON.stringify(m)})'>编辑</button>` : '';
            const deleteBtn = canManage ? `<button class="btn btn-delete" onclick="MenuManagement.deleteMenu(${m.id})">删除</button>` : '';

            return `
                <tr>
                    <td>${m.sortOrder ?? 0}</td>
                    <td class="icon-cell">${m.icon || '-'}</td>
                    <td>${escapeHtml(m.name)}</td>
                    <td><span class="type-badge ${typeBadgeClass[type] || ''}">${typeLabels[type] || '菜单'}</span></td>
                    <td>${parentName}</td>
                    <td><code>${escapeHtml(m.url) || '-'}</code></td>
                    <td>
                        <span class="badge ${m.isVisible === 1 ? 'badge-show' : 'badge-hide'}">
                            ${m.isVisible === 1 ? '显示' : '隐藏'}
                        </span>
                        ${toggleVisBtn}
                    </td>
                    <td>
                        <div class="actions">
                            ${editBtn}
                            ${deleteBtn}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    /**
     * 显示新增/编辑菜单表单
     */
    async function showMenuForm(menu) {
        const modal = document.getElementById('menu-modal');
        if (!modal) return;

        if (menu) {
            document.getElementById('menu-modal-title').textContent = '编辑菜单';
            document.getElementById('menu-id').value = menu.id;
            document.getElementById('menu-type').value = String(menu.type ?? 1);
            document.getElementById('menu-name').value = menu.name;
            document.getElementById('menu-url').value = menu.url || '';
            document.getElementById('menu-icon').value = menu.icon || '';
            document.getElementById('menu-sort').value = menu.sortOrder ?? 0;
            document.getElementById('menu-target').value = menu.target || '_self';
            document.getElementById('menu-visible').value = String(menu.isVisible);
            document.getElementById('menu-require-login').value = String(menu.requireLogin ?? 0);
            document.getElementById('menu-permission').value = menu.permission || '';
            await loadMenuTreeForSelector(menu.parentId || 0);
        } else {
            document.getElementById('menu-modal-title').textContent = '新增菜单';
            document.getElementById('menu-id').value = '';
            document.getElementById('menu-type').value = '1';
            document.getElementById('menu-name').value = '';
            document.getElementById('menu-url').value = '';
            document.getElementById('menu-icon').value = '';
            document.getElementById('menu-sort').value = '0';
            document.getElementById('menu-target').value = '_self';
            document.getElementById('menu-visible').value = '1';
            document.getElementById('menu-require-login').value = '0';
            document.getElementById('menu-permission').value = '';
            await loadMenuTreeForSelector(0);
        }

        onMenuTypeChange();
        modal.style.display = 'flex';
    }

    /**
     * 加载菜单树用于父级选择器
     */
    async function loadMenuTreeForSelector(selectedParentId) {
        try {
            const res = await fetch(`${MENU_API}/all/tree`);
            const result = await res.json();
            if (!result.success) return;
            allMenuTreeCache = result.data || [];
            renderParentSelect(allMenuTreeCache, selectedParentId);
        } catch (err) {
            console.error('加载菜单树异常:', err);
        }
    }

    /**
     * 渲染父级菜单选择器
     */
    function renderParentSelect(nodes, selectedId, container) {
        const select = container || document.getElementById('menu-parent');
        if (!container) {
            select.innerHTML = '<option value="0">顶级菜单</option>';
        }
        if (!nodes || nodes.length === 0) return;
        
        nodes.forEach(node => {
            const opt = document.createElement('option');
            opt.value = node.id;
            const typeLabel = node.type === 0 ? '[目录]' : node.type === 2 ? '[按钮]' : '[菜单]';
            opt.textContent = `${typeLabel} ${node.icon || ''} ${node.name}`.trim();
            if (selectedId && String(node.id) === String(selectedId)) opt.selected = true;
            if (!container) select.appendChild(opt);
            else container.appendChild(opt);

            if (node.children && node.children.length > 0) {
                renderParentSelect(node.children, selectedId, select);
            }
        });
    }

    /**
     * 根据菜单类型显示/隐藏字段
     */
    function onMenuTypeChange() {
        const type = parseInt(document.getElementById('menu-type').value);
        const urlGroup = document.getElementById('menu-url-group');
        const permGroup = document.getElementById('menu-perm-group');
        const targetGroup = document.getElementById('menu-target-group');

        if (urlGroup) urlGroup.style.display = (type === 2) ? 'none' : 'block';
        if (permGroup) permGroup.style.display = (type === 2) ? 'block' : 'none';
        if (targetGroup) targetGroup.style.display = (type === 0) ? 'none' : 'block';
    }

    /**
     * 保存菜单
     */
    async function saveMenu(e) {
        e.preventDefault();
        const id = document.getElementById('menu-id').value;
        const payload = {
            type: parseInt(document.getElementById('menu-type').value),
            parentId: parseInt(document.getElementById('menu-parent').value),
            name: document.getElementById('menu-name').value.trim(),
            url: document.getElementById('menu-url').value.trim(),
            icon: document.getElementById('menu-icon').value.trim(),
            sortOrder: parseInt(document.getElementById('menu-sort').value),
            target: document.getElementById('menu-target').value,
            isVisible: parseInt(document.getElementById('menu-visible').value),
            requireLogin: parseInt(document.getElementById('menu-require-login').value),
            permission: document.getElementById('menu-permission').value.trim()
        };

        try {
            const isEdit = !!id;
            const method = isEdit ? 'PUT' : 'POST';
            if (isEdit) payload.id = parseInt(id);

            const res = await fetch(MENU_API, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (!result.success) {
                showToast('保存失败: ' + result.error, 'error');
                return;
            }
            closeModal();
            loadMenus();
            showToast('保存成功', 'success');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 关闭弹窗
     */
    function closeModal() {
        const modal = document.getElementById('menu-modal');
        if (modal) modal.style.display = 'none';
    }

    /**
     * 删除菜单
     */
    async function deleteMenu(id) {
        if (typeof showConfirmDialog === 'function') {
            if (!await showConfirmDialog('确定要删除此菜单吗？', '🗑️')) return;
        }

        try {
            const res = await fetch(`${MENU_API}/${id}`, { method: 'DELETE' });
            const result = await res.json();
            if (!result.success) {
                showToast('删除失败: ' + result.error, 'error');
                return;
            }
            loadMenus();
            showToast('删除成功', 'success');
        } catch (err) {
            showToast('删除异常: ' + err.message, 'error');
        }
    }

    /**
     * 切换显示状态
     */
    async function toggleVisible(id) {
        try {
            const res = await fetch(`${MENU_API}/${id}/toggle`, { method: 'PUT' });
            const result = await res.json();
            if (!result.success) {
                showToast('切换失败: ' + result.error, 'error');
                return;
            }
            loadMenus();
        } catch (err) {
            showToast('切换异常: ' + err.message, 'error');
        }
    }

    /**
     * 应用权限控制
     */
    function applyPermissions() {
        if (typeof hasPermission !== 'function') return;

        if (!hasPermission('menu:manage')) {
            document.querySelectorAll('#menu-tab-content .btn-primary').forEach(btn => btn.style.display = 'none');
            document.querySelectorAll('#menu-tab-content .btn-edit, #menu-tab-content .btn-delete, #menu-tab-content .btn-toggle-vis').forEach(btn => btn.style.display = 'none');
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
     * 销毁组件
     */
    function destroy() {
        console.log('[MenuManagement] 组件销毁');
        allMenuTreeCache = [];
    }

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('menu-management', {
            init,
            destroy,
            loadMenus,
            showMenuForm,
            closeModal,
            saveMenu,
            deleteMenu,
            toggleVisible,
            onMenuTypeChange
        });
    }

    // 暴露到全局
    window.MenuManagement = {
        loadMenus,
        showMenuForm,
        closeModal,
        saveMenu,
        deleteMenu,
        toggleVisible,
        onMenuTypeChange
    };

})();
