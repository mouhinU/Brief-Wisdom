/**
 * 项目成果管理组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    const apiRequest = (...args) => window.apiRequest(...args);
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
                <div class="form-field-with-ai">
                    <textarea name="content" id="ach-f-content" rows="4" required>${escapeHtml(data?.content || '')}</textarea>
                    <button type="button" class="ai-polish-btn" onclick="window.achievementManagement.polishAchievement()" title="AI润色">✨ AI</button>
                </div>
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
        const confirmed = await showConfirmDialog('确定删除该项目成果？', '🗑️');
        if (!confirmed) return;
        
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
     * AI 润色项目成果
     */
    async function polishAchievement() {
        const textarea = document.getElementById('ach-f-content');
        if (!textarea) return;

        const originalText = textarea.value.trim();
        if (!originalText) {
            alert('请先输入需要润色的文本');
            return;
        }

        // 获取关联的项目名称作为上下文
        const projectId = document.getElementById('ach-f-projectId')?.value;
        let context = '';
        if (projectId) {
            const project = projects.find(p => p.id == projectId);
            if (project) {
                context = project.name || '';
            }
        }

        // 显示加载状态
        const btn = textarea.parentElement.querySelector('.ai-polish-btn');
        const originalBtnText = btn ? btn.innerHTML : '';
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '⏳ 润色中...';
            btn.style.opacity = '0.6';
        }
        textarea.style.borderColor = '#6366f1';

        // 显示全局加载遮罩，明确提示用户请求正在进行（由 ai-polish.js 提供）
        if (typeof window.showAiPolishLoading === 'function') {
            window.showAiPolishLoading();
        }

        try {
            const response = await fetch('/api/resume/ai/polish', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: originalText,
                    fieldType: 'achievement',
                    context: context
                })
            });

            if (!response.ok) throw new Error('请求失败: ' + response.status);

            const responseJson = await response.json();
            
            // 处理 Result 包装格式: { success, code, msg, data }
            let polishedText;
            let errorMsg;
            
            if (responseJson.data && typeof responseJson.data === 'object') {
                // Result 包装格式
                polishedText = responseJson.data.result;
                errorMsg = responseJson.data.error;
            } else if (responseJson.result) {
                // 直接格式（兼容旧版本）
                polishedText = responseJson.result;
                errorMsg = responseJson.error;
            } else {
                throw new Error('响应数据格式异常');
            }
            
            if (errorMsg) {
                alert(errorMsg);
            } else if (polishedText) {
                // 优先使用 AiPolishComponent 的对比弹窗
                if (window.AiPolishComponent && window.AiPolishComponent.showComparisonModal) {
                    window.AiPolishComponent.showComparisonModal(originalText, polishedText, textarea);
                } else if (window.OnlineEditor && window.OnlineEditor.showPolishComparison) {
                    // 降级：使用在线编辑器的对比弹窗
                    window.OnlineEditor.showPolishComparison(originalText, polishedText, textarea);
                } else {
                    // 最终降级：使用自定义确认弹窗
                    const confirmed = await showConfirmDialog(
                        'AI 润色结果：\n\n' + polishedText + '\n\n' +
                        '点击"确定"替换原文本，点击"取消"保留原文。', '⚠️'
                    );
                    if (confirmed) {
                        textarea.value = polishedText;
                    }
                }
            } else {
                alert('AI 润色结果为空');
            }
        } catch (error) {
            console.error('[AchievementManagement] AI润色失败:', error);
            alert('AI 润色失败: ' + error.message);
        } finally {
            // 恢复按钮状态
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalBtnText;
                btn.style.opacity = '1';
            }
            textarea.style.borderColor = '';
            // 移除全局加载遮罩
            if (typeof window.hideAiPolishLoading === 'function') {
                window.hideAiPolishLoading();
            }
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
        closeModal,
        polishAchievement
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
