/**
 * AI 文本润色组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-05
 */

(function() {
    'use strict';

    /**
     * 生成 AI 润色按钮的 HTML
     * @param {string} textareaId - 关联的 textarea ID
     * @param {string} fieldType - 字段类型：description/background/duty/achievement
     * @param {string} context - 上下文信息（可选）
     * @returns {string} HTML 字符串
     */
    function generateButtonHTML(textareaId, fieldType, context = '') {
        return `
            <div class="ai-polish-wrapper" 
                 data-textarea-id="${textareaId}" 
                 data-field-type="${fieldType}" 
                 data-context="${escapeAttr(context)}">
                <button type="button" 
                        class="ai-polish-btn" 
                        onclick="window.AiPolishComponent.polish('${textareaId}', '${fieldType}', '${escapeAttr(context)}')" 
                        title="AI润色">
                    ✨ AI
                </button>
            </div>
        `;
    }

    /**
     * 生成带 AI 润色按钮的 textarea HTML
     * @param {Object} options - 配置选项
     * @param {string} options.id - textarea 的 ID
     * @param {string} options.name - textarea 的 name 属性
     * @param {string} options.value - 初始值
     * @param {number} options.rows - 行数
     * @param {string} options.placeholder - 占位符
     * @param {boolean} options.required - 是否必填
     * @param {string} options.fieldType - 字段类型
     * @param {string} options.context - 上下文信息
     * @param {string} options.label - 标签文本
     * @returns {string} HTML 字符串
     */
    function generateTextareaWithPolish(options) {
        const {
            id,
            name = id,
            value = '',
            rows = 4,
            placeholder = '',
            required = false,
            fieldType,
            context = '',
            label = ''
        } = options;

        const requiredAttr = required ? 'required' : '';
        const placeholderAttr = placeholder ? `placeholder="${escapeAttr(placeholder)}"` : '';

        return `
            <div class="form-group ai-polish-field">
                ${label ? `<label>${escapeHtml(label)}</label>` : ''}
                <div class="form-field-with-ai">
                    <textarea 
                        id="${id}" 
                        name="${name}" 
                        rows="${rows}" 
                        ${requiredAttr} 
                        ${placeholderAttr}>${escapeHtml(value)}</textarea>
                    ${generateButtonHTML(id, fieldType, context)}
                </div>
            </div>
        `;
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

    // 暴露全局对象
    window.AiPolishComponent = {
        generateButtonHTML,
        generateTextareaWithPolish,
        escapeHtml,
        escapeAttr
    };

    console.log('[AiPolishComponent] AI 润色组件模板加载成功');
})();
