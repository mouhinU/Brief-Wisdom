/**
 * AI 智能助手组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    /**
     * 生成 AI 助手的完整 HTML 结构
     * @returns {string} HTML 字符串
     */
    function generateHTML() {
        return `
            <!-- AI 悬浮按钮 -->
            <div class="ai-fab" id="aiFab">
                <span class="ai-fab-icon">🤖</span>
            </div>

            <!-- AI 聊天弹窗 -->
            <div class="chat-popup" id="chatPopup">
                <!-- 左侧：会话历史 -->
                <div class="session-sidebar">
                    <div class="session-header">
                        <h2>💬 会话历史</h2>
                    </div>
                    <div class="session-list" id="sessionList"></div>
                    <button class="new-session-btn" onclick="createNewSession()">+ 新建会话</button>
                </div>

                <!-- 右侧：聊天主区域 -->
                <div class="chat-main">
                    <div class="chat-header">
                        <h1>🤖 AI 智能助手</h1>
                        <p>有任何问题都可以问我哦~</p>
                        <button class="close-button" onclick="toggleChat()">×</button>
                    </div>
                    
                    <div class="chat-messages" id="chatMessages">
                        <div class="welcome-message">
                            <p>请输入您的问题,我会尽力为您解答</p>
                        </div>
                    </div>
                    
                    <div class="typing-indicator" id="typingIndicator">
                        <span></span><span></span><span></span>
                    </div>
                    
                    <div class="chat-input-container">
                        <!-- 快捷提问按钮 -->
                        <div class="quick-actions">
                            <button class="quick-action-btn" onclick="quickAsk('帮我写一段代码')">💻 写代码</button>
                            <button class="quick-action-btn" onclick="quickAsk('解释这段代码')">📖 解释代码</button>
                            <button class="quick-action-btn" onclick="quickAsk('优化建议')">⚡ 优化</button>
                            <button class="quick-action-btn" onclick="quickAsk('生成文档')">📝 文档</button>
                        </div>
                        
                        <div class="chat-input-wrapper">
                            <select id="modelSelector" class="model-selector-select" onchange="onModelChange()">
                                <option value="">加载中...</option>
                            </select>
                            <input type="text" class="chat-input" id="chatInput"
                                   placeholder="输入您的问题... (Enter 发送, Ctrl+Enter 换行)" autocomplete="off">
                            <button class="send-button" id="sendButton" onclick="sendMessage()">发送</button>
                        </div>
                        <div class="input-hint">按 Enter 发送消息 | Ctrl+Enter 换行</div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 渲染 AI 助手 HTML 到页面
     * @param {HTMLElement} container - 可选容器元素（如果不提供则直接添加到 body）
     */
    function render(container) {
        const html = generateHTML();
        
        if (container) {
            container.innerHTML = html;
            console.log('[AiAssistantTemplate] HTML 渲染完成到容器');
        } else {
            // 直接添加到 body（保持与原有逻辑一致）
            document.body.insertAdjacentHTML('beforeend', html);
            console.log('[AiAssistantTemplate] HTML 渲染完成到 body');
        }
    }

    // 暴露到全局
    window.AiAssistantTemplate = {
        generateHTML: generateHTML,
        render: render
    };

    console.log('[AiAssistantTemplate] 模板加载成功');
})();
