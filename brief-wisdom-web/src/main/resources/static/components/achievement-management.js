/**
 * 项目成果管理组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let achievements = [];
    let projects = [];  // 用于选择关联项目

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[AchievementManagement] 组件初始化');
        
        if (window.AchievementManagementTemplate) {
            const container = document.getElementById('achievement-tab-content');
            if (container) {
                AchievementManagementTemplate.render(container);
                console.log('[AchievementManagement] 模板渲染完成');
                
                await loadData();
            } else {
                console.error('[AchievementManagement] 容器不存在');
            }
        } else {
            console.error('[AchievementManagement] 模板未加载');
        }
    }

    /**
     * 加载数据
     */
    async function loadData() {
        try {
            const response = await fetch('/api/resume/manage/achievements');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('[AchievementManagement] 原始数据:', data);
            console.log('[AchievementManagement] 数据类型:', typeof data, Array.isArray(data));
            
            // 处理不同的返回格式
            let list = [];
            if (Array.isArray(data)) {
                list = data;
            } else if (data && data.data && Array.isArray(data.data)) {
                list = data.data;
            } else if (data && typeof data === 'object') {
                list = [data];
            }
            
            achievements = list;
            console.log('[AchievementManagement] 处理后的数据:', achievements);
            console.log('[AchievementManagement] 数据条数:', achievements.length);
            
            renderList();
            
        } catch (error) {
            console.error('[AchievementManagement] 加载数据失败:', error);
            const container = document.getElementById('achievement-tab-content');
            if (container) {
                container.innerHTML = '<div class="error-message">数据加载失败，请刷新重试</div>';
            }
        }
    }

    /**
     * 渲染列表
     */
    function renderList() {
        const container = document.getElementById('ach-list');
        if (!container) return;
        
        if (achievements.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>暂无项目成果数据</p></div>';
            return;
        }
        
        container.innerHTML = achievements.map(item => {
            const achId = item.id || 'null';
            
            return `
                <div class="data-card">
                    <div class="data-card-header">
                        <div>
                            <div class="data-card-title">${escapeHtml(item.content || '未命名')}</div>
                        </div>
                        <div class="data-card-actions">
                            <button class="btn btn-edit" onclick="window.achievementManagement.editAchievement(${achId})">编辑</button>
                            <button class="btn btn-delete" onclick="window.achievementManagement.deleteAchievement(${achId})">删除</button>
                        </div>
                    </div>
                    <div class="data-card-meta">
                        <span class="meta-item">项目ID: ${item.projectId || '-'}</span>
                        <span class="meta-item">排序: ${item.sortOrder || 0}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    /**
     * 显示编辑表单
     */
    async function showAchievementForm(data = null) {
        const isEdit = !!data;
        
        // 加载项目列表用于选择
        let projectOptions = '';
        try {
            const response = await fetch('/api/resume/experiences');
            if (response.ok) {
                const experiencesData = await response.json();
                const experiences = Array.isArray(experiencesData) ? experiencesData : (experiencesData.data || []);
                
                // 提取所有项目
                projects = [];
                experiences.forEach(exp => {
                    if (exp.projects) {
                        exp.projects.forEach(proj => {
                            projects.push(proj);
                        });
                    }
                });
                
                projectOptions = projects.map(p => 
                    `<option value="${p.id}" ${data?.projectId === p.id ? 'selected' : ''}>${escapeHtml((p.name || '未命名').substring(0, 40))}</option>`
                ).join('');
            }
        } catch (error) {
            console.error('[AchievementManagement] 加载项目列表失败:', error);
        }
        
        const formHtml = `
            <div class="form-group">
                <label>关联项目 *</label>
                <select name="projectId" id="ach-f-projectId" required>${projectOptions}</select>
            </div>
            <div class="form-group">
                <label>成果内容 *</label>
                <textarea name="content" id="ach-f-content" rows="4" required>${escapeHtml(data?.content || '')}</textarea>
            </div>
            <div class="form-group">
                <label>排序序号</label>
                <input type="number" name="sortOrder" id="ach-f-sortOrder" value="${data?.sortOrder ?? 0}">
            </div>
            <div class="form-actions">
                <button type="button" class="btn btn-cancel" onclick="achievementManagement.closeModal()">取消</button>
                <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
            </div>
        `;
        
        openModal(isEdit ? '编辑项目成果' : '新增项目成果', formHtml);
        
        const form = document.getElementById('modal-form');
        if (!form) {
            console.error('[AchievementManagement] 表单元素不存在');
            return;
        }
        
        form.onsubmit = async (e) => {
                e.preventDefault();
                
                const fd = new FormData(form);
                console.log('[AchievementManagement] FormData内容:');
                for (let [key, value] of fd.entries()) {
                    console.log(`  ${key}: ${value}`);
                }
                
                const payload = {
                    projectId: parseInt(fd.get('projectId')),
                    content: fd.get('content'),
                    sortOrder: parseInt(fd.get('sortOrder')) || 0
                };
                
                console.log('[AchievementManagement] 最终提交数据:', payload);
                
                try {
                    if (isEdit) {
                        await apiRequest(`/api/resume/manage/achievements/${data.id}`, 'PUT', payload);
                    } else {
                        await apiRequest('/api/resume/manage/achievements', 'POST', payload);
                    }
                    closeModal();
                    loadData();
                } catch (error) {
                    console.error('[AchievementManagement] 保存失败:', error);
                    alert('保存失败: ' + error.message);
                }
            };
    }

    /**
     * 编辑项目成果
     */
    function editAchievement(id) {
        console.log('[AchievementManagement] 编辑ID:', id, typeof id);
        const achievement = achievements.find(a => a.id === id || String(a.id) === String(id));
        console.log('[AchievementManagement] 找到数据:', achievement);
        if (achievement) {
            showAchievementForm(achievement);
        } else {
            console.error('[AchievementManagement] 未找到ID为', id, '的项目成果');
            alert('未找到对应的项目成果数据');
        }
    }

    /**
     * 删除项目成果
     */
    async function deleteAchievement(id) {
        if (!confirm('确定删除该项目成果？')) return;
        
        try {
            await apiRequest(`/api/resume/manage/achievements/${id}`, 'DELETE');
            loadData();
        } catch (error) {
            console.error('[AchievementManagement] 删除失败:', error);
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
        modal.innerHTML = '<div class="modal-content"><div class="modal-header"><h3 id="modal-title">' + title + '</h3><button class="modal-close" onclick="achievementManagement.closeModal()">&times;</button></div><form id="modal-form" class="modal-form">' + content + '</form></div>';
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
        console.log('[AchievementManagement] 组件销毁');
        achievements = [];
    }

    // 暴露全局方法
    window.achievementManagement = {
        showAchievementForm,
        editAchievement,
        deleteAchievement,
        closeModal
    };

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('achievement-management', {
            init,
            destroy
        });
        console.log('[AchievementManagement] 组件已注册');
    }

})();
