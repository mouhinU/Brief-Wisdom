/**
 * 模型管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <div class="model-section">
                <div class="section-header">
                    <h2>模型配置</h2>
                    <button class="btn btn-primary" onclick="ModelManagement.showModelForm()">+ 新增模型</button>
                </div>
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>排序</th>
                            <th>模型标识</th>
                            <th>显示名称</th>
                            <th>服务商</th>
                            <th>描述</th>
                            <th>状态</th>
                            <th>激活</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody id="model-table-body"></tbody>
                    </table>
                </div>
            </div>

            <!-- 模型编辑弹窗 -->
            <div id="model-modal" class="modal" style="display:none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="model-modal-title">新增模型</h3>
                        <button class="modal-close" onclick="ModelManagement.closeModelModal()">×</button>
                    </div>
                    <form id="model-form" class="modal-form" onsubmit="ModelManagement.saveModel(event)">
                        <input type="hidden" id="model-id">
                        <div class="form-group">
                            <label>模型标识 *</label>
                            <input type="text" id="model-name-input" required placeholder="例如: qwen-max">
                        </div>
                        <div class="form-group">
                            <label>显示名称 *</label>
                            <input type="text" id="model-display-name" required placeholder="例如: 通义千问 Max">
                        </div>
                        <div class="form-group">
                            <label>服务商</label>
                            <input type="text" id="model-provider" value="dashscope" placeholder="dashscope">
                        </div>
                        <div class="form-group">
                            <label>描述</label>
                            <input type="text" id="model-description" placeholder="模型描述">
                        </div>
                        <div class="form-group">
                            <label>排序</label>
                            <input type="number" id="model-sort-order" value="0" min="0">
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="ModelManagement.closeModelModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
    }

    function render(container) {
        const target = typeof container === 'string' 
            ? document.querySelector(container) 
            : container;
        
        if (!target) {
            console.error('[ModelManagement.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[ModelManagement.Template] 模板已渲染到:', target);
    }

    window.ModelManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[ModelManagement.Template] 模板生成器已注册');
})();
