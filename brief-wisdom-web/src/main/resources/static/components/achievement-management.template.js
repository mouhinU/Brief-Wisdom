/**
 * 项目成果组件 - HTML 模板生成器
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
                    <h2>项目成果管理</h2>
                    <button class="btn btn-primary" onclick="window.achievementManagement.showAchievementForm()">+ 新增项目成果</button>
                </div>
                <div id="ach-list" class="data-list"></div>
            </section>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[AchievementTemplate] 容器不存在');
            return;
        }
        
        container.innerHTML = generateHTML();
        console.log('[AchievementTemplate] HTML 渲染完成');
    }

    window.AchievementManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[AchievementTemplate] 项目成果模板加载成功');
})();
