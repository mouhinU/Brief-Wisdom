/**
 * 技术栈管理组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let techStacks = [];
    let experiences = [];  // 用于选择关联工作经历

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[TechStackManagement] 组件初始化');
        
        if (window.TechStackManagementTemplate) {
            const container = document.getElementById('tech-stack-tab-content');
            if (container) {
                TechStackManagementTemplate.render(container);
                console.log('[TechStackManagement] 模板渲染完成');
                
                await loadData();
            } else {
                console.error('[TechStackManagement] 容器不存在');
            }
        } else {
            console.error('[TechStackManagement] 模板未加载');
        }
    }

    /**
     * 加载数据
     */
    async function loadData() {
        try {
            const response = await fetch('/api/resume/manage/stacks');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('[TechStackManagement] 原始数据:', data);
            console.log('[TechStackManagement] 数据类型:', typeof data, Array.isArray(data));
            
            // 处理不同的返回格式
            let list = [];
            if (Array.isArray(data)) {
                list = data;
            } else if (data && data.data && Array.isArray(data.data)) {
                list = data.data;
            } else if (data && typeof data === 'object') {
                list = [data];
            }
            
            techStacks = list;
            console.log('[TechStackManagement] 处理后的数据:', techStacks);
            console.log('[TechStackManagement] 数据条数:', techStacks.length);
            
            renderList();
            
        } catch (error) {
            console.error('[TechStackManagement] 加载数据失败:', error);
            const container = document.getElementById('tech-stack-tab-content');
            if (container) {
                container.innerHTML = '<div class="error-message">数据加载失败，请刷新重试</div>';
            }
        }
    }

    /**
     * 渲染列表
     */
    function renderList() {
        const container = document.getElementById('stack-list');
        if (!container) return;
        
        if (techStacks.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>暂无技术栈数据</p></div>';
            return;
        }
        
        container.innerHTML = techStacks.map(item => {
            const stackId = item.id || 'null';
            
            return `
                <div class="data-card">
                    <div class="data-card-header">
                        <div>
                            <div class="data-card-title">${escapeHtml(item.techName || '未命名')}</div>
                        </div>
                        <div class="data-card-actions">
                            <button class="btn btn-edit" onclick="window.techStackManagement.editTechStack(${stackId})">编辑</button>
                            <button class="btn btn-delete" onclick="window.techStackManagement.deleteTechStack(${stackId})">删除</button>
                        </div>
                    </div>
                    <div class="data-card-meta">
                        <span class="meta-item">经历ID: ${item.experienceId || '-'}</span>
                        <span class="meta-item">排序: ${item.sortOrder || 0}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * 显示编辑表单
     */
    async function showTechStackForm(data = null) {
        const isEdit = !!data;
        
        // 加载工作经历列表用于选择
        let expOptions = '';
        try {
            const response = await fetch('/api/resume/experiences');
            if (response.ok) {
                const experiencesData = await response.json();
                experiences = Array.isArray(experiencesData) ? experiencesData : (experiencesData.data || []);
                
                expOptions = experiences.map(e => 
                    `<option value="${e.id}" ${data?.experienceId === e.id ? 'selected' : ''}>${escapeHtml((e.title || '未命名').substring(0, 40))}</option>`
                ).join('');
            }
        } catch (error) {
            console.error('[TechStackManagement] 加载工作经历失败:', error);
        }
        
        const formHtml = `
            <div class="form-group">
                <label>关联工作经历 *</label>
                <select name="experienceId" id="stack-f-experienceId" required>${expOptions}</select>
            </div>
            <div class="form-group">
                <label>技术名称 *</label>
                <input type="text" name="techName" id="stack-f-techName" value="${escapeAttr(data?.techName || '')}" required>
            </div>
            <div class="form-group">
                <label>熟练度</label>
                <select name="proficiency" id="stack-f-proficiency">
                    <option value="了解" ${data?.proficiency === '了解' ? 'selected' : ''}>了解</option>
                    <option value="熟悉" ${data?.proficiency === '熟悉' ? 'selected' : ''}>熟悉</option>
                    <option value="精通" ${data?.proficiency === '精通' ? 'selected' : ''}>精通</option>
                </select>
            </div>
            <div class="form-group">
                <label>排序序号</label>
                <input type="number" name="sortOrder" id="stack-f-sortOrder" value="${data?.sortOrder ?? 0}">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
                <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
            </div>
        `;
        
        openModal(isEdit ? '编辑技术栈' : '新增技术栈', formHtml);
        
        // 等待 DOM 更新后再绑定事件
        setTimeout(() => {
            const form = document.getElementById('modal-form');
            if (!form) {
                console.error('[TechStackManagement] 表单元素不存在');
                return;
            }
            
            form.onsubmit = async (e) => {
                e.preventDefault();
                
                const fd = new FormData(form);
                console.log('[TechStackManagement] FormData内容:');
                for (let [key, value] of fd.entries()) {
                    console.log(`  ${key}: ${value}`);
                }
                
                const payload = {
                    experienceId: parseInt(fd.get('experienceId')),
                    techName: fd.get('techName'),
                    proficiency: fd.get('proficiency'),
                    sortOrder: parseInt(fd.get('sortOrder')) || 0
                };
                
                console.log('[TechStackManagement] 最终提交数据:', payload);
                
                try {
                    if (isEdit) {
                        await apiRequest(`/api/resume/manage/stacks/${data.id}`, 'PUT', payload);
                    } else {
                        await apiRequest('/api/resume/manage/stacks', 'POST', payload);
                    }
                    closeModal();
                    loadData();
                } catch (error) {
                    console.error('[TechStackManagement] 保存失败:', error);
                    alert('保存失败: ' + error.message);
                }
            };
        }, 0);
    }

    /**
     * 编辑技术栈
     */
    function editTechStack(id) {
        console.log('[TechStackManagement] 编辑ID:', id, typeof id);
        const stack = techStacks.find(s => s.id === id || String(s.id) === String(id));
        console.log('[TechStackManagement] 找到数据:', stack);
        if (stack) {
            showTechStackForm(stack);
        } else {
            console.error('[TechStackManagement] 未找到ID为', id, '的技术栈');
            alert('未找到对应的技术栈数据');
        }
    }

    /**
     * 删除技术栈
     */
    async function deleteTechStack(id) {
        if (!confirm('确定删除该技术栈？')) return;
        
        try {
            await apiRequest(`/api/resume/manage/stacks/${id}`, 'DELETE');
            loadData();
        } catch (error) {
            console.error('[TechStackManagement] 删除失败:', error);
            alert('删除失败: ' + error.message);
        }
    }

    /**
     * HTML转义
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * HTML属性转义
     */
    function escapeAttr(text) {
        return text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    /**
     * API请求封装
     */
    async function apiRequest(url, method = 'GET', body = null) {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        if (body) {
            options.body = JSON.stringify(body);
        }
        
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        return response.json();
    }

    /**
     * 打开弹窗
     */
    function openModal(title, content) {
        const modal = document.getElementById('modal');
        const modalTitle = document.getElementById('modal-title');
        
        if (!modal || !modalTitle) {
            console.error('[TechStackManagement] 弹窗元素不存在');
            return;
        }
        
        modalTitle.textContent = title;
        
        // 替换整个modal-content的内容
        const modalContent = modal.querySelector('.modal-content');
        if (modalContent) {
            modalContent.innerHTML = `
                <div class="modal-header">
                    <h3 id="modal-title">${title}</h3>
                    <button class="modal-close" onclick="closeModal()">×</button>
                </div>
                <form id="modal-form" class="modal-form">${content}</form>
            `;
        }
        
        modal.style.display = 'flex';
    }

    /**
     * 关闭弹窗
     */
    function closeModal() {
        const modal = document.getElementById('modal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    /**
     * 销毁组件
     */
    function destroy() {
        console.log('[TechStackManagement] 组件销毁');
        techStacks = [];
    }

    // 暴露全局方法
    window.techStackManagement = {
        showTechStackForm,
        editTechStack,
        deleteTechStack
    };
    
    // 暴露 closeModal 到全局，供弹窗中的按钮使用
    window.closeModal = closeModal;

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('tech-stack-management', {
            init,
            destroy
        });
        console.log('[TechStackManagement] 组件已注册');
    }

})();
