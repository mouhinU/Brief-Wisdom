/**
 * 项目经历管理组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let projects = [];

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[ProjectManagement] 组件初始化');
        
        if (window.ProjectManagementTemplate) {
            const container = document.getElementById('project-tab-content');
            if (container) {
                ProjectManagementTemplate.render(container);
                console.log('[ProjectManagement] 模板渲染完成');
                
                await loadData();
            } else {
                console.error('[ProjectManagement] 容器不存在');
            }
        } else {
            console.error('[ProjectManagement] 模板未加载');
        }
    }

    /**
     * 加载数据
     */
    async function loadData() {
        try {
            const response = await fetch('/api/resume/manage/projects');
            if (!response.ok) throw new Error('HTTP error');
            const data = await response.json();
            projects = Array.isArray(data) ? data : (data.data || []);
            console.log(`[ProjectManagement] 加载 ${projects.length} 个项目`);
            renderList();
        } catch (error) {
            console.error('[ProjectManagement] 加载失败:', error);
        }
    }

    /**
     * 渲染列表
     */
    function renderList() {
        const container = document.getElementById('proj-list');
        if (!container) return;
        
        if (projects.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>暂无项目数据</p></div>';
            return;
        }
        
        container.innerHTML = projects.map(proj => {
            const achievementCount = proj.achievements ? proj.achievements.length : 0;
            const projId = proj.id || 'null';
            
            return `
                <div class="data-card">
                    <div class="data-card-header">
                        <div>
                            <div class="data-card-title">${escapeHtml(proj.name || '未命名')}</div>
                            <div class="data-card-subtitle">${escapeHtml(proj.lifecycle || '')}</div>
                        </div>
                        <div class="data-card-actions">
                            <button class="btn btn-edit" onclick="window.projectManagement.editProject(${projId})">编辑</button>
                            <button class="btn btn-delete" onclick="window.projectManagement.deleteProject(${projId})">删除</button>
                        </div>
                    </div>
                    <div class="data-card-body">
                        <p>${escapeHtml(proj.background || '')}</p>
                    </div>
                    <div class="data-card-meta">
                        <span class="meta-item">成果数: ${achievementCount}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * 显示编辑表单
     */
    async function showProjectForm(data = null) {
        const isEdit = !!data;
        
        // 加载工作经历列表用于选择
        let expOptions = '';
        try {
            const response = await fetch('/api/resume/experiences');
            if (response.ok) {
                const experiencesData = await response.json();
                const experiences = Array.isArray(experiencesData) ? experiencesData : (experiencesData.data || []);
                expOptions = experiences.map(e => 
                    `<option value="${e.id}" ${data?.experienceId === e.id ? 'selected' : ''}>${escapeHtml(e.title.substring(0, 40))}</option>`
                ).join('');
            }
        } catch (error) {
            console.error('[ProjectManagement] 加载工作经历失败:', error);
        }
        
        const formHtml = `
            <div class="form-group">
                <label>关联工作经历 *</label>
                <select name="experienceId" id="proj-f-experienceId" required>${expOptions}</select>
            </div>
            <div class="form-group">
                <label>项目名称 *</label>
                <input type="text" name="name" id="proj-f-name" value="${escapeAttr(data?.name || '')}" required>
            </div>
            <div class="form-group">
                <label>项目周期</label>
                <input type="text" name="lifecycle" id="proj-f-lifecycle" value="${escapeAttr(data?.lifecycle || '')}" placeholder="如: 2024.12 - 2025.10">
            </div>
            <div class="form-group">
                <label>项目背景</label>
                <textarea name="background" id="proj-f-background" rows="3">${escapeHtml(data?.background || '')}</textarea>
            </div>
            <div class="form-group">
                <label>职责描述</label>
                <textarea name="duty" id="proj-f-duty" rows="3">${escapeHtml(data?.duty || '')}</textarea>
            </div>
            <div class="form-group">
                <label>排序序号</label>
                <input type="number" name="sortOrder" id="proj-f-sortOrder" value="${data?.sortOrder ?? 0}">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
                <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
            </div>
        `;
        
        openModal(isEdit ? '编辑项目' : '新增项目', formHtml);
        
        // 等待 DOM 更新后再绑定事件
        setTimeout(() => {
            const form = document.getElementById('modal-form');
            if (!form) {
                console.error('[ProjectManagement] 表单元素不存在');
                return;
            }
            
            form.onsubmit = async (e) => {
                e.preventDefault();
                
                // 调试：输出表单所有元素
                console.log('[ProjectManagement] 表单元素数量:', form.elements.length);
                for (let i = 0; i < form.elements.length; i++) {
                    const elem = form.elements[i];
                    if (elem.name) {
                        console.log(`[ProjectManagement] 字段 ${elem.name}:`, elem.value);
                    }
                }
                
                const fd = new FormData(form);
                console.log('[ProjectManagement] FormData内容:');
                for (let [key, value] of fd.entries()) {
                    console.log(`  ${key}: ${value}`);
                }
                
                const payload = {
                    experienceId: parseInt(fd.get('experienceId')) || null,
                    name: fd.get('name'),
                    lifecycle: fd.get('lifecycle'),
                    background: fd.get('background'),
                    duty: fd.get('duty'),
                    sortOrder: parseInt(fd.get('sortOrder')) || 0
                };
                
                console.log('[ProjectManagement] 最终提交数据:', payload);
                
                try {
                    if (isEdit) {
                        await apiRequest(`/api/resume/manage/projects/${data.id}`, 'PUT', payload);
                    } else {
                        await apiRequest('/api/resume/manage/projects', 'POST', payload);
                    }
                    closeModal();
                    loadData();
                } catch (error) {
                    console.error('[ProjectManagement] 保存失败:', error);
                    alert('保存失败: ' + error.message);
                }
            };
        }, 0);
    }

    /**
     * 编辑项目
     */
    function editProject(id) {
        const proj = projects.find(p => p.id === id || String(p.id) === String(id));
        if (proj) showProjectForm(proj);
    }

    /**
     * 删除项目
     */
    async function deleteProject(id) {
        if (!confirm('确定删除该项目？删除后关联的成果也将被删除。')) return;
        
        try {
            await apiRequest(`/api/resume/manage/projects/${id}`, 'DELETE');
            loadData();
        } catch (error) {
            console.error('[ProjectManagement] 删除失败:', error);
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
            console.error('[ProjectManagement] 弹窗元素不存在');
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

    // 暴露全局方法
    window.projectManagement = {
        showProjectForm,
        editProject,
        deleteProject,
        loadData
    };
    
    // 暴露 closeModal 到全局，供弹窗中的按钮使用
    window.closeModal = closeModal;

    /**
     * 销毁组件
     */
    function destroy() {
        console.log('[ProjectManagement] 组件销毁');
        projects = [];
    }

    // 注册组件
    registerComponent('project-management', {
        init,
        destroy
    });

    console.log('[ProjectManagement] 项目经历组件加载成功');
})();
