const aiFab = document.getElementById('aiFab');
const chatPopup = document.getElementById('chatPopup');
const chatMessages = document.getElementById('chatMessages');
const chatInput = document.getElementById('chatInput');
const sendButton = document.getElementById('sendButton');
const typingIndicator = document.getElementById('typingIndicator');
const sessionList = document.getElementById('sessionList');

// 当前会话 ID
let currentSessionId = null;

// 分页状态
let sessionCurrentPage = 1;
let sessionHasMore = false;
let sessionIsLoading = false;
let allSessions = [];  // 累积存储所有已加载的会话

// 分页配置（从后端动态获取）
let paginationConfig = {
    sessionList: { defaultSize: 20, maxSize: 100 },
    messageHistory: { defaultSize: 50, maxSize: 200 }
};

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

// 配置 marked.js
marked.setOptions({
    breaks: true,  // 支持换行符
    gfm: true,     // 启用 GitHub Flavored Markdown
    headerIds: false,
    mangle: false
});

// 切换聊天窗口显示/隐藏
function toggleChat() {
    chatPopup.classList.toggle('show');
    if (chatPopup.classList.contains('show')) {
        // 加载会话列表
        loadSessions();
        setTimeout(() => {
            chatInput.focus();
        }, 300);
    }
}

// 加载会话列表（重置为第一页）
async function loadSessions() {
    sessionCurrentPage = 1;
    allSessions = [];
    sessionHasMore = false;

    try {
        const pageSize = paginationConfig.sessionList.defaultSize;
        const response = await fetch(`/api/ai/sessions?page=1&size=${pageSize}`);
        const data = await response.json();
        
        if (data.success) {
            const pageData = data.data;
            allSessions = pageData.records || [];
            sessionHasMore = pageData.hasMore;
            sessionCurrentPage = 1;

            renderSessionList(allSessions);
            updateLoadMoreIndicator();
            
            // 如果没有会话，创建一个
            if (allSessions.length === 0) {
                await createNewSession();
            } else if (!currentSessionId) {
                // 选中第一个会话
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
        const response = await fetch(`/api/ai/sessions?page=${nextPage}&size=${pageSize}`);
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
    let indicator = document.getElementById('loadMoreIndicator');
    if (!indicator) {
        indicator = document.createElement('div');
        indicator.id = 'loadMoreIndicator';
        indicator.className = 'load-more-indicator';
        indicator.innerHTML = '<span>加载中...</span>';
        sessionList.appendChild(indicator);
    }
    indicator.style.display = 'flex';
}

function updateLoadMoreIndicator() {
    let indicator = document.getElementById('loadMoreIndicator');
    if (indicator) {
        indicator.remove();
    }
    if (sessionHasMore) {
        indicator = document.createElement('div');
        indicator.id = 'loadMoreIndicator';
        indicator.className = 'load-more-indicator';
        indicator.innerHTML = '<span>↓ 下拉加载更多</span>';
        sessionList.appendChild(indicator);
    }
}

// 渲染会话列表（全量替换）
function renderSessionList(sessions) {
    sessionList.innerHTML = '';
    appendSessionItems(sessions);
}

// 追加会话项（增量添加）
function appendSessionItems(sessions) {
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
        
        sessionList.appendChild(sessionItem);
    });
}

// 创建新会话
async function createNewSession() {
    try {
        const response = await fetch('/api/ai/session', {
            method: 'POST'
        });
        const data = await response.json();
        
        if (data.success) {
            const newSessionId = data.data;
            currentSessionId = newSessionId;
            
            // 清空聊天窗口
            clearChatMessages();
            
            // 重新加载会话列表以显示新会话
            await loadSessions();
            
            console.log('创建新会话成功:', newSessionId);
        }
    } catch (error) {
        console.error('创建会话失败:', error);
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
    const sessionItems = sessionList.querySelectorAll('.session-item');
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

// 加载会话历史
async function loadSessionHistory(sessionId) {
    try {
        console.log('正在加载会话历史:', sessionId);
        const response = await fetch(`/api/ai/session/${sessionId}/history`);
        const data = await response.json();
        
        console.log('历史记录响应:', data);
        
        if (data.success && data.data.length > 0) {
            console.log(`加载到 ${data.data.length} 条历史消息`);
            // 清空当前消息
            clearChatMessages();
            
            // 加载历史消息
            data.data.forEach(msg => {
                addMessage(msg.content, msg.role === 'user' ? 'user' : 'ai', false);
            });
        } else {
            console.log('没有历史消息');
            clearChatMessages();
        }
    } catch (error) {
        console.error('加载历史记录失败:', error);
    }
}

// 清空聊天消息
function clearChatMessages() {
    // 总是清空所有消息，显示欢迎界面
    chatMessages.innerHTML = `
        <div class="welcome-message">
            <h2>欢迎使用 AI 智能助手!</h2>
            <p>请输入您的问题,我会尽力为您解答</p>
        </div>
    `;
}

// 发送消息
async function sendMessage() {
    const message = chatInput.value.trim();
    
    if (!message) {
        return;
    }
    
    // 确保有当前会话
    if (!currentSessionId) {
        console.log('没有当前会话，创建新会话...');
        await createNewSession();
        console.log('创建后的 currentSessionId:', currentSessionId);
    }
    
    console.log('准备发送消息，sessionId:', currentSessionId, '消息:', message);

    // 清空欢迎消息
    const welcomeMessage = chatMessages.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.remove();
    }

    // 添加用户消息
    addMessage(message, 'user');
    
    // 清空输入框
    chatInput.value = '';
    
    // 禁用发送按钮
    sendButton.disabled = true;
    
    // 显示打字指示器
    showTypingIndicator();

    try {
        // 调用带上下文的 AI API
        if (!currentSessionId) {
            console.error('错误：currentSessionId 为空！');
            hideTypingIndicator();
            addMessage('错误：会话ID为空，请刷新页面重试', 'ai');
            sendButton.disabled = false;
            return;
        }
        
        const url = `/api/ai/chat/session/${currentSessionId}`;
        console.log('========== 发送请求 ==========');
        console.log('currentSessionId:', currentSessionId);
        console.log('请求 URL:', url);
        console.log('请求消息:', message);
        
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                message: message
            })
        });

        const data = await response.json();
        console.log('响应数据:', data);
        
        // 隐藏打字指示器
        hideTypingIndicator();

        if (data.success) {
            addMessage(data.data, 'ai');
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
        sendButton.disabled = false;
        chatInput.focus();
    }
}

// 添加消息到聊天窗口
function addMessage(text, sender, scroll = true) {
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
    } else {
        messageContent.textContent = text;
    }
    
    messageDiv.appendChild(messageContent);
    chatMessages.appendChild(messageDiv);
    
    // 滚动到底部
    if (scroll) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
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
    typingIndicator.classList.add('show');
    chatMessages.appendChild(typingIndicator);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// 隐藏打字指示器
function hideTypingIndicator() {
    typingIndicator.classList.remove('show');
}

// 按 Enter 键发送消息
chatInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
});

// 页面加载完成后聚焦输入框并加载分页配置
window.addEventListener('load', async function() {
    chatInput.focus();
    // 初始化时加载分页配置
    await loadPaginationConfig();
});

// 会话列表滚动监听 - 触底自动加载下一页
sessionList.addEventListener('scroll', function() {
    // 距离底部 30px 时触发加载
    const threshold = 30;
    const isNearBottom = sessionList.scrollTop + sessionList.clientHeight >= sessionList.scrollHeight - threshold;

    if (isNearBottom && sessionHasMore && !sessionIsLoading) {
        loadMoreSessions();
    }
});
