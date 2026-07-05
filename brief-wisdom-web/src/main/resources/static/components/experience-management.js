/**
 * 工作经历管理组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let experiences = [];

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[ExperienceManagement] 组件初始化');
        
        if (window.ExperienceManagementTemplate) {
            const container = document.getElementById('experience-tab-content');
            if (container) {
                ExperienceManagementTemplate.render(container);
                console.log('[ExperienceManagement] 模板渲染完成');
                
                await loadData();
            } else {
                console.error('[ExperienceManagement] 容器不存在');
            }
        } else {
            console.error('[ExperienceManagement] 模板未加载');
        }
    }

    /**
     * 加载数据
     */
    async function loadData() {
        try {
            const response = await fetch('/api/resume/manage/experiences');
            if (!response.ok) throw new Error('HTTP error');
            
            const data = await response.json();
            experiences = Array.isArray(data) ? data : (data.data || []);
            
            console.log(`[ExperienceManagement] 加载 ${experiences.length} 条工作经历`);
            if (experiences.length > 0) {
                console.log('[ExperienceManagement] 第一条数据:', experiences[0]);
                console.log('[ExperienceManagement] ID字段值:', experiences[0].id, typeof experiences[0].id);
            }
            renderList();
        } catch (error) {
            console.error('[ExperienceManagement] 加载失败:', error);
        }
    }

    /**
     * 渲染列表
     */
    function renderList() {
        const container = document.getElementById('exp-list');
        if (!container) return;
        
        if (experiences.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>暂无工作经历数据</p></div>';
            return;
        }
        
        container.innerHTML = experiences.map(exp => {
            const projectCount = exp.projects ? exp.projects.length : 0;
            const stackCount = exp.stacks ? exp.stacks.length : 0;
            
            // 确保ID存在且为数字类型
            if (!exp.id) {
                console.warn('[ExperienceManagement] 数据缺少ID:', exp);
                return '';  // 跳过没有ID的数据
            }
            
            console.log(`[ExperienceManagement] 渲染工作经历 ID=${exp.id}, title=${exp.title}`);
            
            return `
                <div class="data-card">
                    <div class="data-card-header">
                        <div>
                            <div class="data-card-title">${escapeHtml(exp.title || '未命名')}</div>
                            <div class="data-card-subtitle">${escapeHtml(exp.job || '')}</div>
                        </div>
                        <div class="data-card-actions">
                            <button class="btn btn-edit" onclick="console.log('点击编辑按钮, ID:', ${exp.id}); window.experienceManagement.editExperience(${exp.id})">编辑</button>
                            <button class="btn btn-delete" onclick="console.log('点击删除按钮, ID:', ${exp.id}); window.experienceManagement.deleteExperience(${exp.id})">删除</button>
                        </div>
                    </div>
                    <div class="data-card-body">
                        <p>${escapeHtml(exp.description || '')}</p>
                    </div>
                    <div class="data-card-meta">
                        <span class="meta-item">项目数: ${projectCount}</span>
                        <span class="meta-item">技术栈: ${stackCount}</span>
                    </div>
                </div>
            `;
        }).filter(html => html !== '').join('');
    }

    /**
     * 显示编辑表单
     */
    function showExperienceForm(data = null) {
        const isEdit = !!data;
        
        const formHtml = `
            <div class="form-group">
                <label>职位标题 *</label>
                <input type="text" name="title" id="exp-f-title" value="${escapeAttr(data?.title || '')}" required>
            </div>
            <div class="form-group">
                <label>岗位角色 *</label>
                <input type="text" name="job" id="exp-f-job" value="${escapeAttr(data?.job || '')}" required>
            </div>
            <div class="form-group">
                <label>整体描述</label>
                <div class="form-field-with-ai">
                    <textarea name="description" id="exp-f-description" rows="4">${escapeHtml(data?.description || '')}</textarea>
                    <button type="button" 
                            class="ai-polish-btn" 
                            onclick="window.AiPolishComponent.polish('exp-f-description', 'description', '${escapeAttr(data?.title || '')}')" 
                            title="AI润色">
                        ✨ AI
                    </button>
                </div>
            </div>
            <div class="form-group">
                <label>排序序号</label>
                <input type="number" name="sortOrder" id="exp-f-sortOrder" value="${data?.sortOrder ?? 0}">
            </div>
            <div class="form-group">
                <label>是否显示</label>
                <select name="isVisible" id="exp-f-isVisible">
                    <option value="1" ${data?.isVisible === 1 ? 'selected' : ''}>显示</option>
                    <option value="0" ${data?.isVisible === 0 ? 'selected' : ''}>隐藏</option>
                </select>
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-cancel" onclick="experienceManagement.closeModal()">取消</button>
                <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
            </div>
        `;
        
        openModal(isEdit ? '编辑工作经历' : '新增工作经历', formHtml);
        
        const form = document.getElementById('modal-form');
        if (!form) {
            console.error('[ExperienceManagement] 表单元素不存在');
            return;
        }
        
        form.onsubmit = async (e) => {
                e.preventDefault();
                
                // 调试：输出表单所有元素
                console.log('[ExperienceManagement] 表单元素数量:', form.elements.length);
                for (let i = 0; i < form.elements.length; i++) {
                    const elem = form.elements[i];
                    if (elem.name) {
                        console.log(`[ExperienceManagement] 字段 ${elem.name}:`, elem.value);
                    }
                }
                
                const fd = new FormData(form);
                console.log('[ExperienceManagement] FormData内容:');
                for (let [key, value] of fd.entries()) {
                    console.log(`  ${key}: ${value}`);
                }
                
                const payload = {
                    title: fd.get('title'),
                    job: fd.get('job'),
                    description: fd.get('description'),
                    sortOrder: parseInt(fd.get('sortOrder')) || 0,
                    isVisible: parseInt(fd.get('isVisible'))
                };
                
                console.log('[ExperienceManagement] 最终提交数据:', payload);
                
                try {
                    if (isEdit) {
                        await apiRequest(`/api/resume/manage/experiences/${data.id}`, 'PUT', payload);
                    } else {
                        await apiRequest('/api/resume/manage/experiences', 'POST', payload);
                    }
                    closeModal();
                    loadData();
                } catch (error) {
                    console.error('[ExperienceManagement] 保存失败:', error);
                    alert('保存失败: ' + error.message);
                }
            };
    }

    /**
     * 编辑工作经历
     */
    function editExperience(id) {
        console.log('[ExperienceManagement] 编辑ID:', id, '类型:', typeof id);
        
        // 尝试多种类型匹配（数字、字符串）
        const exp = experiences.find(e => e.id === id || String(e.id) === String(id));
        console.log('[ExperienceManagement] 找到数据:', exp);
        
        if (exp) {
            showExperienceForm(exp);
        } else {
            console.error('[ExperienceManagement] 未找到ID为', id, '的工作经历');
            console.log('[ExperienceManagement] 当前所有数据的ID:', experiences.map(e => e.id));
            alert('未找到对应的工作经历数据');
        }
    }

    /**
     * 删除工作经历
     */
    async function deleteExperience(id) {
        if (!confirm('确定删除该工作经历？删除后关联的项目、成果、技术栈也将被删除。')) return;
        
        try {
            await apiRequest(`/api/resume/manage/experiences/${id}`, 'DELETE');
            loadData();
        } catch (error) {
            console.error('[ExperienceManagement] 删除失败:', error);
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
     * 打开弹窗（弹窗不存在时自动创建）
     */
    function openModal(title, content) {
        let modal = document.getElementById('modal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'modal';
            modal.className = 'modal';
            modal.style.display = 'none';
            document.body.appendChild(modal);
        }
        modal.innerHTML = '<div class="modal-content"><div class="modal-header"><h3 id="modal-title">' + title + '</h3><button class="modal-close" onclick="experienceManagement.closeModal()">&times;</button></div><form id="modal-form" class="modal-form">' + content + '</form></div>';
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
    window.experienceManagement = {
        showExperienceForm,
        editExperience,
        deleteExperience,
        closeModal
    };

    /**
     * 销毁组件
     */
    function destroy() {
        console.log('[ExperienceManagement] 组件销毁');
        experiences = [];
    }

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('experience-management', {
            init,
            destroy
        });
        console.log('[ExperienceManagement] 组件已注册');
    }

    console.log('[ExperienceManagement] 工作经历组件加载成功');
})();
