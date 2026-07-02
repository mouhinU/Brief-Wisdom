/**
 * 会话历史管理组件
 * 用于系统设置页面中的会话历史功能
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
(function() {
    'use strict';

    const API_BASE = '/api/ai/manage';

    // 状态管理
    const state = {
        selectedUserId: null,
        selectedSessionId: null,
        currentMessages: []
    };

    /**
     * 初始化组件
     */
    function init(options = {}) {
        const containerId = options.containerId || 'sessions-tab-content';
        console.log('[SessionHistory] 会话历史组件初始化开始，容器:', containerId);

        // 如果提供了容器且模板生成器可用，先渲染 HTML 模板
        const container = document.getElementById(containerId);
        if (container && window.SessionHistoryTemplate) {
            console.log('[SessionHistory] 使用模板生成器渲染 HTML');
            SessionHistoryTemplate.render(container);
        }

        loadUsers();
    }

    /**
     * 加载用户列表
     */
    async function loadUsers() {
        console.log('[SessionHistory] 开始加载用户列表');
        try {
            const res = await fetch(`${API_BASE}/users`);
            const result = await res.json();
            console.log('[SessionHistory] 用户列表返回:', JSON.stringify(result, null, 2));

            // 兼容两种返回格式
            let users;
            if (result.success !== undefined && result.data) {
                users = result.data;
                console.log('[SessionHistory] 检测到 Result 包装格式');
            } else {
                users = result;
                console.log('[SessionHistory] 检测到直接返回数组格式');
            }

            console.log('[SessionHistory] 用户数量:', users ? users.length : 0);
            renderUsers(users || []);
        } catch (err) {
            console.error('[SessionHistory] 加载用户异常:', err);
            showToast('加载用户异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染用户列表
     */
    function renderUsers(users) {
        const container = document.getElementById('user-list');
        if (!container) {
            console.error('[SessionHistory] 找不到 user-list 容器');
            return;
        }

        const countEl = document.getElementById('user-count');
        if (countEl) countEl.textContent = users.length;

        if (!users || users.length === 0) {
            container.innerHTML = '<div class="empty-hint">暂无用户数据</div>';
            return;
        }

        container.innerHTML = users.map(u => `
            <div class="user-item ${state.selectedUserId === u.userId ? 'active' : ''}"
                 onclick="SessionHistory.selectUser('${u.userId}', '${escapeAttr(u.nickname || u.username)}', this)">
                <div class="user-avatar">${getAvatarEmoji(u.userLevel)}</div>
                <div class="user-info">
                    <div class="user-name">${manageEscapeHtml(u.nickname || u.username)}</div>
                    <div class="user-meta">
                        <span class="level-badge level-${u.userLevel}">${levelLabel(u.userLevel)}</span>
                        <span>${u.sessionCount} 个会话</span>
                    </div>
                </div>
            </div>
        `).join('');
    }

    /**
     * 选择用户并加载其会话
     */
    async function selectUser(userId, userName, el) {
        console.log('[SessionHistory] 选择用户:', userId, userName);
        state.selectedUserId = userId;
        state.selectedSessionId = null;
        clearMessages();

        // 更新用户列表高亮
        document.querySelectorAll('.user-item').forEach(e => e.classList.remove('active'));
        if (el) el.classList.add('active');

        const titleEl = document.getElementById('session-title');
        if (titleEl) titleEl.textContent = `${userName} 的会话`;

        try {
            const url = `${API_BASE}/sessions/user/${userId}`;
            const res = await fetch(url);
            const result = await res.json();

            // 兼容两种返回格式
            let sessions;
            if (result.success !== undefined && result.data) {
                sessions = result.data;
            } else {
                sessions = result;
            }

            console.log('[SessionHistory] 会话数量:', sessions ? sessions.length : 0);
            renderSessions(sessions || []);
        } catch (err) {
            console.error('[SessionHistory] 加载会话异常:', err);
            showToast('加载会话异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染会话列表
     */
    function renderSessions(sessions) {
        const container = document.getElementById('session-list');
        if (!container) {
            console.error('[SessionHistory] 找不到 session-list 容器');
            return;
        }

        const countEl = document.getElementById('session-count');
        if (countEl) countEl.textContent = sessions.length;

        if (!sessions || sessions.length === 0) {
            container.innerHTML = '<div class="empty-hint">暂无会话数据</div>';
            return;
        }

        container.innerHTML = sessions.map(s => `
            <div class="session-item" onclick="SessionHistory.selectSession('${s.sessionId}', '${escapeAttr(s.title)}', this)">
                <div class="session-title">${manageEscapeHtml(s.title)}</div>
                <div class="session-meta">
                    <span>${s.messageCount || 0} 条消息</span>
                    <span>${manageFormatTime(s.updateTime)}</span>
                </div>
            </div>
        `).join('');
    }

    /**
     * 选择会话并加载消息
     */
    async function selectSession(sessionId, title, el) {
        console.log('[SessionHistory] 选择会话:', sessionId, title);
        state.selectedSessionId = sessionId;

        // 更新会话列表高亮
        document.querySelectorAll('.session-item').forEach(e => e.classList.remove('active'));
        if (el) el.classList.add('active');

        const titleEl = document.getElementById('message-title');
        if (titleEl) titleEl.textContent = title || '消息详情';

        try {
            const res = await fetch(`${API_BASE}/session/${sessionId}/messages`);
            const result = await res.json();

            // 兼容两种返回格式
            let messages;
            if (result.success !== undefined && result.data) {
                messages = result.data;
            } else {
                messages = result;
            }

            console.log('[SessionHistory] 消息数量:', messages ? messages.length : 0);
            renderMessages(messages || []);
        } catch (err) {
            console.error('[SessionHistory] 加载消息异常:', err);
            showToast('加载消息异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染消息列表
     */
    function renderMessages(messages) {
        state.currentMessages = messages || [];
        const container = document.getElementById('message-list');
        if (!container) {
            console.error('[SessionHistory] 找不到 message-list 容器');
            return;
        }

        const countEl = document.getElementById('message-count');
        if (countEl) countEl.textContent = state.currentMessages.length;

        if (!state.currentMessages.length) {
            container.innerHTML = '<div class="empty-hint">暂无消息</div>';
            return;
        }

        container.innerHTML = state.currentMessages.map((m, i) => `
            <div class="message-item message-clickable" onclick="SessionHistory.showMessageDetail(${i})">
                <div>
                    <span class="message-role role-${m.role}">${m.role === 'user' ? '用户' : 'AI'}</span>
                </div>
                <div class="message-content">${manageEscapeHtml(m.content)}</div>
                <div class="message-time">${manageFormatTime(m.timestamp)}</div>
                ${m.model ? `<div class="message-model">模型: ${manageEscapeHtml(m.model)}</div>` : ''}
            </div>
        `).join('');
    }

    /**
     * 显示消息详情
     */
    function showMessageDetail(index) {
        const m = state.currentMessages[index];
        if (!m) return;

        const roleLabel = m.role === 'user' ? '用户' : 'AI';
        const modalTitle = document.getElementById('detail-modal-title');
        if (modalTitle) modalTitle.textContent = `${roleLabel} 消息详情`;

        const rows = [
            { label: '角色', value: roleLabel },
            { label: '消息类型', value: m.messageType || '-' },
            { label: '模型', value: m.model || '-' },
            { label: 'Token数', value: m.tokens != null ? m.tokens : '-' },
        ];

        let html = '<div class="detail-rows">';
        rows.forEach(r => {
            html += `<div class="detail-row"><span class="detail-label">${r.label}</span><span class="detail-value">${manageEscapeHtml(String(r.value))}</span></div>`;
        });
        html += '</div>';

        // 检测内容类型并格式化渲染
        const fmt = detectAndFormatContent(m.content || '');
        html += `<div class="detail-content-section">`;
        html += `<div class="detail-content-label">消息内容 <span class="detail-content-type">${fmt.type}</span></div>`;
        html += `<div class="detail-content-body ${fmt.cssClass}">${fmt.html}</div>`;
        html += '</div>';

        const modalBody = document.getElementById('detail-modal-body');
        if (modalBody) modalBody.innerHTML = html;

        const modal = document.getElementById('detail-modal');
        if (modal) modal.style.display = 'flex';
    }

    /**
     * 清空消息
     */
    function clearMessages() {
        state.currentMessages = [];
        const container = document.getElementById('message-list');
        if (container) container.innerHTML = '';
        const countEl = document.getElementById('message-count');
        if (countEl) countEl.textContent = '0';
    }

    /**
     * 关闭详情弹窗
     */
    function closeDetailModal() {
        const modal = document.getElementById('detail-modal');
        if (modal) modal.style.display = 'none';
    }

    // ===== 工具函数 =====

    function escapeAttr(str) {
        return String(str).replace(/'/g, "\\'").replace(/"/g, '&quot;');
    }

    function manageEscapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function manageFormatTime(timeStr) {
        if (!timeStr) return '-';
        const date = new Date(timeStr);
        if (isNaN(date.getTime())) return timeStr;
        const now = new Date();
        const diff = now - date;
        if (diff < 60000) return '刚刚';
        if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
        if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
        if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';
        return date.toLocaleDateString('zh-CN');
    }

    function getAvatarEmoji(level) {
        const emojis = { admin: '👨‍💼', vip: '⭐', normal: '👤' };
        return emojis[level] || '👤';
    }

    function levelLabel(level) {
        const labels = { admin: '管理员', vip: '会员', normal: '普通用户' };
        return labels[level] || level;
    }

    function showToast(message, type = 'info') {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type);
        } else {
            console.log(`[${type.toUpperCase()}] ${message}`);
        }
    }

    function detectAndFormatContent(content) {
        // 简化版本，实际项目中可能需要更复杂的检测逻辑
        return {
            type: '文本',
            cssClass: '',
            html: manageEscapeHtml(content)
        };
    }

    // 暴露全局接口
    window.SessionHistory = {
        init,
        selectUser,
        selectSession,
        showMessageDetail,
        closeDetailModal
    };

    // 自动注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('session-history', window.SessionHistory);
    }

    console.log('[SessionHistory] 组件已注册到 window.SessionHistory');
})();
