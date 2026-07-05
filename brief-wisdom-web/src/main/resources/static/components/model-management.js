/**
 * 模型管理组件
 * 用于系统设置页面中的模型管理功能
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
(function() {
    'use strict';

    const MODEL_API = '/api/ai/models';

    /**
     * 初始化组件
     */
    function init(options = {}) {
        const containerId = options.containerId || 'models-tab-content';
        console.log('[ModelManagement] 模型管理组件初始化开始，容器:', containerId);

        // 如果提供了容器且模板生成器可用，先渲染 HTML 模板
        const container = document.getElementById(containerId);
        if (container && window.ModelManagementTemplate) {
            console.log('[ModelManagement] 使用模板生成器渲染 HTML');
            ModelManagementTemplate.render(container);
        }

        loadModels();
    }

    /**
     * 加载模型列表
     */
    async function loadModels() {
        console.log('[ModelManagement] 开始加载模型列表');
        try {
            const res = await fetch(MODEL_API);
            const result = await res.json();
            console.log('[ModelManagement] API 返回数据:', JSON.stringify(result, null, 2));

            // 兼容两种返回格式
            let models;
            if (result.success !== undefined && result.data) {
                models = result.data;
                console.log('[ModelManagement] 检测到 Result 包装格式');
            } else {
                models = result;
                console.log('[ModelManagement] 检测到直接返回数组格式');
            }

            console.log('[ModelManagement] 模型数量:', models ? models.length : 0);
            renderModelTable(models || []);
        } catch (err) {
            console.error('[ModelManagement] 加载模型异常:', err);
            showToast('加载模型异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染模型表格
     */
    function renderModelTable(models) {
        const tbody = document.getElementById('model-table-body');
        if (!tbody) {
            console.error('[ModelManagement] 找不到 model-table-body 元素');
            return;
        }

        if (!models || models.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty-hint">暂无模型数据</td></tr>';
            return;
        }

        tbody.innerHTML = models.map(m => `
            <tr>
                <td>${m.sortOrder ?? 0}</td>
                <td><code>${escapeHtml(m.modelName)}</code></td>
                <td>${escapeHtml(m.displayName)}</td>
                <td>${escapeHtml(m.provider)}</td>
                <td>${escapeHtml(m.description || '-')}</td>
                <td>
                    <span class="status-badge ${m.thinkingMode === 'thinking' ? 'status-thinking' : 'status-normal'}">
                        ${m.thinkingMode === 'thinking' ? '思考模式' : '普通模式'}
                    </span>
                </td>
                <td>
                    <span class="status-badge ${m.isEnabled === 1 ? 'status-enabled' : 'status-disabled'}">
                        ${m.isEnabled === 1 ? '启用' : '禁用'}
                    </span>
                    <button class="btn-sm btn-toggle" onclick="ModelManagement.toggleModel(${m.id}, ${m.isEnabled !== 1})">
                        ${m.isEnabled === 1 ? '禁用' : '启用'}
                    </button>
                </td>
                <td>
                    ${m.isActive === 1
                        ? '<span class="status-badge status-active">当前激活</span>'
                        : `<button class="btn-sm btn-activate" onclick="ModelManagement.activateModel(${m.id})">激活</button>`
                    }
                </td>
                <td>
                    <button class="btn-sm btn-edit-sm" onclick='ModelManagement.editModel(${JSON.stringify(m)})'>编辑</button>
                    <button class="btn-sm btn-delete-sm" onclick="ModelManagement.deleteModel(${m.id})">删除</button>
                </td>
            </tr>
        `).join('');
    }

    /**
     * 显示模型表单
     */
    function showModelForm() {
        document.getElementById('model-modal-title').textContent = '新增模型';
        document.getElementById('model-id').value = '';
        document.getElementById('model-name-input').value = '';
        document.getElementById('model-display-name').value = '';
        document.getElementById('model-provider').value = 'dashscope';
        document.getElementById('model-description').value = '';
        document.getElementById('model-thinking-mode').value = 'normal';
        document.getElementById('model-sort-order').value = '0';
        document.getElementById('model-modal').style.display = 'flex';
    }

    /**
     * 编辑模型
     */
    function editModel(model) {
        document.getElementById('model-modal-title').textContent = '编辑模型';
        document.getElementById('model-id').value = model.id;
        document.getElementById('model-name-input').value = model.modelName;
        document.getElementById('model-display-name').value = model.displayName;
        document.getElementById('model-provider').value = model.provider;
        document.getElementById('model-description').value = model.description || '';
        document.getElementById('model-thinking-mode').value = model.thinkingMode || 'normal';
        document.getElementById('model-sort-order').value = model.sortOrder ?? 0;
        document.getElementById('model-modal').style.display = 'flex';
    }

    /**
     * 保存模型
     */
    async function saveModel(e) {
        e.preventDefault();
        const id = document.getElementById('model-id').value;
        const payload = {
            modelName: document.getElementById('model-name-input').value.trim(),
            displayName: document.getElementById('model-display-name').value.trim(),
            provider: document.getElementById('model-provider').value.trim(),
            description: document.getElementById('model-description').value.trim(),
            thinkingMode: document.getElementById('model-thinking-mode').value,
            sortOrder: parseInt(document.getElementById('model-sort-order').value) || 0
        };

        try {
            const isEdit = !!id;
            if (isEdit) payload.id = parseInt(id);
            const url = MODEL_API;
            const method = isEdit ? 'PUT' : 'POST';

            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();

            // 兼容两种返回格式
            let success;
            if (result.success !== undefined) {
                success = result.success;
            } else {
                success = true; // 直接返回 DTO 视为成功
            }

            if (!success) {
                showToast('保存失败: ' + (result.error || result.msg), 'error');
                return;
            }

            closeModelModal();
            loadModels();
            showToast(isEdit ? '模型更新成功' : '模型创建成功');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 删除模型
     */
    async function deleteModel(id) {
        if (!await showConfirmDialog('确定要删除此模型吗？', '🗑️')) return;
        try {
            const res = await fetch(`${MODEL_API}/${id}`, { method: 'DELETE' });
            const result = await res.json();

            // 兼容两种返回格式
            let success;
            if (result.success !== undefined) {
                success = result.success;
            } else {
                success = true; // 直接返回 Boolean 视为成功
            }

            if (!success) {
                showToast('删除失败: ' + (result.error || result.msg), 'error');
                return;
            }

            loadModels();
            showToast('删除成功');
        } catch (err) {
            showToast('删除异常: ' + err.message, 'error');
        }
    }

    /**
     * 激活模型
     */
    async function activateModel(id) {
        if (!await showConfirmDialog('确定要激活此模型吗？（其他模型将取消激活）', '⚡')) return;
        try {
            const res = await fetch(`${MODEL_API}/activate/${id}`, { method: 'PUT' });
            const result = await res.json();

            // 兼容两种返回格式
            let success;
            if (result.success !== undefined) {
                success = result.success;
            } else {
                success = true;
            }

            if (!success) {
                showToast('激活失败: ' + (result.error || result.msg), 'error');
                return;
            }

            loadModels();
            showToast('激活成功');
        } catch (err) {
            showToast('激活异常: ' + err.message, 'error');
        }
    }

    /**
     * 切换模型状态
     */
    async function toggleModel(id, enabled) {
        try {
            const res = await fetch(`${MODEL_API}/${id}/toggle?enabled=${enabled}`, { method: 'PUT' });
            const result = await res.json();

            // 兼容两种返回格式
            let success;
            if (result.success !== undefined) {
                success = result.success;
            } else {
                success = true;
            }

            if (!success) {
                showToast('操作失败: ' + (result.error || result.msg), 'error');
                return;
            }

            loadModels();
            showToast(enabled ? '已启用' : '已禁用');
        } catch (err) {
            showToast('操作异常: ' + err.message, 'error');
        }
    }

    /**
     * 关闭模型弹窗
     */
    function closeModelModal() {
        document.getElementById('model-modal').style.display = 'none';
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
     * 显示确认对话框
     */
    async function showConfirmDialog(message, icon = '⚠️') {
        return new Promise(resolve => {
            if (typeof window.showConfirmDialog === 'function') {
                window.showConfirmDialog(message, icon).then(resolve);
            } else {
                resolve(confirm(message));
            }
        });
    }

    /**
     * 显示提示消息
     */
    function showToast(message, type = 'success') {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type);
        } else {
            console.log(`[${type}] ${message}`);
            alert(message);
        }
    }

    // 暴露公共方法到全局
    window.ModelManagement = {
        init,
        loadModels,
        showModelForm,
        editModel,
        saveModel,
        deleteModel,
        activateModel,
        toggleModel,
        closeModelModal
    };

    // 自动注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('model-management', {
            name: '模型管理',
            init: init
        });
    }

})();
