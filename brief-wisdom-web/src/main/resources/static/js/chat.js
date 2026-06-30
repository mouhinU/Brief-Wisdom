// DOM 元素引用（动态注入时可能延迟获取）
function getEl(id) { return document.getElementById(id); }

// 当前页面上下文（用于 AI 助手识别当前所在页面）
function getCurrentPageContext() {
    return window.location.pathname || '/';
}

// 页面上下文 -> 显示名称
function getPageContextLabel(pageContext) {
    const labels = {
        '/': '首页',
        '/about.html': '个人简历',
        '/resume-manage.html': '简历维护',
        '/system-settings.html': '系统设置',
        '/ai-manage.html': 'AI管理'
    };
    return labels[pageContext] || '';
}

// 页面上下文 -> 图标
function getPageContextIcon(pageContext) {
    const icons = {
        '/': '🏠',
        '/about.html': '👤',
        '/resume-manage.html': '📝',
        '/system-settings.html': '⚙️',
        '/ai-manage.html': '🤖'
    };
    return icons[pageContext] || '💬';
}

// 当前会话 ID
let currentSessionId = null;

// 当前选中的模型
let currentModel = null;

// 可用模型列表
let availableModels = [];

// 分页状态
let sessionCurrentPage = 1;
let sessionHasMore = false;
let sessionIsLoading = false;
let allSessions = [];  // 累积存储所有已加载的会话

// 消息历史分页状态
let historyCurrentPage = 1;
let historyHasMore = false;
let historyIsLoading = false;

// 分页配置（从后端动态获取）
let paginationConfig = {
    sessionList: { defaultSize: 20, maxSize: 100 },
    messageHistory: { defaultSize: 20, maxSize: 200 }
};

// 多端同步状态（支持 SSE / WebSocket 双模式）
let syncTransport = null;          // 'sse' 或 'websocket'，首次连接时从后端获取
let syncEventSource = null;        // SSE 模式下的 EventSource 实例
let syncWebSocket = null;          // WebSocket 模式下的 WebSocket 实例
let syncReconnectTimer = null;
let syncPingTimer = null;          // WebSocket 心跳定时器
const SYNC_RECONNECT_DELAY = 3000; // 断线后 3 秒重连
const SYNC_PING_INTERVAL = 30000;  // WebSocket 心跳间隔 30 秒

// 加载分页配置（页面初始化时调用一次）
async function loadPaginationConfig() {
    try {
        const response = await fetch('/api/ai/config/pagination');
        const data = await response.json();
        if (data.success) {
            paginationConfig = data.data;
            console.log('分页配置已加载:', paginationConfig);
        }
    } catch (error) {
        console.warn('加载分页配置失败，使用默认值:', error);
    }
}

// 加载可用模型列表
async function loadModels() {
    try {
        const response = await fetch('/api/ai/models/enabled');
        const data = await response.json();
        if (data.success && data.data) {
            availableModels = data.data;
            renderModelSelector();
        }
    } catch (error) {
        console.warn('加载模型列表失败:', error);
    }
}

// 渲染模型选择器
function renderModelSelector() {
    const selector = document.getElementById('modelSelector');
    if (!selector) return;
    
    selector.innerHTML = availableModels.map(m => 
        `<option value="${m.modelName}" ${m.isActive === 1 ? 'selected' : ''}>${m.displayName}</option>`
    ).join('');
    
    // 设置当前模型为激活的模型
    const activeModel = availableModels.find(m => m.isActive === 1);
    if (activeModel) {
        currentModel = activeModel.modelName;
        selector.value = currentModel;
    }
}

// 模型切换事件
function onModelChange() {
    const selector = document.getElementById('modelSelector');
    if (selector) {
        currentModel = selector.value;
        console.log('切换模型:', currentModel);
    }
}

// 配置 marked.js（如果已加载）
if (typeof marked !== 'undefined') {
    marked.setOptions({
        breaks: true,
        gfm: true,
        headerIds: false,
        mangle: false
    });
}

// 显示会话列表加载中提示
function showSessionListLoading() {
    const list = getEl('sessionList');
    if (!list) return;
    list.innerHTML = `
        <div class="session-loading-indicator">
            <div class="chat-loading-spinner"></div>
            <span>加载中...</span>
        </div>
    `;
}

// 切换聊天窗口显示/隐藏
async function toggleChat() {
    const chatPopup = getEl('chatPopup');
    if (!chatPopup) return;
    chatPopup.classList.toggle('show');
    if (chatPopup.classList.contains('show')) {
        // 仅在首次加载时显示 loading 提示
        if (!chatDataLoaded) {
            showSessionListLoading();
            showChatLoading();
        }
        // 首次打开时懒加载数据（模型列表、会话列表等）
        await ensureChatDataLoaded();
        // 初始化聊天：加载会话列表，若无会话则自动创建
        await ensureChatInitialized();
        connectSync();
        setTimeout(() => {
            const input = getEl('chatInput');
            if (input) input.focus();
        }, 300);
    } else {
        disconnectSync();
    }
}

// ========== 弹窗拖动功能 ==========
let isDragging = false;
let dragStartX = 0;
let dragStartY = 0;
let popupStartLeft = 0;
let popupStartTop = 0;

function initPopupDrag() {
    const chatPopup = getEl('chatPopup');
    if (!chatPopup) return;
    const header = chatPopup.querySelector('.chat-header');
    if (!header) return;

    header.addEventListener('mousedown', function(e) {
        // 排除关闭按钮等交互元素
        if (e.target.closest('.close-button')) return;

        isDragging = true;
        dragStartX = e.clientX;
        dragStartY = e.clientY;

        // 将 right/bottom 定位转换为 left/top 定位
        const rect = chatPopup.getBoundingClientRect();
        popupStartLeft = rect.left;
        popupStartTop = rect.top;

        chatPopup.style.left = popupStartLeft + 'px';
        chatPopup.style.top = popupStartTop + 'px';
        chatPopup.style.right = 'auto';
        chatPopup.style.bottom = 'auto';

        document.body.style.userSelect = 'none';
        e.preventDefault();
    });

    document.addEventListener('mousemove', function(e) {
        if (!isDragging) return;
        const chatPopup = getEl('chatPopup');
        if (!chatPopup) return;

        const dx = e.clientX - dragStartX;
        const dy = e.clientY - dragStartY;

        let newLeft = popupStartLeft + dx;
        let newTop = popupStartTop + dy;

        // 边界约束：不超出视口
        const rect = chatPopup.getBoundingClientRect();
        const maxLeft = window.innerWidth - rect.width;
        const maxTop = window.innerHeight - rect.height;

        newLeft = Math.max(0, Math.min(newLeft, maxLeft));
        newTop = Math.max(0, Math.min(newTop, maxTop));

        chatPopup.style.left = newLeft + 'px';
        chatPopup.style.top = newTop + 'px';
    });

    document.addEventListener('mouseup', function() {
        if (!isDragging) return;
        isDragging = false;
        document.body.style.userSelect = '';
    });
}

// 确保聊天已初始化（有可用会话）
async function ensureChatInitialized() {
    // 如果还没加载过会话列表，先加载（不自动选中会话，显示欢迎界面）
    if (allSessions.length === 0 && !sessionIsLoading) {
        await loadSessions(false);
    }
    // 显示欢迎界面（不加载历史消息）
    clearChatMessages();
    // 不自动创建会话，等待用户发送消息时再创建
}

// 加载会话列表（重置为第一页）
// autoSelect: 是否自动选中第一个会话并加载历史（用户主动点击"新对话"时为 true）
async function loadSessions(autoSelect = false) {
    sessionCurrentPage = 1;
    allSessions = [];
    sessionHasMore = false;

    try {
        const pageSize = paginationConfig.sessionList.defaultSize;
        const url = `/api/ai/sessions?page=1&size=${pageSize}`;
        const response = await fetch(url);
        const data = await response.json();
        
        if (data.success) {
            const pageData = data.data;
            allSessions = pageData.records || [];
            sessionHasMore = pageData.hasMore;
            sessionCurrentPage = 1;

            renderSessionList(allSessions);
            updateLoadMoreIndicator();
            
            // 仅在明确需要时自动选中第一个会话并加载历史
            if (autoSelect && allSessions.length > 0 && !currentSessionId) {
                currentSessionId = allSessions[0].sessionId;
                await loadSessionHistory(currentSessionId);
                // 重新渲染以更新高亮
                renderSessionList(allSessions);
            }
        }
    } catch (error) {
        console.error('加载会话列表失败:', error);
    }
}

// 加载更多会话（下一页）
async function loadMoreSessions() {
    if (sessionIsLoading || !sessionHasMore) {
        return;
    }

    sessionIsLoading = true;
    showLoadMoreIndicator();

    try {
        const nextPage = sessionCurrentPage + 1;
        const pageSize = paginationConfig.sessionList.defaultSize;
        const url = `/api/ai/sessions?page=${nextPage}&size=${pageSize}`;
        const response = await fetch(url);
        const data = await response.json();

        if (data.success) {
            const pageData = data.data;
            const newRecords = pageData.records || [];

            // 追加到全量列表
            allSessions = allSessions.concat(newRecords);
            sessionCurrentPage = nextPage;
            sessionHasMore = pageData.hasMore;

            // 追加渲染新会话项
            appendSessionItems(newRecords);
            updateLoadMoreIndicator();
        }
    } catch (error) {
        console.error('加载更多会话失败:', error);
    } finally {
        sessionIsLoading = false;
    }
}

// 显示/隐藏加载更多指示器
function showLoadMoreIndicator() {
    const list = getEl('sessionList');
    if (!list) return;
    let indicator = document.getElementById('loadMoreIndicator');
    if (!indicator) {
        indicator = document.createElement('div');
        indicator.id = 'loadMoreIndicator';
        indicator.className = 'load-more-indicator';
        indicator.innerHTML = '<span>加载中...</span>';
        list.appendChild(indicator);
    }
    indicator.style.display = 'flex';
}

function updateLoadMoreIndicator() {
    let indicator = document.getElementById('loadMoreIndicator');
    if (indicator) {
        indicator.remove();
    }
    const list = getEl('sessionList');
    if (!list) return;
    if (sessionHasMore) {
        indicator = document.createElement('div');
        indicator.id = 'loadMoreIndicator';
        indicator.className = 'load-more-indicator';
        indicator.innerHTML = '<span>↓ 下拉加载更多</span>';
        list.appendChild(indicator);
    }
}

// 渲染会话列表（全量替换）
function renderSessionList(sessions) {
    const list = getEl('sessionList');
    if (!list) return;
    list.innerHTML = '';
    if (!sessions || sessions.length === 0) {
        list.innerHTML = '<div class="session-empty-tip">暂无会话，发送消息即可开始</div>';
        return;
    }
    appendSessionItems(sessions);
}

// 追加会话项（增量添加）
function appendSessionItems(sessions) {
    const list = getEl('sessionList');
    if (!list) return;
    // 移除加载更多指示器（稍后重新添加）
    const existingIndicator = document.getElementById('loadMoreIndicator');
    if (existingIndicator) {
        existingIndicator.remove();
    }

    sessions.forEach(session => {
        const sessionItem = document.createElement('div');
        sessionItem.className = 'session-item';
        if (session.sessionId === currentSessionId) {
            sessionItem.classList.add('active');
        }
        
        // 使用会话的更新时间（即最后一条消息的时间）
        const timeStr = formatTime(session.updateTime);

        // 页面上下文图标
        const pageLabel = getPageContextLabel(session.pageContext);
        const pageLabelHtml = pageLabel ? `<span class="session-page-label" title="${escapeHtml(pageLabel)}">${getPageContextIcon(session.pageContext)}</span>` : '';
        
        sessionItem.innerHTML = `
            <div class="session-title-row">
                ${pageLabelHtml}
                <div class="session-title" title="双击编辑标题">${escapeHtml(session.title)}</div>
            </div>
            <div class="session-time">${timeStr}</div>
            <button class="delete-session-btn" onclick="deleteSession(event, '${session.sessionId}')">×</button>
        `;
        
        // 双击标题进入编辑模式
        const titleEl = sessionItem.querySelector('.session-title');
        titleEl.addEventListener('dblclick', (e) => {
            e.stopPropagation();
            startEditSessionTitle(titleEl, session.sessionId, session.title);
        });
        
        sessionItem.onclick = (e) => {
            if (!e.target.classList.contains('delete-session-btn') && !e.target.classList.contains('session-title-input')) {
                selectSession(session.sessionId);
            }
        };
        
        list.appendChild(sessionItem);
    });
}

// 创建新会话（返回 sessionId 或 null）
async function createNewSession() {
    try {
        const response = await fetch('/api/ai/session', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                pageContext: getCurrentPageContext()
            })
        });
        const data = await response.json();
        
        if (data.success && data.data) {
            const newSessionId = data.data;
            currentSessionId = newSessionId;
            
            // 清空聊天窗口
            clearChatMessages();
            
            // 重新加载会话列表以显示新会话
            await loadSessions();
            
            console.log('创建新会话成功:', newSessionId);
            return newSessionId;
        } else {
            console.error('创建会话返回失败:', data);
            // 显示后端返回的具体错误信息
            const errorMsg = data.msg || data.error || '创建会话失败';
            addMessage('错误：' + errorMsg, 'ai');
            return null;
        }
    } catch (error) {
        console.error('创建会话失败:', error);
        addMessage('错误：网络请求失败，请检查网络连接', 'ai');
        return null;
    }
}

// 选择会话
async function selectSession(sessionId) {
    console.log('切换会话:', sessionId);
    currentSessionId = sessionId;

    // 重置发送历史浏览状态
    historyIndex = -1;
    pendingInput = '';
    
    // 先清空当前聊天内容
    clearChatMessages();
    
    // 加载历史消息
    await loadSessionHistory(sessionId);
    
    // 更新会话列表UI的高亮状态
    document.querySelectorAll('.session-item').forEach(item => {
        item.classList.remove('active');
    });
    
    // 找到对应的会话项并添加高亮
    const list = getEl('sessionList');
    if (!list) return;
    const sessionItems = list.querySelectorAll('.session-item');
    sessionItems.forEach(item => {
        const deleteBtn = item.querySelector('.delete-session-btn');
        if (deleteBtn) {
            const sessionIdAttr = deleteBtn.getAttribute('onclick');
            if (sessionIdAttr && sessionIdAttr.includes(sessionId)) {
                item.classList.add('active');
            }
        }
    });
    
    console.log('会话切换完成');
}

// 删除会话
async function deleteSession(event, sessionId) {
    event.stopPropagation();
    
    const confirmed = await showConfirmDialog('确定要删除这个会话吗？', '🗑️');
    if (!confirmed) {
        return;
    }
    
    try {
        const response = await fetch(`/api/ai/session/${sessionId}`, {
            method: 'DELETE'
        });
        const data = await response.json();
        
        if (data.success) {
            if (currentSessionId === sessionId) {
                currentSessionId = null;
                clearChatMessages();
            }
            await loadSessions();
        }
    } catch (error) {
        console.error('删除会话失败:', error);
    }
}

// 开始编辑会话标题（双击进入编辑模式）
function startEditSessionTitle(titleEl, sessionId, currentTitle) {
    // 如果已经在编辑中，忽略
    if (titleEl.querySelector('.session-title-input')) {
        return;
    }
    
    const originalTitle = currentTitle;
    
    // 创建输入框
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'session-title-input';
    input.value = currentTitle;
    
    // 保存原始标题
    titleEl.dataset.originalTitle = originalTitle;
    titleEl.textContent = '';
    titleEl.appendChild(input);
    
    // 聚焦并选中文本
    input.focus();
    input.select();
    
    // 保存标题
    const saveTitle = async () => {
        const newTitle = input.value.trim();
        
        // 如果标题没变，恢复原状
        if (newTitle === originalTitle || newTitle === '') {
            titleEl.textContent = originalTitle;
            return;
        }
        
        try {
            const response = await fetch(`/api/ai/session/${sessionId}/title`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: newTitle })
            });
            const data = await response.json();
            
            if (data.success) {
                titleEl.textContent = newTitle;
                console.log('会话标题已更新:', newTitle);
            } else {
                titleEl.textContent = originalTitle;
                console.error('更新标题失败:', data);
            }
        } catch (error) {
            titleEl.textContent = originalTitle;
            console.error('更新标题失败:', error);
        }
    };
    
    // 回车保存
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            saveTitle();
        } else if (e.key === 'Escape') {
            // Esc 取消编辑
            titleEl.textContent = originalTitle;
        }
    });
    
    // 失焦保存
    input.addEventListener('blur', () => {
        saveTitle();
    });
}

// 显示聊天区域加载中提示
function showChatLoading() {
    const messages = getEl('chatMessages');
    if (!messages) return;
    messages.innerHTML = `
        <div class="chat-loading-indicator">
            <div class="chat-loading-spinner"></div>
            <span>加载中...</span>
        </div>
    `;
}

// 加载会话历史（分页，初始加载最新一页）
async function loadSessionHistory(sessionId) {
    // 重置消息历史分页状态
    historyCurrentPage = 1;
    historyHasMore = false;
    historyIsLoading = false;

    // 显示加载中提示
    showChatLoading();

    try {
        console.log('正在加载会话历史:', sessionId);
        const pageSize = paginationConfig.messageHistory.defaultSize;
        const response = await fetch(`/api/ai/session/${sessionId}/history?page=1&size=${pageSize}`);
        const data = await response.json();

        console.log('历史记录响应:', data);

        // 清空当前消息
        clearChatMessages();

        if (data.success && data.data.records && data.data.records.length > 0) {
            const records = data.data.records;
            historyCurrentPage = 1;
            historyHasMore = data.data.hasMore;

            console.log(`加载到 ${records.length} 条历史消息，还有更多: ${historyHasMore}`);

            // 渲染消息（接口已按正序返回）
            records.forEach(msg => {
                addMessage(msg.content, msg.role === 'user' ? 'user' : 'ai', false, msg.model);
            });

            // 如果有更多历史消息，显示顶部加载提示
            if (historyHasMore) {
                showHistoryLoadMoreIndicator();
            }

            // 滚动到底部
            const messages = getEl('chatMessages');
            if (messages) {
                messages.scrollTop = messages.scrollHeight;
            }
        } else {
            console.log('没有历史消息');
        }
    } catch (error) {
        console.error('加载历史记录失败:', error);
    }
}

// 加载更多历史消息（向上滚动触发，加载更早的消息）
async function loadMoreHistory() {
    if (historyIsLoading || !historyHasMore || !currentSessionId) {
        return;
    }

    historyIsLoading = true;
    showHistoryLoadingIndicator();

    try {
        const nextPage = historyCurrentPage + 1;
        const pageSize = paginationConfig.messageHistory.defaultSize;
        const response = await fetch(`/api/ai/session/${currentSessionId}/history?page=${nextPage}&size=${pageSize}`);
        const data = await response.json();

        if (data.success && data.data.records && data.data.records.length > 0) {
            const records = data.data.records;
            historyCurrentPage = nextPage;
            historyHasMore = data.data.hasMore;

            console.log(`加载到第 ${nextPage} 页，共 ${records.length} 条更早的消息`);

            // 在顶部插入更早的消息（接口返回正序，需要倒序插入到顶部）
            prependMessages(records);

            // 更新加载提示
            if (historyHasMore) {
                showHistoryLoadMoreIndicator();
            } else {
                removeHistoryLoadIndicator();
                showHistoryFullyLoaded();
            }
        } else {
            // 没有更多数据
            historyHasMore = false;
            removeHistoryLoadIndicator();
            showHistoryFullyLoaded();
        }
    } catch (error) {
        console.error('加载更多历史消息失败:', error);
    } finally {
        historyIsLoading = false;
    }
}

// 在聊天窗口顶部插入消息（用于加载更早的历史消息）
function prependMessages(records) {
    const messages = getEl('chatMessages');
    if (!messages) return;

    // 记录当前滚动高度，用于加载后保持位置
    const oldScrollHeight = messages.scrollHeight;

    // 移除顶部的加载指示器
    removeHistoryLoadIndicator();

    // 将消息按顺序插入到顶部（欢迎消息之后）
    const welcomeMsg = messages.querySelector('.welcome-message');
    const insertBefore = welcomeMsg ? welcomeMsg.nextSibling : messages.firstChild;

    records.forEach(msg => {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${msg.role === 'user' ? 'user' : 'ai'}`;

        const messageContent = document.createElement('div');
        messageContent.className = 'message-content';

        if (msg.role === 'user') {
            messageContent.textContent = msg.content;
        } else {
            try {
                messageContent.innerHTML = marked.parse(msg.content);
            } catch (e) {
                messageContent.textContent = msg.content;
            }
            if (msg.model) {
                const modelLabel = document.createElement('div');
                modelLabel.className = 'message-model-label';
                modelLabel.textContent = msg.model;
                messageDiv.appendChild(modelLabel);
            }
        }

        messageDiv.appendChild(messageContent);
        messages.insertBefore(messageDiv, insertBefore);
    });

    // 保持用户当前的阅读位置（不跳到顶部或底部）
    const newScrollHeight = messages.scrollHeight;
    messages.scrollTop = oldScrollHeight > 0 ? (messages.scrollTop + (newScrollHeight - oldScrollHeight)) : 0;
}

// 消息历史 - 顶部"加载更多"指示器
function showHistoryLoadMoreIndicator() {
    removeHistoryLoadIndicator();
    const messages = getEl('chatMessages');
    if (!messages) return;

    const indicator = document.createElement('div');
    indicator.id = 'historyLoadMore';
    indicator.className = 'history-load-more-indicator';
    indicator.innerHTML = '<span>↑ 滚动加载更早消息</span>';
    indicator.onclick = () => loadMoreHistory();
    messages.insertBefore(indicator, messages.firstChild);
}

function showHistoryLoadingIndicator() {
    removeHistoryLoadIndicator();
    const messages = getEl('chatMessages');
    if (!messages) return;

    const indicator = document.createElement('div');
    indicator.id = 'historyLoadMore';
    indicator.className = 'history-load-more-indicator';
    indicator.innerHTML = '<span>加载中...</span>';
    messages.insertBefore(indicator, messages.firstChild);
}

function removeHistoryLoadIndicator() {
    const indicator = document.getElementById('historyLoadMore');
    if (indicator) indicator.remove();
}

function showHistoryFullyLoaded() {
    const messages = getEl('chatMessages');
    if (!messages) return;
    // 不再显示任何提示，已加载全部
}

// 清空聊天消息，显示欢迎界面
function clearChatMessages() {
    const messages = getEl('chatMessages');
    if (!messages) return;
    messages.innerHTML = `
        <div class="welcome-message">
            <h2>👋 你好，我是简知 AI 助手</h2>
            <p class="welcome-desc">我可以帮你解答问题、提供建议、分析思路。以下是我能做的事情：</p>
            <div class="welcome-features">
                <div class="welcome-feature-item">
                    <span class="feature-icon">💡</span>
                    <span>知识问答与解释</span>
                </div>
                <div class="welcome-feature-item">
                    <span class="feature-icon">📝</span>
                    <span>文案撰写与润色</span>
                </div>
                <div class="welcome-feature-item">
                    <span class="feature-icon">🔍</span>
                    <span>问题分析与思路梳理</span>
                </div>
                <div class="welcome-feature-item">
                    <span class="feature-icon">💻</span>
                    <span>编程辅助与代码解读</span>
                </div>
            </div>
            <p class="welcome-tips">💬 直接在下方输入框提问即可开始对话，我会记住上下文。</p>
        </div>
    `;
}

// 发送消息
let isSending = false; // 防重复提交标志
async function sendMessage() {
    const chatInput = getEl('chatInput');
    const sendButton = getEl('sendButton');
    if (!chatInput) return;
    const message = chatInput.value.trim();

    if (!message) {
        return;
    }

    // 防重复提交：如果正在发送中，直接返回
    if (isSending) {
        return;
    }
    isSending = true;
    
    // 确保有当前会话
    if (!currentSessionId) {
        console.log('没有当前会话，创建新会话...');
        const newId = await createNewSession();
        console.log('创建后的 currentSessionId:', newId);
        if (!newId) {
            // createNewSession() 已经显示了错误信息
            if (sendButton) sendButton.disabled = false;
            isSending = false;
            return;
        }
    }
    
    console.log('准备发送消息，sessionId:', currentSessionId, '消息:', message);

    // 清空欢迎消息
    const messages = getEl('chatMessages');
    if (messages) {
        const welcomeMessage = messages.querySelector('.welcome-message');
        if (welcomeMessage) {
            welcomeMessage.remove();
        }
    }

    // 记录到发送历史
    pushSendHistory(message);

    // 添加用户消息
    addMessage(message, 'user');
    
    // 清空输入框并禁用（防止重复提交）
    chatInput.value = '';
    chatInput.disabled = true;
    
    // 禁用发送按钮
    if (sendButton) sendButton.disabled = true;
    
    // 显示打字指示器
    showTypingIndicator();

    try {
        // 调用带上下文的 AI API
        if (!currentSessionId) {
            console.error('错误：currentSessionId 为空！');
            hideTypingIndicator();
            addMessage('错误：会话ID为空，请刷新页面重试', 'ai');
            if (sendButton) sendButton.disabled = false;
            if (chatInput) { chatInput.disabled = false; chatInput.value = message; chatInput.focus(); }
            isSending = false;
            return;
        }
        
        const url = `/api/ai/chat/session/${currentSessionId}`;
        console.log('========== 发送请求 ==========');
        console.log('currentSessionId:', currentSessionId);
        console.log('model:', currentModel);
        console.log('请求 URL:', url);
        console.log('请求消息:', message);
        
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                message: message,
                model: currentModel,
                pageContext: getCurrentPageContext()
            })
        });

        const data = await response.json();
        console.log('响应数据:', data);
        
        // 隐藏打字指示器
        hideTypingIndicator();

        if (data.success) {
            addMessage(data.data, 'ai', true, currentModel);
            // 更新会话列表（可能标题变了）
            await loadSessions();
        } else {
            addMessage('抱歉,出现了错误: ' + data.error, 'ai');
            // 失败时恢复输入框内容
            if (chatInput) { chatInput.value = message; }
        }
    } catch (error) {
        console.error('发送消息失败:', error);
        hideTypingIndicator();
        addMessage('抱歉,网络请求失败: ' + error.message, 'ai');
        // 失败时恢复输入框内容
        if (chatInput) { chatInput.value = message; }
    } finally {
        if (sendButton) sendButton.disabled = false;
        if (chatInput) { chatInput.disabled = false; chatInput.focus(); }
        isSending = false;
    }
}

// 添加消息到聊天窗口
function addMessage(text, sender, scroll = true, modelName = null) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    
    // 对 AI 消息进行 Markdown 渲染，用户消息保持纯文本
    if (sender === 'ai') {
        try {
            messageContent.innerHTML = marked.parse(text);
        } catch (error) {
            console.error('Markdown 解析错误:', error);
            messageContent.textContent = text;
        }
        // 显示模型名称
        if (modelName) {
            const modelLabel = document.createElement('div');
            modelLabel.className = 'message-model-label';
            modelLabel.textContent = modelName;
            messageDiv.appendChild(modelLabel);
        }
    } else {
        messageContent.textContent = text;
    }
    
    messageDiv.appendChild(messageContent);
    const messages = getEl('chatMessages');
    if (messages) {
        messages.appendChild(messageDiv);
        // 滚动到底部
        if (scroll) {
            messages.scrollTop = messages.scrollHeight;
        }
    }
}

// 格式化时间
function formatTime(timeInput) {
    if (!timeInput) {
        return '';
    }
    
    let date;
    
    // 处理字符串格式的时间 (ISO 8601)
    if (typeof timeInput === 'string') {
        date = new Date(timeInput);
    } 
    // 处理数组格式的时间 [year, month, day, hour, minute, second, ...]
    else if (Array.isArray(timeInput) && timeInput.length >= 5) {
        const [year, month, day, hour, minute] = timeInput;
        date = new Date(year, month - 1, day, hour, minute);
    } else {
        return '';
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
        return '';
    }
    
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) {
        return '刚刚';
    } else if (minutes < 60) {
        return `${minutes}分钟前`;
    } else if (hours < 24) {
        return `${hours}小时前`;
    } else if (days < 7) {
        return `${days}天前`;
    } else {
        const month = date.getMonth() + 1;
        const day = date.getDate();
        return `${month}/${day}`;
    }
}

// HTML 转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 显示打字指示器
function showTypingIndicator() {
    const typingIndicator = getEl('typingIndicator');
    const messages = getEl('chatMessages');
    if (!typingIndicator || !messages) return;
    typingIndicator.classList.add('show');
    messages.appendChild(typingIndicator);
    messages.scrollTop = messages.scrollHeight;
}

// 隐藏打字指示器
function hideTypingIndicator() {
    const typingIndicator = getEl('typingIndicator');
    if (typingIndicator) typingIndicator.classList.remove('show');
}

// 按 Enter 键发送消息
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        const input = e.target;
        if (input && input.id === 'chatInput') {
            sendMessage();
        }
    }
});

// 初始化函数（支持动态加载场景）
let chatAppInitialized = false;
let chatDataLoaded = false;
async function initChatApp() {
    if (chatAppInitialized) return;
    chatAppInitialized = true;
    // 初始化弹窗拖动
    initPopupDrag();
}

// 懒加载聊天数据（首次打开聊天窗口时调用）
async function ensureChatDataLoaded() {
    if (chatDataLoaded) return;
    chatDataLoaded = true;
    await loadPaginationConfig();
    await loadModels();
    await loadSessions();
}

// 页面常规加载场景
window.addEventListener('load', function() {
    // 若窗口已加载完毕（动态注入场景由 navbar.js 调用 initChatApp），则跳过
    if (document.readyState === 'complete') return;
    initChatApp();
});

// 会话列表滚动监听 - 触底自动加载下一页
document.addEventListener('scroll', function(e) {
    const list = getEl('sessionList');
    if (!list || e.target !== list) return;
    // 距离底部 30px 时触发加载
    const threshold = 30;
    const isNearBottom = list.scrollTop + list.clientHeight >= list.scrollHeight - threshold;

    if (isNearBottom && sessionHasMore && !sessionIsLoading) {
        loadMoreSessions();
    }
}, true);

// ========== 多端同步（SSE / WebSocket 双模式） ==========

// 统一连接入口：自动探测传输方式并建立连接
async function connectSync() {
    // 清理旧连接和重连定时器
    cleanupAllSyncConnections();

    // 首次连接时从后端获取传输方式
    if (!syncTransport) {
        try {
            const resp = await fetch('/api/ai/sync/transport');
            const data = await resp.json();
            syncTransport = (data.success && data.data && data.data.transport) ? data.data.transport : 'sse';
            console.log('[Sync] 传输方式:', syncTransport);
        } catch (e) {
            console.warn('[Sync] 获取传输方式失败，降级为 SSE:', e);
            syncTransport = 'sse';
        }
    }

    if (syncTransport === 'websocket') {
        connectSyncWebSocket();
    } else {
        connectSyncSSE();
    }
}

// 统一断开入口
function disconnectSync() {
    if (syncReconnectTimer) {
        clearTimeout(syncReconnectTimer);
        syncReconnectTimer = null;
    }
    if (syncTransport === 'websocket') {
        disconnectSyncWebSocket();
    } else {
        disconnectSyncSSE();
    }
}

// 清理所有连接（切换模式或重连前调用）
function cleanupAllSyncConnections() {
    if (syncReconnectTimer) {
        clearTimeout(syncReconnectTimer);
        syncReconnectTimer = null;
    }
    if (syncPingTimer) {
        clearInterval(syncPingTimer);
        syncPingTimer = null;
    }
    if (syncEventSource) {
        syncEventSource.close();
        syncEventSource = null;
    }
    if (syncWebSocket) {
        syncWebSocket.close();
        syncWebSocket = null;
    }
}

// ---------- SSE 模式 ----------

// 连接 SSE
function connectSyncSSE() {
    try {
        syncEventSource = new EventSource('/api/ai/sync/events');

        syncEventSource.addEventListener('connected', function(e) {
            console.log('[SSE] 连接已建立');
        });

        syncEventSource.addEventListener('sync', function(e) {
            try {
                const event = JSON.parse(e.data);
                console.log('[SSE] 收到同步事件:', event);
                handleSyncEvent(event);
            } catch (err) {
                console.warn('[SSE] 解析事件失败:', err);
            }
        });

        syncEventSource.onerror = function() {
            console.warn('[SSE] 连接断开，将自动重连...');
            if (syncEventSource && syncEventSource.readyState === EventSource.CLOSED) {
                scheduleReconnect();
            }
        };
    } catch (error) {
        console.warn('[SSE] 创建 EventSource 失败:', error);
        scheduleReconnect();
    }
}

// 断开 SSE
function disconnectSyncSSE() {
    if (syncEventSource) {
        syncEventSource.close();
        syncEventSource = null;
    }
    // 通知服务端清理 SSE 连接资源
    fetch('/api/ai/sync/events', { method: 'DELETE' }).catch(() => {});
}

// ---------- WebSocket 模式 ----------

// 连接 WebSocket
function connectSyncWebSocket() {
    try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = protocol + '//' + window.location.host + '/ws/sync';
        syncWebSocket = new WebSocket(wsUrl);

        syncWebSocket.onopen = function() {
            console.log('[WebSocket] 连接已建立');
            // 启动心跳保活
            startWebSocketPing();
        };

        syncWebSocket.onmessage = function(e) {
            try {
                const msg = JSON.parse(e.data);
                console.log('[WebSocket] 收到消息:', msg);

                // 连接确认消息，无需处理
                if (msg.type === 'connected') {
                    console.log('[WebSocket] 服务端确认连接');
                    return;
                }
                // 心跳响应，忽略
                if (msg.type === 'pong') {
                    return;
                }
                // 同步事件（服务端推送的 sync 消息包含 type 和 sessionId）
                handleSyncEvent(msg);
            } catch (err) {
                console.warn('[WebSocket] 解析消息失败:', err);
            }
        };

        syncWebSocket.onerror = function(error) {
            console.warn('[WebSocket] 连接出错:', error);
        };

        syncWebSocket.onclose = function(event) {
            console.warn('[WebSocket] 连接关闭 (code: ' + event.code + ')');
            stopWebSocketPing();
            // 非主动关闭时尝试重连
            if (event.code !== 1000) {
                scheduleReconnect();
            }
        };
    } catch (error) {
        console.warn('[WebSocket] 创建连接失败:', error);
        scheduleReconnect();
    }
}

// 断开 WebSocket
function disconnectSyncWebSocket() {
    stopWebSocketPing();
    if (syncWebSocket) {
        // code 1000 = 正常关闭，不会触发重连
        syncWebSocket.close(1000);
        syncWebSocket = null;
    }
}

// 启动 WebSocket 心跳
function startWebSocketPing() {
    stopWebSocketPing();
    syncPingTimer = setInterval(() => {
        if (syncWebSocket && syncWebSocket.readyState === WebSocket.OPEN) {
            syncWebSocket.send(JSON.stringify({ type: 'ping' }));
        }
    }, SYNC_PING_INTERVAL);
}

// 停止 WebSocket 心跳
function stopWebSocketPing() {
    if (syncPingTimer) {
        clearInterval(syncPingTimer);
        syncPingTimer = null;
    }
}

// ---------- 公共逻辑 ----------

// 延迟重连
function scheduleReconnect() {
    if (syncReconnectTimer) return;
    syncReconnectTimer = setTimeout(() => {
        syncReconnectTimer = null;
        // 仅在聊天窗口仍然打开时重连
        const chatPopup = getEl('chatPopup');
        if (chatPopup && chatPopup.classList.contains('show')) {
            if (syncTransport === 'websocket') {
                connectSyncWebSocket();
            } else {
                connectSyncSSE();
            }
        }
    }, SYNC_RECONNECT_DELAY);
}

// 处理同步事件（SSE 和 WebSocket 共用）
async function handleSyncEvent(event) {
    const type = event.type;
    const sessionId = event.sessionId;

    if (type === 'session_created' || type === 'session_deleted') {
        await loadSessions();
        if (type === 'session_deleted' && sessionId === currentSessionId) {
            currentSessionId = null;
            clearChatMessages();
            if (allSessions.length > 0) {
                await selectSession(allSessions[0].sessionId);
            }
        }
    } else if (type === 'message_added') {
        await loadSessions();
        if (sessionId && sessionId === currentSessionId) {
            await loadSessionHistory(sessionId);
        }
    }
}

// 消息历史滚动监听 - 触顶自动加载更早消息
document.addEventListener('scroll', function(e) {
    const messages = getEl('chatMessages');
    if (!messages || e.target !== messages) return;
    // 距离顶部 30px 时触发加载
    const threshold = 30;
    const isNearTop = messages.scrollTop <= threshold;

    if (isNearTop && historyHasMore && !historyIsLoading) {
        loadMoreHistory();
    }
}, true);

// ========== 输入框历史消息（上下键翻页） ==========

// 发送历史：记录用户发送过的消息（最近 50 条）
const sendHistory = [];
const SEND_HISTORY_MAX = 50;
// 当前浏览历史的索引（-1 表示未浏览历史，正在输入新内容）
let historyIndex = -1;
// 暂存用户正在输入但还没发送的内容（按上键前输入框的内容）
let pendingInput = '';

/**
 * 将消息加入发送历史
 */
function pushSendHistory(message) {
    if (!message || !message.trim()) return;
    // 去重：如果和最后一条相同，不重复添加
    if (sendHistory.length > 0 && sendHistory[sendHistory.length - 1] === message) return;
    sendHistory.push(message);
    // 超过上限则移除最早的
    if (sendHistory.length > SEND_HISTORY_MAX) {
        sendHistory.shift();
    }
    // 重置历史浏览索引
    historyIndex = -1;
    pendingInput = '';
}

/**
 * 输入框键盘事件：上下键浏览历史消息
 */
document.addEventListener('keydown', function(e) {
    const input = e.target;
    if (!input || input.id !== 'chatInput') return;

    if (e.key === 'ArrowUp') {
        // 光标在开头时才触发历史浏览（避免与正常编辑冲突）
        if (input.selectionStart !== 0) return;
        e.preventDefault();

        if (sendHistory.length === 0) return;

        // 第一次按上键：暂存当前输入内容
        if (historyIndex === -1) {
            pendingInput = input.value;
        }
        // 索引向前（更早的消息）移动
        if (historyIndex < sendHistory.length - 1) {
            historyIndex++;
            input.value = sendHistory[sendHistory.length - 1 - historyIndex];
        }
    } else if (e.key === 'ArrowDown') {
        // 只有在浏览历史时才响应下键
        if (historyIndex === -1) return;
        e.preventDefault();

        // 索引向后（更新的消息）移动
        if (historyIndex > 0) {
            historyIndex--;
            input.value = sendHistory[sendHistory.length - 1 - historyIndex];
        } else {
            // 回到最新位置，恢复暂存的输入内容
            historyIndex = -1;
            input.value = pendingInput;
            pendingInput = '';
        }
    }
});
