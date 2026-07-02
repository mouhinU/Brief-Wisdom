/**
 * 在线编辑组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    // 当前步骤
    let currentStep = 1;
    
    // 数据缓存
    const dataCache = {
        experiences: [],
        projects: [],
        achievements: [],
        techStacks: []
    };

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[OnlineEditor] 组件初始化');
        
        // 加载模板
        if (window.OnlineEditorTemplate) {
            const container = document.getElementById('online-editor-tab-content');
            if (container) {
                OnlineEditorTemplate.render(container);
                console.log('[OnlineEditor] 模板渲染完成');
                
                // 绑定事件
                bindEvents();
                
                // 加载初始数据
                await loadInitialData();
            } else {
                console.error('[OnlineEditor] 容器不存在');
            }
        } else {
            console.error('[OnlineEditor] 模板未加载');
        }
    }

    /**
     * 绑定事件
     */
    function bindEvents() {
        // 步骤切换
        document.querySelectorAll('.step-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const step = parseInt(this.dataset.step);
                if (step) {
                    goToStep(step);
                }
            });
        });
    }

    /**
     * 加载初始数据
     */
    async function loadInitialData() {
        try {
            console.log('[OnlineEditor] 开始加载数据');
            
            // 从后端加载工作经历数据
            const response = await fetch('/api/resume/experiences');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            console.log('[OnlineEditor] 数据加载成功:', data);
            
            // 处理不同的返回格式
            let experiences = [];
            if (Array.isArray(data)) {
                experiences = data;
            } else if (data && Array.isArray(data.data)) {
                experiences = data.data;
            }
            
            if (experiences.length > 0) {
                // 缓存数据
                dataCache.experiences = experiences;
                
                // 提取所有项目和技术栈
                dataCache.projects = [];
                dataCache.achievements = [];
                dataCache.techStacks = [];
                
                experiences.forEach(exp => {
                    if (exp.projects) {
                        dataCache.projects.push(...exp.projects);
                        exp.projects.forEach(proj => {
                            if (proj.achievements) {
                                dataCache.achievements.push(...proj.achievements);
                            }
                        });
                    }
                    if (exp.stacks) {
                        dataCache.techStacks.push(...exp.stacks);
                    }
                });
                
                // 去重技术栈
                dataCache.techStacks = [...new Set(dataCache.techStacks)];
                
                console.log(`[OnlineEditor] 数据解析完成: ${experiences.length}段经历, ${dataCache.projects.length}个项目, ${dataCache.techStacks.length}个技术栈`);
                
                // 渲染第一步的内容
                renderExperiencePanel();
            } else {
                console.warn('[OnlineEditor] 没有工作经历数据');
            }
        } catch (error) {
            console.error('[OnlineEditor] 加载数据失败:', error);
            // 显示错误提示
            const listPanel = document.getElementById('editor-list-panel');
            if (listPanel) {
                listPanel.innerHTML = '<div class="editor-empty" style="color: #dc3545;">数据加载失败，请刷新重试</div>';
            }
        }
    }

    /**
     * 切换到指定步骤
     */
    window.goToStep = function(step) {
        currentStep = step;
        
        // 更新按钮状态
        document.querySelectorAll('.step-btn').forEach(btn => {
            btn.classList.remove('active');
            if (parseInt(btn.dataset.step) === step) {
                btn.classList.add('active');
            }
        });
        
        // 根据步骤加载不同的内容
        switch(step) {
            case 1:
                renderExperiencePanel();
                break;
            case 2:
                renderProjectPanel();
                break;
        }
        
        console.log(`[OnlineEditor] 切换到步骤 ${step}`);
    };

    /**
     * 渲染工作经历面板
     */
    function renderExperiencePanel() {
        const listPanel = document.getElementById('editor-list-panel');
        const formPanel = document.getElementById('editor-form-panel');
        
        if (!listPanel || !formPanel) return;
        
        // 渲染左侧列表
        if (dataCache.experiences.length === 0) {
            listPanel.innerHTML = '<div class="editor-empty">暂无工作经历</div>';
            formPanel.innerHTML = '<div class="editor-empty">点击“新增”按钮添加工作经历</div>';
            return;
        }
        
        // 生成列表 HTML
        let listHtml = '<div class="editor-list">';
        dataCache.experiences.forEach((exp, index) => {
            const projectCount = exp.projects ? exp.projects.length : 0;
            const stackCount = exp.stacks ? exp.stacks.length : 0;
            
            listHtml += `
                <div class="editor-list-item" onclick="selectExperience(${index})">
                    <div class="editor-list-item-title">${exp.title || '未命名'}</div>
                    <div class="editor-list-item-meta">
                        <span>${exp.job || '无职位'}</span>
                        <span>${projectCount}个项目</span>
                        <span>${stackCount}个技术栈</span>
                    </div>
                </div>
            `;
        });
        listHtml += '</div>';
        
        listPanel.innerHTML = listHtml;
        
        // 默认选中第一个
        selectExperience(0);
    }

    /**
     * 选中的工作经历索引
     */
    let selectedExperienceIndex = -1;
    
    /**
     * 选择工作经历
     */
    window.selectExperience = function(index) {
        if (index < 0 || index >= dataCache.experiences.length) return;
        
        selectedExperienceIndex = index;
        const exp = dataCache.experiences[index];
        
        // 更新列表选中状态
        document.querySelectorAll('.editor-list-item').forEach((item, i) => {
            item.classList.toggle('active', i === index);
        });
        
        // 渲染右侧表单
        renderExperienceForm(exp);
    };
    
    /**
     * 渲染工作经历表单
     */
    function renderExperienceForm(exp) {
        const formPanel = document.getElementById('editor-form-panel');
        if (!formPanel) return;
        
        const projectCount = exp.projects ? exp.projects.length : 0;
        const stackCount = exp.stacks ? exp.stacks.length : 0;
        
        formPanel.innerHTML = `
            <div class="editor-form">
                <h3>工作经历详情</h3>
                <div class="form-group">
                    <label>公司名称</label>
                    <input type="text" class="form-control" value="${exp.title || ''}" readonly>
                </div>
                <div class="form-group">
                    <label>职位</label>
                    <input type="text" class="form-control" value="${exp.job || ''}" readonly>
                </div>
                <div class="form-group">
                    <label>描述</label>
                    <textarea class="form-control" rows="4" readonly>${exp.description || ''}</textarea>
                </div>
                <div class="form-group">
                    <label>包含项目 (${projectCount}个)</label>
                    <div class="sub-list">
                        ${(exp.projects || []).map(p => `<div class="sub-item">${p.name}</div>`).join('')}
                    </div>
                </div>
                <div class="form-group">
                    <label>技术栈 (${stackCount}个)</label>
                    <div class="tech-tags">
                        ${(exp.stacks || []).map(s => `<span class="tech-tag">${s}</span>`).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 选中的项目索引
     */
    let selectedProjectIndex = -1;
    
    /**
     * 选择项目
     */
    window.selectProject = function(index) {
        if (index < 0 || index >= dataCache.projects.length) return;
        
        selectedProjectIndex = index;
        const proj = dataCache.projects[index];
        
        // 更新列表选中状态
        document.querySelectorAll('.editor-list-item').forEach((item, i) => {
            item.classList.toggle('active', i === index);
        });
        
        // 渲染右侧表单
        renderProjectForm(proj);
    };
    
    /**
     * 渲染项目表单
     */
    function renderProjectForm(proj) {
        const formPanel = document.getElementById('editor-form-panel');
        if (!formPanel) return;
        
        const achievementCount = proj.achievements ? proj.achievements.length : 0;
        
        formPanel.innerHTML = `
            <div class="editor-form">
                <h3>项目详情</h3>
                <div class="form-group">
                    <label>项目名称</label>
                    <input type="text" class="form-control" value="${proj.name || ''}" readonly>
                </div>
                <div class="form-group">
                    <label>项目周期</label>
                    <input type="text" class="form-control" value="${proj.lifecycle || ''}" readonly>
                </div>
                <div class="form-group">
                    <label>项目背景</label>
                    <textarea class="form-control" rows="3" readonly>${proj.background || ''}</textarea>
                </div>
                <div class="form-group">
                    <label>个人职责</label>
                    <textarea class="form-control" rows="3" readonly>${proj.duty || ''}</textarea>
                </div>
                <div class="form-group">
                    <label>项目成果 (${achievementCount}项)</label>
                    <div class="sub-list">
                        ${(proj.achievements || []).map(a => `<div class="sub-item">${a}</div>`).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 渲染项目面板
     */
    function renderProjectPanel() {
        const listPanel = document.getElementById('editor-list-panel');
        const formPanel = document.getElementById('editor-form-panel');
        
        if (!listPanel || !formPanel) return;
        
        // 渲染左侧列表
        if (dataCache.projects.length === 0) {
            listPanel.innerHTML = '<div class="editor-empty">暂无项目</div>';
            formPanel.innerHTML = '<div class="editor-empty">请先添加工作经历和项目</div>';
            return;
        }
        
        // 生成列表 HTML
        let listHtml = '<div class="editor-list">';
        dataCache.projects.forEach((proj, index) => {
            const achievementCount = proj.achievements ? proj.achievements.length : 0;
            
            listHtml += `
                <div class="editor-list-item" onclick="selectProject(${index})">
                    <div class="editor-list-item-title">${proj.name || '未命名'}</div>
                    <div class="editor-list-item-meta">
                        <span>${proj.lifecycle || '无周期'}</span>
                        <span>${achievementCount}项成果</span>
                    </div>
                </div>
            `;
        });
        listHtml += '</div>';
        
        listPanel.innerHTML = listHtml;
        
        // 默认选中第一个
        selectProject(0);
    }

    /**
     * 预览简历
     */
    window.previewResume = function() {
        const modal = document.getElementById('resume-preview-modal');
        if (!modal) return;
        
        // 检查是否有数据
        if (dataCache.experiences.length === 0) {
            console.warn('[OnlineEditor] 没有数据可预览');
            alert('暂无简历数据，请先添加工作经历');
            return;
        }
        
        modal.style.display = 'flex';
        console.log('[OnlineEditor] 打开简历预览');
        
        // 自动刷新预览内容
        refreshPreview();
    };

    /**
     * 刷新预览
     */
    window.refreshPreview = function() {
        console.log('[OnlineEditor] 刷新预览');
        
        const previewBody = document.getElementById('resume-preview-body');
        if (!previewBody) return;
        
        // 生成预览 HTML
        let html = '<div class="resume-preview-content">';
        
        dataCache.experiences.forEach(exp => {
            html += `
                <div class="preview-section">
                    <h3>${exp.title || '未命名'}</h3>
                    <p class="preview-job">${exp.job || ''}</p>
                    <p class="preview-desc">${exp.description || ''}</p>
            `;
            
            if (exp.projects && exp.projects.length > 0) {
                html += '<div class="preview-projects">';
                exp.projects.forEach(proj => {
                    html += `
                        <div class="preview-project">
                            <h4>${proj.name}</h4>
                            <p class="preview-lifecycle">${proj.lifecycle || ''}</p>
                            <p>${proj.background || ''}</p>
                            <p><strong>职责：</strong>${proj.duty || ''}</p>
                    `;
                    
                    if (proj.achievements && proj.achievements.length > 0) {
                        html += '<ul class="preview-achievements">';
                        proj.achievements.forEach(a => {
                            html += `<li>${a}</li>`;
                        });
                        html += '</ul>';
                    }
                    
                    html += '</div>';
                });
                html += '</div>';
            }
            
            if (exp.stacks && exp.stacks.length > 0) {
                html += '<div class="preview-stacks">';
                exp.stacks.forEach(s => {
                    html += `<span class="preview-stack-tag">${s}</span>`;
                });
                html += '</div>';
            }
            
            html += '</div>';
        });
        
        html += '</div>';
        
        previewBody.innerHTML = html;
    };

    /**
     * 打开完整简历页面
     */
    window.openFullResume = function() {
        window.open('/about.html', '_blank');
    };

    /**
     * 关闭预览
     */
    window.closePreview = function() {
        const modal = document.getElementById('resume-preview-modal');
        if (modal) {
            modal.style.display = 'none';
        }
    };

    /**
     * 销毁组件
     */
    function destroy() {
        console.log('[OnlineEditor] 组件销毁');
        currentStep = 1;
        // 清理缓存
        Object.keys(dataCache).forEach(key => {
            dataCache[key] = [];
        });
    }

    // 注册组件
    registerComponent('online-editor', {
        init,
        destroy
    });

    console.log('[OnlineEditor] 在线编辑组件加载成功');
})();
