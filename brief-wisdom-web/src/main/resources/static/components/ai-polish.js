/**
 * AI 文本润色组件 - 业务逻辑
 * 
 * @author Brief-Wisdom
 * @date 2026-07-05
 */

(function() {
    'use strict';

    /**
     * AI 文本润色核心函数
     * @param {string} textareaId - 目标 textarea 的 DOM id
     * @param {string} fieldType - 字段类型：description/background/duty/achievement
     * @param {string} context - 上下文信息（如公司名、项目名）
     */
    async function polish(textareaId, fieldType, context = '') {
        const textarea = document.getElementById(textareaId);
        if (!textarea) {
            console.error('[AiPolish] 找不到 textarea:', textareaId);
            return;
        }

        const originalText = textarea.value.trim();
        if (!originalText) {
            showToast('请先输入需要润色的文本', 'warning');
            return;
        }

        // 显示加载状态
        const wrapper = textarea.parentElement.querySelector('.ai-polish-wrapper');
        const btn = wrapper ? wrapper.querySelector('.ai-polish-btn') : null;
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

            const responseJson = await response.json();
            
            // 处理 Result 包装格式: { success, code, msg, data }
            let polishedText;
            let errorMsg;
            
            if (responseJson.data && typeof responseJson.data === 'object') {
                // Result 包装格式
                polishedText = responseJson.data.result;
                errorMsg = responseJson.data.error;
            } else if (responseJson.result) {
                // 直接格式（兼容旧版本）
                polishedText = responseJson.result;
                errorMsg = responseJson.error;
            } else {
                throw new Error('响应数据格式异常');
            }
            
            if (errorMsg) {
                showToast(errorMsg, 'error');
            } else if (polishedText) {
                // 显示对比弹窗
                showComparisonModal(originalText, polishedText, textarea);
            } else {
                showToast('AI 润色结果为空', 'warning');
            }
        } catch (error) {
            console.error('[AiPolish] AI润色失败:', error);
            showToast('AI 润色失败: ' + error.message, 'error');
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
     * 显示 AI 润色对比弹窗
     * @param {string} originalText - 原始文本
     * @param {string} polishedText - 润色后的文本
     * @param {HTMLElement} targetTextarea - 目标 textarea 元素
     */
    function showComparisonModal(originalText, polishedText, targetTextarea) {
        // 检查是否已有弹窗，避免重复创建
        const existingOverlay = document.querySelector('.ai-polish-modal-overlay');
        if (existingOverlay) {
            existingOverlay.remove();
        }

        // 创建遮罩层
        const overlay = document.createElement('div');
        overlay.className = 'ai-polish-modal-overlay';
        
        // 创建弹窗
        const modal = document.createElement('div');
        modal.className = 'ai-polish-modal';
        
        // 渲染内容：优先使用 Markdown，降级为纯文本
        const renderContent = (text) => {
            if (typeof marked !== 'undefined' && marked.parse) {
                try {
                    return marked.parse(text);
                } catch (e) {
                    console.warn('[AiPolish] Markdown 渲染失败，降级为纯文本:', e);
                }
            }
            return escapeHtml(text);
        };

        const originalRendered = renderContent(originalText);
        const polishedRendered = renderContent(polishedText);

        modal.innerHTML = `
            <div class="ai-polish-modal-header">
                <div class="ai-polish-modal-title">
                    <span>✨</span>
                    <span>AI 润色结果对比</span>
                </div>
                <button class="ai-polish-modal-close" title="关闭">×</button>
            </div>
            <div class="ai-polish-modal-body">
                <div class="ai-polish-comparison">
                    <div class="ai-polish-section">
                        <div class="ai-polish-section-label">
                            <span class="label-icon">📝</span>
                            <span>原文内容</span>
                        </div>
                        <div class="ai-polish-text original">${originalRendered}</div>
                    </div>
                    <div class="ai-polish-section">
                        <div class="ai-polish-section-label">
                            <span class="label-icon">✨</span>
                            <span>AI 润色后</span>
                        </div>
                        <div class="ai-polish-text polished">${polishedRendered}</div>
                    </div>
                </div>
            </div>
            <div class="ai-polish-footer">
                <button class="ai-polish-btn-action reject">
                    <span>✕</span>
                    <span>保留原文</span>
                </button>
                <button class="ai-polish-btn-action accept">
                    <span>✓</span>
                    <span>采纳润色</span>
                </button>
            </div>
        `;
        
        overlay.appendChild(modal);
        document.body.appendChild(overlay);
        
        // 绑定按钮事件
        const closeBtn = modal.querySelector('.ai-polish-modal-close');
        const acceptBtn = modal.querySelector('.ai-polish-btn-action.accept');
        const rejectBtn = modal.querySelector('.ai-polish-btn-action.reject');
        
        // 关闭按钮
        closeBtn.addEventListener('click', () => {
            overlay.remove();
        });
        
        // 采纳按钮
        acceptBtn.addEventListener('click', () => {
            targetTextarea.value = polishedText;
            overlay.remove();
            showToast('已采纳 AI 润色结果', 'success');
            
            // 触发自定义事件，方便其他组件监听
            triggerEvent('aiPolishAccepted', {
                textareaId: targetTextarea.id,
                originalText: originalText,
                polishedText: polishedText
            });
        });
        
        // 拒绝按钮
        rejectBtn.addEventListener('click', () => {
            overlay.remove();
            showToast('已保留原文', 'info');
            
            // 触发自定义事件
            triggerEvent('aiPolishRejected', {
                textareaId: targetTextarea.id,
                originalText: originalText
            });
        });
        
        // 点击遮罩层关闭
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                overlay.remove();
            }
        });
        
        // ESC 键关闭
        const escHandler = (e) => {
            if (e.key === 'Escape') {
                overlay.remove();
                document.removeEventListener('keydown', escHandler);
            }
        };
        document.addEventListener('keydown', escHandler);
    }

    /**
     * 显示轻量级提示消息
     * @param {string} message - 提示消息
     * @param {string} type - 类型：success/info/warning/error
     */
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        const colors = {
            success: { bg: '#10b981', icon: '✓' },
            info: { bg: '#3b82f6', icon: 'ℹ' },
            warning: { bg: '#f59e0b', icon: '⚠' },
            error: { bg: '#ef4444', icon: '✕' }
        };
        const color = colors[type] || colors.info;
        
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 12px 20px;
            background: ${color.bg};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            font-size: 14px;
            font-weight: 500;
            z-index: 10001;
            animation: slideInRight 0.3s ease;
            display: flex;
            align-items: center;
            gap: 8px;
        `;
        
        toast.innerHTML = `<span>${color.icon}</span><span>${message}</span>`;
        document.body.appendChild(toast);
        
        // 3秒后自动消失
        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
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
     * 触发自定义事件
     * @param {string} eventName - 事件名称
     * @param {Object} detail - 事件详情
     */
    function triggerEvent(eventName, detail) {
        const event = new CustomEvent(eventName, {
            bubbles: true,
            detail: detail
        });
        document.dispatchEvent(event);
    }

    /**
     * 批量初始化页面上的 AI 润色按钮
     * 自动查找所有带有 .ai-polish-wrapper 的元素并绑定事件
     */
    function initPage() {
        console.log('[AiPolish] 初始化页面 AI 润色功能');
        
        const wrappers = document.querySelectorAll('.ai-polish-wrapper');
        wrappers.forEach(wrapper => {
            const textareaId = wrapper.dataset.textareaId;
            const fieldType = wrapper.dataset.fieldType;
            const context = wrapper.dataset.context || '';
            
            if (textareaId && fieldType) {
                const btn = wrapper.querySelector('.ai-polish-btn');
                if (btn) {
                    btn.onclick = () => polish(textareaId, fieldType, context);
                }
            }
        });
        
        console.log(`[AiPolish] 已初始化 ${wrappers.length} 个 AI 润色按钮`);
    }

    // 暴露全局对象
    window.AiPolishComponent = {
        polish,
        showComparisonModal,
        showToast,
        initPage,
        escapeHtml
    };

    // 页面加载完成后自动初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPage);
    } else {
        initPage();
    }

    console.log('[AiPolish] AI 润色组件加载成功');
})();
