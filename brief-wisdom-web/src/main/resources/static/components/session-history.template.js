/**
 * 会话历史组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <!-- 三栏布局 -->
            <div class="panels">
                <!-- 左栏：用户列表 -->
                <div class="panel panel-users">
                    <div class="panel-header">
                        <h2>用户列表</h2>
                        <span id="sh-user-count" class="count-badge">0</span>
                    </div>
                    <div id="sh-user-list" class="panel-body">
                        <div class="empty-hint">加载中...</div>
                    </div>
                </div>

                <!-- 中栏：会话列表 -->
                <div class="panel panel-sessions">
                    <div class="panel-header">
                        <h2 id="sh-session-title">会话列表</h2>
                        <span id="sh-session-count" class="count-badge">0</span>
                    </div>
                    <div id="sh-session-list" class="panel-body">
                        <div class="empty-hint">请选择用户查看会话</div>
                    </div>
                </div>

                <!-- 右栏：消息详情 -->
                <div class="panel panel-messages">
                    <div class="panel-header">
                        <h2 id="sh-message-title">消息详情</h2>
                        <span id="sh-message-count" class="count-badge">0</span>
                    </div>
                    <div id="sh-message-list" class="panel-body">
                        <div class="empty-hint">请选择会话查看消息</div>
                    </div>
                </div>
            </div>

            <!-- 消息详情弹窗 -->
            <div id="sh-detail-modal" class="modal" style="display:none;">
                <div class="modal-content detail-modal-content">
                    <div class="modal-header">
                        <h3 id="sh-detail-modal-title">消息详情</h3>
                        <button class="modal-close" onclick="SessionHistory.closeDetailModal()">×</button>
                    </div>
                    <div id="sh-detail-modal-body" class="modal-form">
                        <!-- 动态内容 -->
                    </div>
                </div>
            </div>
        `;
    }

    function render(container) {
        const target = typeof container === 'string' 
            ? document.querySelector(container) 
            : container;
        
        if (!target) {
            console.error('[SessionHistory.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[SessionHistory.Template] 模板已渲染到:', target);
    }

    window.SessionHistoryTemplate = {
        generateHTML,
        render
    };

    console.log('[SessionHistory.Template] 模板生成器已注册');
})();
