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
                <!-- AI 辅助开关栏 -->
                <div class="editor-ai-bar" id="editor-ai-bar">
                    <label class="editor-ai-toggle">
                        <input type="checkbox" id="editor-ai-enabled" checked>
                        <span class="editor-ai-toggle-slider"></span>
                    </label>
                    <span class="editor-ai-label">AI 辅助润色</span>
                    <span class="editor-ai-hint">开启后，文本框旁将显示 AI 润色按钮</span>
                </div>
                <!-- 步骤导航 -->
                <div class="editor-steps">
                    <button class="step-btn active" data-step="1" onclick="OnlineEditor.goToStep(1)">
                        <span class="step-num">1</span> 工作经历 & 技术栈
                    </button>
                    <button class="step-btn" data-step="2" onclick="OnlineEditor.goToStep(2)">
                        <span class="step-num">2</span> 项目 & 成果
                    </button>
                    <button class="step-btn preview-step-btn" onclick="OnlineEditor.previewResume()">
                        <span class="step-num">&#128065;</span> 预览简历
                    </button>
                </div>
                <!-- 编辑器主体 -->
                <div class="editor-body">
                    <div class="editor-list-panel" id="editor-list-panel">
                        <div class="editor-list-header" id="editor-list-header">
                            <h3 id="editor-list-title">工作经历</h3>
                            <button class="editor-btn editor-btn-add" id="editor-add-btn" onclick="OnlineEditor.addItem()">+ 新增</button>
                        </div>
                        <div class="editor-list-body" id="editor-list-body">
                            <div class="editor-empty">加载中...</div>
                        </div>
                    </div>
                    <div class="editor-form-panel" id="editor-form-panel">
                        <div class="editor-empty">请从左侧选择或新增项目进行编辑</div>
                    </div>
                </div>
            </div>

            <!-- 简历预览弹窗 -->
            <div id="editor-preview-modal" class="resume-preview-overlay" style="display:none;">
                <div class="resume-preview-container">
                    <div class="resume-preview-toolbar">
                        <h3>简历预览</h3>
                        <div class="resume-preview-actions">
                            <button class="editor-btn editor-btn-secondary" onclick="OnlineEditor.refreshPreview()">&#128260; 刷新</button>
                            <button class="editor-btn editor-btn-primary" onclick="OnlineEditor.openFullResume()">&#128279; 完整页面</button>
                            <button class="editor-btn editor-btn-danger" onclick="OnlineEditor.closePreview()">&#10005; 关闭</button>
                        </div>
                    </div>
                    <div class="resume-preview-body" id="editor-preview-body">
                        <div class="editor-empty">点击"刷新"加载简历预览...</div>
                    </div>
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
