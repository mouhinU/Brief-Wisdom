// DOM 元素引用（动态注入时可能延迟获取）
function getEl(id) { return document.getElementById(id); }

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

// 多端同步状态（SSE 实时推送）
let syncEventSource = null;
let syncReconnectTimer = null;
const SYNC_RECONNECT_DELAY = 3000; // 断线后 3 秒重连

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
        connectSyncSSE();
        setTimeout(() => {
            const input = getEl('chatInput');
            if (input) input.focus();
        }, 300);
    } else {
        disconnectSyncSSE();
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
    // 如果还没加载过会话列表，先加载
    if (allSessions.length === 0 && !sessionIsLoading) {
        await loadSessions();
    }
    // 如果加载后仍没有会话（新用户/未登录用户），自动创建一个
    if (!currentSessionId) {
        console.log('没有可用会话，自动创建新会话...');
        await createNewSession();
    }
}

// 加载会话列表（重置为第一页）
async function loadSessions() {
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
            
            // 选中第一个会话（不自动创建）
            if (allSessions.length > 0 && !currentSessionId) {
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
        
        sessionItem.innerHTML = `
            <div class="session-title">${escapeHtml(session.title)}</div>
            <div class="session-time">${timeStr}</div>
            <button class="delete-session-btn" onclick="deleteSession(event, '${session.sessionId}')">×</button>
        `;
        
        sessionItem.onclick = (e) => {
            if (!e.target.classList.contains('delete-session-btn')) {
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
            method: 'POST'
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
    
    if (!confirm('确定要删除这个会话吗？')) {
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

// 清空聊天消息
function clearChatMessages() {
    const messages = getEl('chatMessages');
    if (!messages) return;
    // 总是清空所有消息，显示欢迎界面
    messages.innerHTML = `
        <div class="welcome-message">
            <p>请输入您的问题,我会尽力为您解答</p>
        </div>
    `;
}

// 发送消息
async function sendMessage() {
    const chatInput = getEl('chatInput');
    const sendButton = getEl('sendButton');
    if (!chatInput) return;
    const message = chatInput.value.trim();
    
    if (!message) {
        return;
    }
    
    // 确保有当前会话
    if (!currentSessionId) {
        console.log('没有当前会话，创建新会话...');
        const newId = await createNewSession();
        console.log('创建后的 currentSessionId:', newId);
        if (!newId) {
            // createNewSession() 已经显示了错误信息
            if (sendButton) sendButton.disabled = false;
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

    // 添加用户消息
    addMessage(message, 'user');
    
    // 清空输入框
    chatInput.value = '';
    
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
                model: currentModel
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
        }
    } catch (error) {
        console.error('发送消息失败:', error);
        hideTypingIndicator();
        addMessage('抱歉,网络请求失败: ' + error.message, 'ai');
    } finally {
        if (sendButton) sendButton.disabled = false;
        if (chatInput) chatInput.focus();
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

// ========== 多端同步（SSE 实时推送） ==========

// 连接 SSE
function connectSyncSSE() {
    // 先关闭旧的 EventSource（不发 DELETE，由关闭聊天窗口时统一清理）
    if (syncReconnectTimer) {
        clearTimeout(syncReconnectTimer);
        syncReconnectTimer = null;
    }
    if (syncEventSource) {
        syncEventSource.close();
        syncEventSource = null;
    }

    try {
        syncEventSource = new EventSource('/api/ai/sync/events');

        // 连接成功
        syncEventSource.addEventListener('connected', function(e) {
            console.log('[SSE] 连接已建立');
        });

        // 收到同步事件
        syncEventSource.addEventListener('sync', function(e) {
            try {
                const event = JSON.parse(e.data);
                console.log('[SSE] 收到同步事件:', event);
                handleSyncEvent(event);
            } catch (err) {
                console.warn('[SSE] 解析事件失败:', err);
            }
        });

        // 连接出错（自动重连由浏览器 EventSource 处理）
        syncEventSource.onerror = function() {
            console.warn('[SSE] 连接断开，将自动重连...');
            // EventSource 会自动尝试重连，但如果.readyState === CLOSED 则需手动重连
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
    if (syncReconnectTimer) {
        clearTimeout(syncReconnectTimer);
        syncReconnectTimer = null;
    }
    if (syncEventSource) {
        syncEventSource.close();
        syncEventSource = null;
    }
    // 通知服务端清理 SSE 连接资源
    fetch('/api/ai/sync/events', { method: 'DELETE' }).catch(() => {});
}

// 延迟重连
function scheduleReconnect() {
    if (syncReconnectTimer) return;
    syncReconnectTimer = setTimeout(() => {
        syncReconnectTimer = null;
        // 仅在聊天窗口仍然打开时重连
        const chatPopup = getEl('chatPopup');
        if (chatPopup && chatPopup.classList.contains('show')) {
            connectSyncSSE();
        }
    }, SYNC_RECONNECT_DELAY);
}

// 处理同步事件
async function handleSyncEvent(event) {
    const type = event.type;
    const sessionId = event.sessionId;

    // 如果是当前设备自己触发的操作（发送消息后），服务端会推送事件，
    // 但由于当前设备已经更新了 UI，这里只需刷新会话列表即可
    if (type === 'session_created' || type === 'session_deleted') {
        // 会话列表变化，刷新会话列表
        await loadSessions();
        // 如果删除的是当前会话，清空聊天窗口
        if (type === 'session_deleted' && sessionId === currentSessionId) {
            currentSessionId = null;
            clearChatMessages();
            // 自动选中第一个会话
            if (allSessions.length > 0) {
                await selectSession(allSessions[0].sessionId);
            }
        }
    } else if (type === 'message_added') {
        // 消息变化，刷新会话列表和当前会话消息
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
