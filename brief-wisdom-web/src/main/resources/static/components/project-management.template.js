/**
 * 项目经历组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <section class="settings-section">
                <div class="section-header">
                    <h2>项目管理</h2>
                    <button class="btn btn-primary" onclick="window.projectManagement.showProjectForm()">+ 新增项目</button>
                </div>
                <div class="filter-bar">
                    <label>筛选工作经历：</label>
                    <select id="proj-experience-filter" onchange="window.projectManagement.loadData()">
                        <option value="">全部</option>
                    </select>
                </div>
                <div id="proj-list" class="data-list"></div>
            </section>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[ProjectTemplate] 容器不存在');
            return;
        }
        
        container.innerHTML = generateHTML();
        console.log('[ProjectTemplate] HTML 渲染完成');
    }

    window.ProjectManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[ProjectTemplate] 项目经历模板加载成功');
})();
