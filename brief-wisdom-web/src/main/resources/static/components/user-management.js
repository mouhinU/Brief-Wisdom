/**
 * 用户管理组件
 * 可独立使用，也可嵌入到任意页面
 */

(function() {
    'use strict';

    const USER_API = '/api/user';

    // 组件内部状态
    let currentPage = 1;
    let hasMore = false;

    /**
     * 初始化组件
     * @param {Object} options - 配置选项
     * @param {string} options.containerId - 容器元素ID
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

        // 加载用户数据
        await loadUsers();

        // 应用权限控制
        setTimeout(() => {
            applyPermissions();
        }, 500);
    }

    /**
     * 加载用户列表
     */
    async function loadUsers(page) {
        if (page) currentPage = page;
        
        const levelFilter = document.getElementById('user-level-filter');
        const searchInput = document.getElementById('user-search-input');
        
        const level = levelFilter ? levelFilter.value : '';
        const keyword = searchInput ? searchInput.value.trim() : '';

        let url = `${USER_API}/list?page=${currentPage}&size=20`;
        if (level) url += `&level=${level}`;
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

        try {
            const res = await fetch(url);
            const result = await res.json();
            if (!result.success) {
                showToast('加载用户失败: ' + result.error, 'error');
                return;
            }
            const data = result.data;
            hasMore = data.hasMore;
            renderUserTable(data.records || []);
            renderUserPagination(data);
        } catch (err) {
            console.error('加载用户异常:', err);
            showToast('加载用户异常', 'error');
        }
    }

    /**
     * 渲染用户表格
     */
    function renderUserTable(users) {
        const tbody = document.getElementById('user-table-body');
        if (!tbody) return;

        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="empty-state">暂无用户数据</td></tr>';
            return;
        }

        const levelLabels = { admin: '管理员', vip: '会员', normal: '普通用户' };
        const levelClasses = { admin: 'level-admin', vip: 'level-vip', normal: 'level-normal' };

        tbody.innerHTML = users.map(u => {
            const lvl = u.userLevel || 'normal';
            const time = u.createTime ? formatDateTime(u.createTime) : '-';
            const canManage = typeof hasPermission === 'function' && hasPermission('user:manage');
            
            const actionBtns = canManage ? `
                <button class="btn btn-edit" onclick="UserManagement.showUserLevelModal(${u.id}, '${lvl}')">改级别</button>
                <button class="btn btn-role" onclick="UserManagement.showUserRoleModal('${escapeHtml(u.userId)}', '${escapeHtml(u.username)}')">分配角色</button>
                <button class="btn btn-reset-pwd" onclick="UserManagement.resetUserPassword(${u.id}, '${escapeHtml(u.username)}')">重置密码</button>
                <button class="btn btn-delete" onclick="UserManagement.deleteUser(${u.id}, '${escapeHtml(u.username)}')">删除</button>
            ` : '<span class="no-perm-hint">无操作权限</span>';

            return `
                <tr>
                    <td>${escapeHtml(u.username)}</td>
                    <td>${escapeHtml(u.nickname) || '-'}</td>
                    <td><span class="level-badge ${levelClasses[lvl] || ''}">${levelLabels[lvl] || lvl}</span></td>
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
     * 渲染分页
     */
    function renderUserPagination(data) {
        const container = document.getElementById('user-pagination');
        if (!container) return;

        if (!data || data.pages <= 1) {
            container.innerHTML = '';
            return;
        }

        let html = '';
        const current = data.page;
        const total = data.pages;

        if (current > 1) {
            html += `<button class="page-btn" onclick="UserManagement.loadUsers(${current - 1})">上一页</button>`;
        }
        for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
            html += `<button class="page-btn ${i === current ? 'active' : ''}" onclick="UserManagement.loadUsers(${i})">${i}</button>`;
        }
        if (current < total) {
            html += `<button class="page-btn" onclick="UserManagement.loadUsers(${current + 1})">下一页</button>`;
        }
        html += `<span class="page-info">共 ${data.total} 条</span>`;
        container.innerHTML = html;
    }

    /**
     * 显示修改级别弹窗
     */
    function showUserLevelModal(id, currentLevel) {
        const modal = document.getElementById('user-level-modal');
        if (!modal) return;

        document.getElementById('user-level-id').value = id;
        document.getElementById('user-level-select').value = currentLevel;
        modal.style.display = 'flex';
    }

    /**
     * 关闭修改级别弹窗
     */
    function closeUserLevelModal() {
        const modal = document.getElementById('user-level-modal');
        if (modal) modal.style.display = 'none';
    }

    /**
     * 保存用户级别
     */
    async function saveUserLevel(e) {
        e.preventDefault();
        const id = document.getElementById('user-level-id').value;
        const level = document.getElementById('user-level-select').value;

        try {
            const res = await fetch(`${USER_API}/${id}/level`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ level })
            });
            const result = await res.json();
            if (!result.success) {
                showToast('修改失败: ' + result.error, 'error');
                return;
            }
            closeUserLevelModal();
            loadUsers();
            showToast('修改成功', 'success');
        } catch (err) {
            showToast('修改异常: ' + err.message, 'error');
        }
    }

    /**
     * 重置用户密码
     */
    async function resetUserPassword(id, username) {
        if (typeof showConfirmDialog === 'function') {
            if (!await showConfirmDialog(`确定要重置用户 "${username}" 的密码吗？`, '🔑')) return;
        }

        try {
            const res = await fetch(`${USER_API}/${id}/reset-password`, { method: 'PUT' });
            const result = await res.json();
            if (!result.success) {
                showToast('重置失败: ' + result.error, 'error');
                return;
            }
            showToast('密码已重置为默认密码', 'success');
        } catch (err) {
            showToast('重置异常: ' + err.message, 'error');
        }
    }

    /**
     * 删除用户
     */
    async function deleteUser(id, username) {
        if (typeof showConfirmDialog === 'function') {
            if (!await showConfirmDialog(`确定要删除用户 "${username}" 吗？`, '🗑️')) return;
        }

        try {
            const res = await fetch(`${USER_API}/${id}`, { method: 'DELETE' });
            const result = await res.json();
            if (!result.success) {
                showToast('删除失败: ' + result.error, 'error');
                return;
            }
            loadUsers();
            showToast('删除成功', 'success');
        } catch (err) {
            showToast('删除异常: ' + err.message, 'error');
        }
    }

    /**
     * 显示用户角色分配弹窗
     */
    async function showUserRoleModal(userId, username) {
        const modal = document.getElementById('user-role-modal');
        if (!modal) return;

        document.getElementById('user-role-title').textContent = `分配角色 - ${username}`;

        try {
            // 并行加载所有角色和用户已有角色
            const [rolesRes, userRolesRes] = await Promise.all([
                fetch('/api/role/enabled'),
                fetch(`/api/role/user/${userId}`)
            ]);
            const rolesResult = await rolesRes.json();
            const userRolesResult = await userRolesRes.json();

            if (!rolesResult.success || !userRolesResult.success) {
                showToast('加载角色数据失败', 'error');
                return;
            }

            const userRoleIds = new Set(userRolesResult.data.map(r => r.id));
            const container = document.getElementById('user-role-checkboxes');
            container.innerHTML = rolesResult.data.map(r => `
                <label class="role-checkbox-item">
                    <input type="checkbox" value="${r.id}" ${userRoleIds.has(r.id) ? 'checked' : ''}>
                    <span class="role-cb-name">${escapeHtml(r.roleName)}</span>
                    <span class="role-cb-key">(${escapeHtml(r.roleKey)})</span>
                </label>
            `).join('');

            modal.style.display = 'flex';
            modal.dataset.userId = userId;
        } catch (err) {
            showToast('加载角色数据异常', 'error');
        }
    }

    /**
     * 关闭用户角色弹窗
     */
    function closeUserRoleModal() {
        const modal = document.getElementById('user-role-modal');
        if (modal) modal.style.display = 'none';
    }

    /**
     * 保存用户角色
     */
    async function saveUserRoles() {
        const modal = document.getElementById('user-role-modal');
        const userId = modal ? modal.dataset.userId : null;
        if (!userId) return;

        const checkedIds = Array.from(
            document.querySelectorAll('#user-role-checkboxes input:checked')
        ).map(cb => parseInt(cb.value));

        try {
            const res = await fetch(`/api/role/assign/${userId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(checkedIds)
            });
            const result = await res.json();
            if (!result.success) {
                showToast('保存失败: ' + result.error, 'error');
                return;
            }
            closeUserRoleModal();
            showToast('用户角色已保存', 'success');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 应用权限控制
     */
    function applyPermissions() {
        if (typeof hasPermission !== 'function') return;
        
        if (!hasPermission('user:manage')) {
            // 隐藏表格中的操作按钮
            document.querySelectorAll('#user-tab-content .btn-edit, #user-tab-content .btn-role, #user-tab-content .btn-reset-pwd, #user-tab-content .btn-delete').forEach(btn => {
                btn.style.display = 'none';
            });
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
        console.log('[UserManagement] 组件销毁');
        currentPage = 1;
        hasMore = false;
    }

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('user-management', {
            init,
            destroy,
            // 暴露公共方法供外部调用
            loadUsers,
            showUserLevelModal,
            closeUserLevelModal,
            saveUserLevel,
            resetUserPassword,
            deleteUser,
            showUserRoleModal,
            closeUserRoleModal,
            saveUserRoles
        });
    }

    // 同时暴露到全局，方便 HTML 中直接调用
    window.UserManagement = {
        loadUsers,
        showUserLevelModal,
        closeUserLevelModal,
        saveUserLevel,
        resetUserPassword,
        deleteUser,
        showUserRoleModal,
        closeUserRoleModal,
        saveUserRoles
    };

})();
