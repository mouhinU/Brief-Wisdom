/**
 * 在线编辑组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <div class="editor-container">
                <!-- 步骤导航 -->
                <div class="editor-steps">
                    <button class="step-btn active" data-step="1" onclick="goToStep(1)">
                        <span class="step-num">1</span> 工作经历 & 技术栈
                    </button>
                    <button class="step-btn" data-step="2" onclick="goToStep(2)">
                        <span class="step-num">2</span> 项目 & 成果
                    </button>
                    <button class="step-btn preview-step-btn" onclick="previewResume()">
                        <span class="step-num">👁</span> 预览简历
                    </button>
                </div>
                <!-- 编辑器主体 -->
                <div class="editor-body">
                    <div class="editor-list-panel" id="editor-list-panel"></div>
                    <div class="editor-form-panel" id="editor-form-panel">
                        <div class="editor-empty">请从左侧选择或新增项目进行编辑</div>
                    </div>
                </div>
            </div>

            <!-- 简历预览弹窗 -->
            <div id="resume-preview-modal" class="resume-preview-overlay" style="display:none;">
                <div class="resume-preview-container">
                    <div class="resume-preview-toolbar">
                        <h3>简历预览</h3>
                        <div class="resume-preview-actions">
                            <button class="editor-btn editor-btn-secondary" onclick="refreshPreview()">🔄 刷新</button>
                            <button class="editor-btn editor-btn-primary" onclick="openFullResume()">🔗 完整页面</button>
                            <button class="editor-btn editor-btn-danger" onclick="closePreview()">✕ 关闭</button>
                        </div>
                    </div>
                    <div class="resume-preview-body" id="resume-preview-body">
                        <div class="editor-empty">点击"刷新"加载简历预览...</div>
                    </div>
                </div>
            </div>

            <!-- 通用弹窗 -->
            <div id="modal" class="modal" style="display:none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="modal-title">新增</h3>
                        <button class="modal-close" onclick="closeModal()">×</button>
                    </div>
                    <form id="modal-form" class="modal-form"></form>
                </div>
            </div>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[OnlineEditorTemplate] 容器不存在');
            return;
        }
        
        container.innerHTML = generateHTML();
        console.log('[OnlineEditorTemplate] HTML 渲染完成');
    }

    window.OnlineEditorTemplate = {
        generateHTML,
        render
    };

    console.log('[OnlineEditorTemplate] 在线编辑模板加载成功');
})();
