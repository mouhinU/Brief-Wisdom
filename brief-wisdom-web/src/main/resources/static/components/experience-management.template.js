/**
 * 工作经历组件 - HTML 模板生成器
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
                    <h2>工作经历管理</h2>
                    <button class="btn btn-primary" onclick="window.experienceManagement.showExperienceForm()">+ 新增工作经历</button>
                </div>
                <div id="exp-list" class="data-list"></div>
            </section>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[ExperienceTemplate] 容器不存在');
            return;
        }
        
        container.innerHTML = generateHTML();
        console.log('[ExperienceTemplate] HTML 渲染完成');
    }

    window.ExperienceManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[ExperienceTemplate] 工作经历模板加载成功');
})();
