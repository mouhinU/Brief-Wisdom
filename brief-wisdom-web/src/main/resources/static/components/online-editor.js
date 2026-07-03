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
        console.log('[OnlineEditor] 组件初始化', options);
        
        // 加载模板
        if (window.OnlineEditorTemplate) {
            const containerId = options.containerId || 'editor-tab-content';
            const container = document.getElementById(containerId);
            
            if (container) {
                OnlineEditorTemplate.render(container);
                console.log('[OnlineEditor] 模板渲染完成');
                
                // 绑定事件
                bindEvents(container);
                
                // 加载初始数据
                await loadInitialData();
            } else {
                console.error('[OnlineEditor] 容器不存在:', containerId);
            }
        } else {
            console.error('[OnlineEditor] 模板未加载');
        }
    }

    /**
     * 绑定事件
     */
    function bindEvents(container) {
        // 步骤切换（限定在当前容器内）
        container.querySelectorAll('.step-btn').forEach(btn => {
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
            
            // 从管理端点加载工作经历数据
            const response = await fetch('/api/resume/manage/experiences');
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
                // 缓存经历数据
                dataCache.experiences = experiences;
                
                // 并行加载项目、技术栈、成果数据
                await Promise.all([
                    loadProjects(),
                    loadTechStacks(),
                    loadAchievements()
                ]);
                
                // 组装嵌套结构
                assembleNestedData();
                
                console.log(`[OnlineEditor] 数据解析完成: ${dataCache.experiences.length}段经历, ${dataCache.projects.length}个项目, ${dataCache.techStacks.length}个技术栈`);
                
                // 渲染第一步的内容
                renderExperiencePanel();
            } else {
                console.warn('[OnlineEditor] 没有工作经历数据');
                showEmptyState();
            }
        } catch (error) {
            console.error('[OnlineEditor] 加载数据失败:', error);
            showError('数据加载失败，请刷新重试');
        }
    }

    /**
     * 加载项目数据
     */
    async function loadProjects() {
        try {
            const response = await fetch('/api/resume/manage/projects');
            if (!response.ok) return;
            
            const data = await response.json();
            dataCache.projects = Array.isArray(data) ? data : (data.data || []);
        } catch (error) {
            console.error('[OnlineEditor] 加载项目失败:', error);
        }
    }

    /**
     * 加载技术栈数据
     */
    async function loadTechStacks() {
        try {
            const response = await fetch('/api/resume/manage/stacks');
            if (!response.ok) return;
            
            const data = await response.json();
            dataCache.techStacks = Array.isArray(data) ? data : (data.data || []);
        } catch (error) {
            console.error('[OnlineEditor] 加载技术栈失败:', error);
        }
    }

    /**
     * 加载项目成果数据
     */
    async function loadAchievements() {
        try {
            const response = await fetch('/api/resume/manage/achievements');
            if (!response.ok) return;
            
            const data = await response.json();
            dataCache.achievements = Array.isArray(data) ? data : (data.data || []);
        } catch (error) {
            console.error('[OnlineEditor] 加载成果失败:', error);
        }
    }

    /**
     * 组装嵌套数据结构
     */
    function assembleNestedData() {
        dataCache.experiences.forEach(exp => {
            // 关联项目
            exp.projects = dataCache.projects.filter(p => p.experienceId == exp.id);
            
            // 关联技术栈
            exp.stacks = dataCache.techStacks
                .filter(s => s.experienceId == exp.id)
                .map(s => s.techName);
            
            // 为每个项目关联成果
            exp.projects.forEach(proj => {
                proj.achievements = dataCache.achievements
                    .filter(a => a.projectId == proj.id)
                    .map(a => a.content);
            });
        });
    }

    /**
     * 显示空状态
     */
    function showEmptyState() {
        const listBody = document.getElementById('editor-list-body');
        const formPanel = document.getElementById('editor-form-panel');
        
        if (listBody) {
            listBody.innerHTML = '<div class="editor-empty">暂无工作经历<br><button class="editor-btn editor-btn-add" onclick="OnlineEditor.addItem()" style="margin-top:12px;">+ 新增工作经历</button></div>';
        }
        if (formPanel) {
            formPanel.innerHTML = '<div class="editor-empty">请从左侧选择或新增项目进行编辑</div>';
        }
    }

    /**
     * 显示错误信息
     */
    function showError(message) {
        const listBody = document.getElementById('editor-list-body');
        if (listBody) {
            listBody.innerHTML = `<div class="editor-empty" style="color: #dc3545;">${message}</div>`;
        }
    }

    /**
     * 切换到指定步骤
     */
    function goToStep(step) {
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
    }

    /**
     * 渲染工作经历面板
     */
    function renderExperiencePanel() {
        const listBody = document.getElementById('editor-list-body');
        const formPanel = document.getElementById('editor-form-panel');
        const listTitle = document.getElementById('editor-list-title');
        
        if (!listBody || !formPanel) return;
        
        // 更新列表头部
        if (listTitle) listTitle.textContent = '工作经历';
        
        // 渲染左侧列表
        if (dataCache.experiences.length === 0) {
            listBody.innerHTML = '<div class="editor-empty">暂无工作经历<br><button class="editor-btn editor-btn-add" onclick="OnlineEditor.addItem()" style="margin-top:12px;">+ 新增工作经历</button></div>';
            formPanel.innerHTML = '<div class="editor-empty">点击左上方"+ 新增"按钮添加工作经历</div>';
            return;
        }
        
        // 生成列表 HTML
        let listHtml = '<div class="editor-list">';
        dataCache.experiences.forEach((exp, index) => {
            const projectCount = exp.projects ? exp.projects.length : 0;
            const stackCount = exp.stacks ? exp.stacks.length : 0;
            
            listHtml += `
                <div class="editor-list-item" onclick="OnlineEditor.selectExperience(${index})">
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
        
        listBody.innerHTML = listHtml;
        
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
    function selectExperience(index) {
        if (index < 0 || index >= dataCache.experiences.length) return;
        
        selectedExperienceIndex = index;
        const exp = dataCache.experiences[index];
        
        // 更新列表选中状态
        document.querySelectorAll('.editor-list-item').forEach((item, i) => {
            item.classList.toggle('active', i === index);
        });
        
        // 渲染右侧表单
        renderExperienceForm(exp);
    }
    
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
                <h3 class="editor-form-title">工作经历详情</h3>
                <div class="editor-form-group">
                    <label>公司名称</label>
                    <input type="text" id="editor-field-title" value="${escapeAttr(exp.title || '')}">
                </div>
                <div class="editor-form-group">
                    <label>职位</label>
                    <input type="text" id="editor-field-job" value="${escapeAttr(exp.job || '')}">
                </div>
                <div class="editor-form-group">
                    <label>描述</label>
                    <div class="editor-field-with-ai">
                        <textarea id="editor-field-description" rows="4">${escapeHtml(exp.description || '')}</textarea>
                        <button class="editor-ai-polish-btn" onclick="OnlineEditor.polishText('editor-field-description', 'description', '${escapeAttr(exp.title || '')}')" title="AI润色">✨ AI</button>
                    </div>
                </div>
                <div class="editor-form-group">
                    <label>包含项目 (${projectCount}个)</label>
                    <div class="sub-list">
                        ${(exp.projects || []).map(p => `<div class="sub-item">${escapeHtml(p.name)}</div>`).join('')}
                    </div>
                </div>
                <div class="editor-form-group">
                    <label>技术栈 (${stackCount}个)</label>
                    <div class="tech-tags">
                        ${(exp.stacks || []).map(s => `<span class="tech-tag">${escapeHtml(s)}</span>`).join('')}
                    </div>
                    <button class="editor-btn editor-btn-add" onclick="OnlineEditor.addTechStack()" style="margin-top:8px;">+ 添加技术栈</button>
                </div>
                <div class="editor-form-actions">
                    <button class="editor-btn editor-btn-primary" onclick="OnlineEditor.saveExperienceForm()">保存修改</button>
                    <button class="editor-btn editor-btn-danger" onclick="OnlineEditor.deleteExperience(${selectedExperienceIndex})">删除经历</button>
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
    function selectProject(index) {
        if (index < 0 || index >= dataCache.projects.length) return;
        
        selectedProjectIndex = index;
        const proj = dataCache.projects[index];
        
        // 更新列表选中状态
        document.querySelectorAll('.editor-list-item').forEach((item, i) => {
            item.classList.toggle('active', i === index);
        });
        
        // 渲染右侧表单
        renderProjectForm(proj);
    }
    
    /**
     * 渲染项目表单
     */
    function renderProjectForm(proj) {
        const formPanel = document.getElementById('editor-form-panel');
        if (!formPanel) return;
        
        const achievementCount = proj.achievements ? proj.achievements.length : 0;
        
        formPanel.innerHTML = `
            <div class="editor-form">
                <h3 class="editor-form-title">项目详情</h3>
                <div class="editor-form-group">
                    <label>项目名称</label>
                    <input type="text" id="editor-proj-name" value="${escapeAttr(proj.name || '')}">
                </div>
                <div class="editor-form-group">
                    <label>项目周期</label>
                    <input type="text" id="editor-proj-lifecycle" value="${escapeAttr(proj.lifecycle || '')}">
                </div>
                <div class="editor-form-group">
                    <label>项目背景</label>
                    <div class="editor-field-with-ai">
                        <textarea id="editor-proj-background" rows="3">${escapeHtml(proj.background || '')}</textarea>
                        <button class="editor-ai-polish-btn" onclick="OnlineEditor.polishText('editor-proj-background', 'background', '${escapeAttr(proj.name || '')}')" title="AI润色">✨ AI</button>
                    </div>
                </div>
                <div class="editor-form-group">
                    <label>个人职责</label>
                    <div class="editor-field-with-ai">
                        <textarea id="editor-proj-duty" rows="3">${escapeHtml(proj.duty || '')}</textarea>
                        <button class="editor-ai-polish-btn" onclick="OnlineEditor.polishText('editor-proj-duty', 'duty', '${escapeAttr(proj.name || '')}')" title="AI润色">✨ AI</button>
                    </div>
                </div>
                <div class="editor-form-group">
                    <label>项目成果 (${achievementCount}项)</label>
                    <div class="sub-list">
                        ${(proj.achievements || []).map(a => `<div class="sub-item">${escapeHtml(a)}</div>`).join('')}
                    </div>
                </div>
                <div class="editor-form-actions">
                    <button class="editor-btn editor-btn-primary" onclick="OnlineEditor.saveProjectForm()">保存修改</button>
                    <button class="editor-btn editor-btn-danger" onclick="OnlineEditor.deleteProject(${selectedProjectIndex})">删除项目</button>
                </div>
            </div>
        `;
    }

    /**
     * 渲染项目面板
     */
    function renderProjectPanel() {
        const listBody = document.getElementById('editor-list-body');
        const formPanel = document.getElementById('editor-form-panel');
        const listTitle = document.getElementById('editor-list-title');
        
        if (!listBody || !formPanel) return;
        
        // 更新列表头部
        if (listTitle) listTitle.textContent = '项目经历';
        
        // 渲染左侧列表
        if (dataCache.projects.length === 0) {
            listBody.innerHTML = '<div class="editor-empty">暂无项目<br><button class="editor-btn editor-btn-add" onclick="OnlineEditor.addItem()" style="margin-top:12px;">+ 新增项目</button></div>';
            formPanel.innerHTML = '<div class="editor-empty">请先添加工作经历，再新增项目</div>';
            return;
        }
        
        // 生成列表 HTML
        let listHtml = '<div class="editor-list">';
        dataCache.projects.forEach((proj, index) => {
            const achievementCount = proj.achievements ? proj.achievements.length : 0;
            
            listHtml += `
                <div class="editor-list-item" onclick="OnlineEditor.selectProject(${index})">
                    <div class="editor-list-item-title">${proj.name || '未命名'}</div>
                    <div class="editor-list-item-meta">
                        <span>${proj.lifecycle || '无周期'}</span>
                        <span>${achievementCount}项成果</span>
                    </div>
                </div>
            `;
        });
        listHtml += '</div>';
        
        listBody.innerHTML = listHtml;
        
        // 默认选中第一个
        selectProject(0);
    }

    /**
     * 预览简历
     */
    function previewResume() {
        const modal = document.getElementById('editor-preview-modal');
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
    }

    /**
     * 刷新预览
     */
    function refreshPreview() {
        console.log('[OnlineEditor] 刷新预览');
        
        const previewBody = document.getElementById('editor-preview-body');
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
    }

    /**
     * 打开完整简历页面
     */
    function openFullResume() {
        window.open('/about.html', '_blank');
    }

    /**
     * 关闭预览
     */
    function closePreview() {
        const modal = document.getElementById('editor-preview-modal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    /**
     * 新增按钮点击 - 根据当前步骤分发
     */
    function addItem() {
        if (currentStep === 1) {
            addExperience();
        } else if (currentStep === 2) {
            addProject();
        }
    }

    /**
     * 新增工作经历
     */
    async function addExperience() {
        const title = prompt('请输入公司名称：');
        if (!title || !title.trim()) return;

        const job = prompt('请输入职位：') || '';
        const description = prompt('请输入描述（可留空）：') || '';

        try {
            const payload = {
                title: title.trim(),
                job: job.trim(),
                description: description.trim(),
                sortOrder: dataCache.experiences.length,
                isVisible: 1
            };

            const res = await fetch('/api/resume/manage/experiences', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('创建失败: ' + res.status);

            const newExp = await res.json();
            newExp.projects = [];
            newExp.stacks = [];
            dataCache.experiences.push(newExp);
            renderExperiencePanel();
            selectExperience(dataCache.experiences.length - 1);
            console.log('[OnlineEditor] 工作经历创建成功');
        } catch (error) {
            console.error('[OnlineEditor] 创建工作经历失败:', error);
            alert('创建失败: ' + error.message);
        }
    }

    /**
     * 新增项目（归属当前选中的工作经历）
     */
    async function addProject() {
        if (dataCache.experiences.length === 0) {
            alert('请先添加工作经历');
            return;
        }

        // 如果当前步骤1且有选中经历，用该经历；否则用第一个
        let expIndex = selectedExperienceIndex;
        if (expIndex < 0 || expIndex >= dataCache.experiences.length) {
            expIndex = 0;
        }
        const parentExp = dataCache.experiences[expIndex];

        const name = prompt('请输入项目名称：');
        if (!name || !name.trim()) return;

        const lifecycle = prompt('请输入项目周期（如：2024.01 - 2024.06）：') || '';
        const background = prompt('请输入项目背景（可留空）：') || '';
        const duty = prompt('请输入个人职责（可留空）：') || '';

        try {
            const payload = {
                experienceId: parentExp.id,
                name: name.trim(),
                lifecycle: lifecycle.trim(),
                background: background.trim(),
                duty: duty.trim(),
                sortOrder: (parentExp.projects || []).length
            };

            const res = await fetch('/api/resume/manage/projects', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('创建失败: ' + res.status);

            const newProj = await res.json();
            newProj.achievements = [];
            dataCache.projects.push(newProj);

            // 同步到父经历的 projects 数组
            if (!parentExp.projects) parentExp.projects = [];
            parentExp.projects.push(newProj);

            renderProjectPanel();
            selectProject(dataCache.projects.length - 1);
            console.log('[OnlineEditor] 项目创建成功');
        } catch (error) {
            console.error('[OnlineEditor] 创建项目失败:', error);
            alert('创建失败: ' + error.message);
        }
    }

    /**
     * 新增技术栈（归属当前选中的工作经历）
     */
    async function addTechStack() {
        if (selectedExperienceIndex < 0 || selectedExperienceIndex >= dataCache.experiences.length) {
            alert('请先选择一个工作经历');
            return;
        }
        const exp = dataCache.experiences[selectedExperienceIndex];

        const techName = prompt('请输入技术栈名称：');
        if (!techName || !techName.trim()) return;

        try {
            const payload = {
                experienceId: exp.id,
                techName: techName.trim(),
                sortOrder: (exp.stacks || []).length
            };

            const res = await fetch('/api/resume/manage/stacks', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('创建失败: ' + res.status);

            const newStack = await res.json();
            dataCache.techStacks.push(newStack);

            // 同步到父经历
            if (!exp.stacks) exp.stacks = [];
            exp.stacks.push(newStack.techName);

            renderExperiencePanel();
            selectExperience(selectedExperienceIndex);
            console.log('[OnlineEditor] 技术栈添加成功');
        } catch (error) {
            console.error('[OnlineEditor] 添加技术栈失败:', error);
            alert('添加失败: ' + error.message);
        }
    }

    /**
     * 删除工作经历
     */
    async function deleteExperience(index) {
        if (index < 0 || index >= dataCache.experiences.length) return;
        const exp = dataCache.experiences[index];

        if (!confirm('确定删除工作经历"' + (exp.title || '未命名') + '"吗？关联的项目和技术栈也会被删除。')) return;

        try {
            const res = await fetch('/api/resume/manage/experiences/' + exp.id, { method: 'DELETE' });
            if (!res.ok) throw new Error('删除失败: ' + res.status);

            // 清理缓存
            const removedProjects = exp.projects || [];
            removedProjects.forEach(p => {
                const pi = dataCache.projects.findIndex(cp => cp.id === p.id);
                if (pi >= 0) dataCache.projects.splice(pi, 1);
            });
            dataCache.techStacks = dataCache.techStacks.filter(s => s.experienceId != exp.id);
            dataCache.experiences.splice(index, 1);

            selectedExperienceIndex = -1;
            renderExperiencePanel();
            console.log('[OnlineEditor] 工作经历删除成功');
        } catch (error) {
            console.error('[OnlineEditor] 删除工作经历失败:', error);
            alert('删除失败: ' + error.message);
        }
    }

    /**
     * 删除项目
     */
    async function deleteProject(index) {
        if (index < 0 || index >= dataCache.projects.length) return;
        const proj = dataCache.projects[index];

        if (!confirm('确定删除项目"' + (proj.name || '未命名') + '"吗？')) return;

        try {
            const res = await fetch('/api/resume/manage/projects/' + proj.id, { method: 'DELETE' });
            if (!res.ok) throw new Error('删除失败: ' + res.status);

            // 清理缓存
            dataCache.achievements = dataCache.achievements.filter(a => a.projectId != proj.id);
            const pi = dataCache.projects.findIndex(cp => cp.id === proj.id);
            if (pi >= 0) dataCache.projects.splice(pi, 1);

            // 同步到父经历
            dataCache.experiences.forEach(exp => {
                if (exp.projects) {
                    exp.projects = exp.projects.filter(p => p.id !== proj.id);
                }
            });

            selectedProjectIndex = -1;
            renderProjectPanel();
            console.log('[OnlineEditor] 项目删除成功');
        } catch (error) {
            console.error('[OnlineEditor] 删除项目失败:', error);
            alert('删除失败: ' + error.message);
        }
    }

    /**
     * 保存工作经历表单编辑
     */
    async function saveExperienceForm() {
        if (selectedExperienceIndex < 0 || selectedExperienceIndex >= dataCache.experiences.length) return;
        const exp = dataCache.experiences[selectedExperienceIndex];

        const titleEl = document.getElementById('editor-field-title');
        const jobEl = document.getElementById('editor-field-job');
        const descEl = document.getElementById('editor-field-description');

        if (!titleEl || !jobEl || !descEl) return;

        const newTitle = titleEl.value.trim();
        const newJob = jobEl.value.trim();
        const newDesc = descEl.value.trim();

        if (!newTitle) {
            alert('公司名称不能为空');
            return;
        }

        // 先用表单值更新缓存，确保内容不丢失
        exp.title = newTitle;
        exp.job = newJob;
        exp.description = newDesc;

        try {
            const payload = {
                title: newTitle,
                job: newJob,
                description: newDesc
            };

            const res = await fetch('/api/resume/manage/experiences/' + exp.id, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('保存失败: ' + res.status);

            // 重新渲染（从缓存读取，内容已在上面更新）
            renderExperiencePanel();
            selectExperience(selectedExperienceIndex);
            console.log('[OnlineEditor] 工作经历更新成功');
        } catch (error) {
            console.error('[OnlineEditor] 更新工作经历失败:', error);
            alert('保存失败: ' + error.message);
            // 保存失败也重新渲染，确保缓存和视图一致
            renderExperiencePanel();
            selectExperience(selectedExperienceIndex);
        }
    }

    /**
     * 保存项目表单编辑
     */
    async function saveProjectForm() {
        if (selectedProjectIndex < 0 || selectedProjectIndex >= dataCache.projects.length) return;
        const proj = dataCache.projects[selectedProjectIndex];

        const nameEl = document.getElementById('editor-proj-name');
        const lifecycleEl = document.getElementById('editor-proj-lifecycle');
        const backgroundEl = document.getElementById('editor-proj-background');
        const dutyEl = document.getElementById('editor-proj-duty');

        if (!nameEl) return;

        const newName = nameEl.value.trim();
        const newLifecycle = lifecycleEl ? lifecycleEl.value.trim() : '';
        const newBackground = backgroundEl ? backgroundEl.value.trim() : '';
        const newDuty = dutyEl ? dutyEl.value.trim() : '';

        if (!newName) {
            alert('项目名称不能为空');
            return;
        }

        // 先用表单值更新缓存
        proj.name = newName;
        proj.lifecycle = newLifecycle;
        proj.background = newBackground;
        proj.duty = newDuty;

        try {
            const payload = {
                name: newName,
                lifecycle: newLifecycle,
                background: newBackground,
                duty: newDuty
            };

            const res = await fetch('/api/resume/manage/projects/' + proj.id, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error('保存失败: ' + res.status);

            renderProjectPanel();
            selectProject(selectedProjectIndex);
            console.log('[OnlineEditor] 项目更新成功');
        } catch (error) {
            console.error('[OnlineEditor] 更新项目失败:', error);
            alert('保存失败: ' + error.message);
            renderProjectPanel();
            selectProject(selectedProjectIndex);
        }
    }

    /**
     * AI 文本润色
     * @param {string} fieldId - 目标 textarea 的 DOM id
     * @param {string} fieldType - 字段类型：description/background/duty/achievement
     * @param {string} context - 上下文信息（如公司名、项目名）
     */
    async function polishText(fieldId, fieldType, context) {
        // 检查 AI 开关是否开启
        const aiToggle = document.getElementById('editor-ai-enabled');
        if (aiToggle && !aiToggle.checked) {
            alert('AI 辅助功能已关闭，请在顶部开关中开启');
            return;
        }

        const textarea = document.getElementById(fieldId);
        if (!textarea) return;

        const originalText = textarea.value.trim();
        if (!originalText) {
            alert('请先输入需要润色的文本');
            return;
        }

        // 显示加载状态
        const btn = textarea.parentElement.querySelector('.editor-ai-polish-btn');
        const originalBtnText = btn ? btn.innerHTML : '';
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '⏳ 润色中...';
            btn.style.opacity = '0.6';
        }
        textarea.style.borderColor = '#6366f1';

        try {
            const response = await fetch('/api/resume/ai/polish', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    text: originalText,
                    fieldType: fieldType,
                    context: context || ''
                })
            });

            if (!response.ok) throw new Error('请求失败: ' + response.status);

            const data = await response.json();
            if (data.error) {
                alert(data.error);
            } else if (data.result) {
                // 弹出对比确认
                const confirmed = confirm(
                    'AI 润色结果：\n\n' + data.result + '\n\n' +
                    '点击"确定"替换原文本，点击"取消"保留原文。'
                );
                if (confirmed) {
                    textarea.value = data.result;
                }
            }
        } catch (error) {
            console.error('[OnlineEditor] AI润色失败:', error);
            alert('AI 润色失败: ' + error.message);
        } finally {
            // 恢复按钮状态
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalBtnText;
                btn.style.opacity = '1';
            }
            textarea.style.borderColor = '';
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
     * 属性转义
     */
    function escapeAttr(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

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

    // 导出命名空间
    window.OnlineEditor = {
        init,
        goToStep,
        selectExperience,
        selectProject,
        previewResume,
        refreshPreview,
        openFullResume,
        closePreview,
        addItem,
        addExperience,
        addProject,
        addTechStack,
        deleteExperience,
        deleteProject,
        saveExperienceForm,
        saveProjectForm,
        polishText
    };

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('online-editor', {
            init,
            destroy
        });
    }

    console.log('[OnlineEditor] 在线编辑组件加载成功');
})();
