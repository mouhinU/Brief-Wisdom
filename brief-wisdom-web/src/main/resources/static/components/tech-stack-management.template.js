/**
 * 技术栈组件 - HTML 模板生成器
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
                    <h2>技术栈管理</h2>
                    <button class="btn btn-primary" onclick="window.techStackManagement.showTechStackForm()">+ 新增技术栈</button>
                </div>
                <div id="stack-list" class="data-list"></div>
            </section>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[TechStackTemplate] 容器不存在');
            return;
        }
        
        container.innerHTML = generateHTML();
        console.log('[TechStackTemplate] HTML 渲染完成');
    }

    window.TechStackManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[TechStackTemplate] 技术栈模板加载成功');
})();
