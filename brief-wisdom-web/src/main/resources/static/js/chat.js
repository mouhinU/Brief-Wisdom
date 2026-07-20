// ========== 常量配置 ==========
const CHAT_CONFIG = {
  MAX_SEND_HISTORY: 50, // 发送历史最大条数
  SCROLL_THRESHOLD: 30, // 滚动加载触发阈值(px)
  AUTO_SCROLL_DELAY: 100, // 自动滚动延迟(ms)
  MESSAGE_CACHE_SIZE: 100, // 消息缓存大小
  RECONNECT_DELAY: 3000, // 重连延迟(ms)
  PING_INTERVAL: 30000, // WebSocket心跳间隔(ms)
  DEBOUNCE_DELAY: 300, // 防抖延迟(ms)
};

// DOM 元素引用（动态注入时可能延迟获取）
function getEl(id) {
  return document.getElementById(id);
}

// 当前页面上下文（用于 AI 助手识别当前所在页面）
function getCurrentPageContext() {
  return window.location.pathname || "/";
}

// 页面上下文 -> 显示名称
function getPageContextLabel(pageContext) {
  const labels = {
    "/": "首页",
    "/about.html": "个人简历",
    "/resume-manage.html": "简历维护",
    "/system-settings.html": "系统设置",
    "/ai-manage.html": "AI管理",
  };
  return labels[pageContext] || "";
}

// 页面上下文 -> 图标
function getPageContextIcon(pageContext) {
  const icons = {
    "/": "🏠",
    "/about.html": "👤",
    "/resume-manage.html": "📝",
    "/system-settings.html": "⚙️",
    "/ai-manage.html": "🤖",
  };
  return icons[pageContext] || "💬";
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
let allSessions = []; // 累积存储所有已加载的会话

// 消息历史分页状态
let historyCurrentPage = 1;
let historyHasMore = false;
let historyIsLoading = false;

// 分页配置（从后端动态获取）
let paginationConfig = {
  sessionList: { defaultSize: 20, maxSize: 100 },
  messageHistory: { defaultSize: 20, maxSize: 200 },
};

// 流式输出配置
let chatStreamingEnabled = true; // 默认启用，从后端加载

// 多端同步状态（支持 SSE / WebSocket 双模式）
let syncTransport = null; // 'sse' 或 'websocket'，首次连接时从后端获取
let syncEventSource = null; // SSE 模式下的 EventSource 实例
let syncWebSocket = null; // WebSocket 模式下的 WebSocket 实例
let syncReconnectTimer = null;
let syncPingTimer = null; // WebSocket 心跳定时器
const SYNC_RECONNECT_DELAY = CHAT_CONFIG.RECONNECT_DELAY;
const SYNC_PING_INTERVAL = CHAT_CONFIG.PING_INTERVAL;

// 加载分页配置（页面初始化时调用一次）
async function loadPaginationConfig() {
  try {
    const response = await fetch("/api/ai/config/pagination");
    const data = await response.json();
    if (data.success) {
      paginationConfig = data.data;
      console.log("分页配置已加载:", paginationConfig);
    }
  } catch (error) {
    console.warn("加载分页配置失败，使用默认值:", error);
  }
}

// 加载聊天模式配置（流式/普通）
async function loadChatConfig() {
  try {
    // 从 application.yml 中读取 app.chat.streaming 配置
    // 由于无法直接读取 yml，这里通过一个专门的接口获取
    const response = await fetch("/api/ai/config/chat");
    const data = await response.json();
    if (data.success && data.data) {
      chatStreamingEnabled = data.data.streaming !== false; // 默认为 true
      console.log("聊天模式配置已加载: streaming=", chatStreamingEnabled);
    }
  } catch (error) {
    console.warn("加载聊天配置失败，使用默认值（流式启用）:", error);
    chatStreamingEnabled = true;
  }
}

// 加载可用模型列表
async function loadModels() {
  try {
    const response = await fetch("/api/ai/models/enabled");
    const data = await response.json();
    if (data.success && data.data) {
      availableModels = data.data;
      renderModelSelector();
    }
  } catch (error) {
    console.warn("加载模型列表失败:", error);
  }
}

// 渲染模型选择器
function renderModelSelector() {
  const selector = document.getElementById("modelSelector");
  if (!selector) return;

  selector.innerHTML = availableModels
    .map((m) => {
      // 根据思考模式添加标识
      let displayName = m.displayName;
      if (m.thinkingMode === "thinking") {
        displayName += " 🧠"; // 大脑图标表示思考模式
      }
      return `<option value="${m.modelName}" ${m.isActive === 1 ? "selected" : ""}>${displayName}</option>`;
    })
    .join("");

  // 设置当前模型为激活的模型
  const activeModel = availableModels.find((m) => m.isActive === 1);
  if (activeModel) {
    currentModel = activeModel.modelName;
    selector.value = currentModel;
  }
}

// 模型切换事件
function onModelChange() {
  const selector = getEl("modelSelector");
  if (selector) {
    currentModel = selector.value;
    console.log("切换模型:", currentModel);
  }
}

/**
 * 快捷提问功能
 */
function quickAsk(question) {
  const chatInput = getEl("chatInput");
  if (!chatInput) return;

  // 填充问题到输入框
  chatInput.value = question;
  chatInput.focus();

  // 自动发送（可选）
  // sendMessage();
}

// 配置 marked.js（如果已加载）
if (typeof marked !== "undefined") {
  marked.setOptions({
    breaks: true,
    gfm: true,
    headerIds: false,
    mangle: false,
  });
}

/**
 * 安全渲染 Markdown 为 HTML（防 XSS）
 * 使用 DOMPurify 过滤 marked.parse() 输出中的恶意脚本
 */
function renderMarkdown(text) {
  if (!text) return "";
  const html = typeof marked !== "undefined" ? marked.parse(text) : text;
  if (typeof DOMPurify !== "undefined") {
    return DOMPurify.sanitize(html, { ADD_ATTR: ["target"] });
  }
  return html;
}

// 显示会话列表加载中提示
function showSessionListLoading() {
  const list = getEl("sessionList");
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
  const chatPopup = getEl("chatPopup");
  if (!chatPopup) return;
  chatPopup.classList.toggle("show");
  if (chatPopup.classList.contains("show")) {
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
      const input = getEl("chatInput");
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
  const chatPopup = getEl("chatPopup");
  if (!chatPopup) return;
  const header = chatPopup.querySelector(".chat-header");
  if (!header) return;

  header.addEventListener("mousedown", function (e) {
    // 排除关闭按钮等交互元素
    if (e.target.closest(".close-button")) return;

    isDragging = true;
    dragStartX = e.clientX;
    dragStartY = e.clientY;

    // 将 right/bottom 定位转换为 left/top 定位
    const rect = chatPopup.getBoundingClientRect();
    popupStartLeft = rect.left;
    popupStartTop = rect.top;

    chatPopup.style.left = popupStartLeft + "px";
    chatPopup.style.top = popupStartTop + "px";
    chatPopup.style.right = "auto";
    chatPopup.style.bottom = "auto";

    document.body.style.userSelect = "none";
    e.preventDefault();
  });

  document.addEventListener("mousemove", function (e) {
    if (!isDragging) return;
    const chatPopup = getEl("chatPopup");
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

    chatPopup.style.left = newLeft + "px";
    chatPopup.style.top = newTop + "px";
  });

  document.addEventListener("mouseup", function () {
    if (!isDragging) return;
    isDragging = false;
    document.body.style.userSelect = "";
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
    const response = await fetch("/api/ai/sessions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ page: 1, size: pageSize }),
    });
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
    console.error("加载会话列表失败:", error);
  }
}

/**
 * 静默更新会话列表（不触发页面刷新，避免抖动）
 * 只更新左侧会话列表，不清空右侧聊天区域
 */
async function updateSessionListSilently() {
  try {
    const pageSize = paginationConfig.sessionList.defaultSize;
    const response = await fetch("/api/ai/sessions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ page: 1, size: pageSize }),
    });
    const data = await response.json();

    if (data.success) {
      const pageData = data.data;
      allSessions = pageData.records || [];
      sessionHasMore = pageData.hasMore;
      sessionCurrentPage = 1;

      // 保存当前选中的会话 ID
      const previousSessionId = currentSessionId;

      // 重新渲染会话列表
      renderSessionList(allSessions);
      updateLoadMoreIndicator();

      // 恢复选中状态（不会触发 loadSessionHistory）
      if (previousSessionId) {
        const activeItem = document.querySelector(
          `.session-item[data-session-id="${previousSessionId}"]`,
        );
        if (activeItem) {
          activeItem.classList.add("active");
        }
      }
    }
  } catch (error) {
    console.error("[静默更新] 会话列表失败:", error);
    // 静默失败，不影响用户体验
  }
}

/**
 * 局部更新当前会话项（只更新标题和时间，不发起网络请求）
 * @param {string} sessionId - 会话 ID
 */
function updateCurrentSessionItem(sessionId) {
  // 从内存缓存中找到当前会话
  const sessionIndex = allSessions.findIndex((s) => s.sessionId === sessionId);
  if (sessionIndex === -1) {
    console.warn("[局部更新] 未找到会话:", sessionId);
    return;
  }

  const session = allSessions[sessionIndex];

  // 更新会话的更新时间为当前时间（表示有新消息）
  session.updateTime = new Date().toISOString();

  // 如果标题还是默认的“新会话”，尝试根据最新消息生成新标题
  if (session.title === "新会话" || !session.title) {
    // 这里可以后续优化：从聊天记录中提取前几个字作为标题
    // 暂时保持原标题不变
  }

  // 找到对应的 DOM 元素
  const sessionItem = document.querySelector(
    `.session-item[data-session-id="${sessionId}"]`,
  );
  if (!sessionItem) {
    console.warn("[局部更新] 未找到会话项 DOM:", sessionId);
    return;
  }

  // 只更新时间（标题可能不需要每次更新）
  const timeEl = sessionItem.querySelector(".session-time");
  if (timeEl) {
    timeEl.textContent = formatTime(session.updateTime);
  }

  console.log("[局部更新] 会话项已更新:", sessionId, session.updateTime);
}

/**
 * 局部添加新会话项到列表（不重新加载整个列表）
 * @param {string} newSessionId - 新会话 ID
 */
async function addNewSessionToList(newSessionId) {
  try {
    // 获取新会话的信息
    const pageSize = paginationConfig.sessionList.defaultSize;
    const response = await fetch("/api/ai/sessions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ page: 1, size: pageSize }),
    });
    const data = await response.json();

    if (!data.success || !data.data || !data.data.records) {
      return;
    }

    // 找到新会话
    const newSession = data.data.records.find(
      (s) => s.sessionId === newSessionId,
    );
    if (!newSession) {
      console.warn("[局部添加] 未找到新会话:", newSessionId);
      return;
    }

    // 添加到内存缓存的开头
    allSessions.unshift(newSession);

    // 在 DOM 中插入新会话项
    const list = getEl("sessionList");
    if (!list) return;

    // 移除空提示
    const emptyTip = list.querySelector(".session-empty-tip");
    if (emptyTip) {
      emptyTip.remove();
    }

    // 创建新会话项
    const sessionItem = document.createElement("div");
    sessionItem.className = "session-item active"; // 默认选中
    sessionItem.dataset.sessionId = newSession.sessionId;

    const timeStr = formatTime(newSession.updateTime);
    const pageLabel = getPageContextLabel(newSession.pageContext);
    const pageLabelHtml = pageLabel
      ? `<span class="session-page-label" title="${escapeHtml(pageLabel)}">${getPageContextIcon(newSession.pageContext)}</span>`
      : "";

    sessionItem.innerHTML = `
            <div class="session-title-row">
                ${pageLabelHtml}
                <div class="session-title" title="双击编辑标题">${escapeHtml(newSession.title)}</div>
            </div>
            <div class="session-time">${timeStr}</div>
            <button class="delete-session-btn" onclick="deleteSession(event, '${newSession.sessionId}')">×</button>
        `;

    // 双击标题进入编辑模式
    const titleEl = sessionItem.querySelector(".session-title");
    titleEl.addEventListener("dblclick", (e) => {
      e.stopPropagation();
      startEditSessionTitle(titleEl, newSession.sessionId, newSession.title);
    });

    sessionItem.onclick = (e) => {
      if (
        !e.target.classList.contains("delete-session-btn") &&
        !e.target.classList.contains("session-title-input")
      ) {
        selectSession(newSession.sessionId);
      }
    };

    // 取消其他会话的选中状态
    list.querySelectorAll(".session-item").forEach((item) => {
      item.classList.remove("active");
    });

    // 插入到列表开头
    if (list.firstChild) {
      list.insertBefore(sessionItem, list.firstChild);
    } else {
      list.appendChild(sessionItem);
    }

    console.log("[局部添加] 新会话项已添加:", newSessionId);
  } catch (error) {
    console.error("[局部添加] 失败:", error);
  }
}

/**
 * 局部删除会话项（不重新加载整个列表）
 * @param {string} sessionId - 会话 ID
 */
function removeSessionFromList(sessionId) {
  // 从内存缓存中删除
  const index = allSessions.findIndex((s) => s.sessionId === sessionId);
  if (index !== -1) {
    allSessions.splice(index, 1);
  }

  // 从 DOM 中删除
  const sessionItem = document.querySelector(
    `.session-item[data-session-id="${sessionId}"]`,
  );
  if (sessionItem) {
    sessionItem.remove();
  }

  // 如果列表为空，显示空提示
  const list = getEl("sessionList");
  if (list && list.querySelectorAll(".session-item").length === 0) {
    list.innerHTML =
      '<div class="session-empty-tip">暂无会话，发送消息即可开始</div>';
  }

  console.log("[局部删除] 会话项已删除:", sessionId);
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
    const response = await fetch("/api/ai/sessions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ page: nextPage, size: pageSize }),
    });
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
    console.error("加载更多会话失败:", error);
  } finally {
    sessionIsLoading = false;
  }
}

// 显示/隐藏加载更多指示器
function showLoadMoreIndicator() {
  const list = getEl("sessionList");
  if (!list) return;
  let indicator = document.getElementById("loadMoreIndicator");
  if (!indicator) {
    indicator = document.createElement("div");
    indicator.id = "loadMoreIndicator";
    indicator.className = "load-more-indicator";
    indicator.innerHTML = "<span>加载中...</span>";
    list.appendChild(indicator);
  }
  indicator.style.display = "flex";
}

function updateLoadMoreIndicator() {
  let indicator = document.getElementById("loadMoreIndicator");
  if (indicator) {
    indicator.remove();
  }
  const list = getEl("sessionList");
  if (!list) return;
  if (sessionHasMore) {
    indicator = document.createElement("div");
    indicator.id = "loadMoreIndicator";
    indicator.className = "load-more-indicator";
    indicator.innerHTML = "<span>↓ 下拉加载更多</span>";
    list.appendChild(indicator);
  }
}

// 渲染会话列表（全量替换）
function renderSessionList(sessions) {
  const list = getEl("sessionList");
  if (!list) return;
  list.innerHTML = "";
  if (!sessions || sessions.length === 0) {
    list.innerHTML =
      '<div class="session-empty-tip">暂无会话，发送消息即可开始</div>';
    return;
  }
  appendSessionItems(sessions);
}

// 追加会话项（增量添加）
function appendSessionItems(sessions) {
  const list = getEl("sessionList");
  if (!list) return;
  // 移除加载更多指示器（稍后重新添加）
  const existingIndicator = document.getElementById("loadMoreIndicator");
  if (existingIndicator) {
    existingIndicator.remove();
  }

  sessions.forEach((session) => {
    const sessionItem = document.createElement("div");
    sessionItem.className = "session-item";
    sessionItem.dataset.sessionId = session.sessionId; // 添加 data 属性
    if (session.sessionId === currentSessionId) {
      sessionItem.classList.add("active");
    }

    // 使用会话的更新时间（即最后一条消息的时间）
    const timeStr = formatTime(session.updateTime);

    // 页面上下文图标
    const pageLabel = getPageContextLabel(session.pageContext);
    const pageLabelHtml = pageLabel
      ? `<span class="session-page-label" title="${escapeHtml(pageLabel)}">${getPageContextIcon(session.pageContext)}</span>`
      : "";

    sessionItem.innerHTML = `
            <div class="session-title-row">
                ${pageLabelHtml}
                <div class="session-title" title="双击编辑标题">${escapeHtml(session.title)}</div>
            </div>
            <div class="session-time">${timeStr}</div>
            <button class="delete-session-btn" onclick="deleteSession(event, '${session.sessionId}')">×</button>
        `;

    // 双击标题进入编辑模式
    const titleEl = sessionItem.querySelector(".session-title");
    titleEl.addEventListener("dblclick", (e) => {
      e.stopPropagation();
      startEditSessionTitle(titleEl, session.sessionId, session.title);
    });

    sessionItem.onclick = (e) => {
      if (
        !e.target.classList.contains("delete-session-btn") &&
        !e.target.classList.contains("session-title-input")
      ) {
        selectSession(session.sessionId);
      }
    };

    list.appendChild(sessionItem);
  });
}

// 创建新会话（返回 sessionId 或 null）
async function createNewSession() {
  try {
    const response = await fetch("/api/ai/session", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        pageContext: getCurrentPageContext(),
      }),
    });
    const data = await response.json();

    if (data.success && data.data) {
      const newSessionId = data.data;
      currentSessionId = newSessionId;

      // 清空聊天窗口
      clearChatMessages();

      // 注意：不再手动添加到列表，等待 SSE 通知 session_created 后自动刷新列表
      // 这样可以避免重复添加
      console.log("创建新会话成功:", newSessionId);
      return newSessionId;
    } else {
      console.error("创建会话返回失败:", data);
      // 显示后端返回的具体错误信息
      const errorMsg = data.msg || data.error || "创建会话失败";
      addMessage("错误：" + errorMsg, "ai");
      return null;
    }
  } catch (error) {
    console.error("创建会话失败:", error);
    addMessage("错误：网络请求失败，请检查网络连接", "ai");
    return null;
  }
}

// 选择会话
async function selectSession(sessionId) {
  console.log("切换会话:", sessionId);
  currentSessionId = sessionId;

  // 重置发送历史浏览状态
  historyIndex = -1;
  pendingInput = "";

  // 先清空当前聊天内容
  clearChatMessages();

  // 加载历史消息
  await loadSessionHistory(sessionId);

  // 更新会话列表UI的高亮状态
  document.querySelectorAll(".session-item").forEach((item) => {
    item.classList.remove("active");
  });

  // 找到对应的会话项并添加高亮
  const list = getEl("sessionList");
  if (!list) return;
  const sessionItems = list.querySelectorAll(".session-item");
  sessionItems.forEach((item) => {
    const deleteBtn = item.querySelector(".delete-session-btn");
    if (deleteBtn) {
      const sessionIdAttr = deleteBtn.getAttribute("onclick");
      if (sessionIdAttr && sessionIdAttr.includes(sessionId)) {
        item.classList.add("active");
      }
    }
  });

  console.log("会话切换完成");
}

// 删除会话
async function deleteSession(event, sessionId) {
  event.stopPropagation();

  const confirmed = await showConfirmDialog("确定要删除这个会话吗？", "🗑️");
  if (!confirmed) {
    return;
  }

  try {
    const response = await fetch(`/api/ai/session/${sessionId}`, {
      method: "DELETE",
    });
    const data = await response.json();

    if (data.success) {
      if (currentSessionId === sessionId) {
        currentSessionId = null;
        clearChatMessages();
      }
      // 局部删除会话项（不重新加载整个列表）
      removeSessionFromList(sessionId);
    }
  } catch (error) {
    console.error("删除会话失败:", error);
  }
}

// 开始编辑会话标题（双击进入编辑模式）
function startEditSessionTitle(titleEl, sessionId, currentTitle) {
  // 如果已经在编辑中，忽略
  if (titleEl.querySelector(".session-title-input")) {
    return;
  }

  const originalTitle = currentTitle;

  // 创建输入框
  const input = document.createElement("input");
  input.type = "text";
  input.className = "session-title-input";
  input.value = currentTitle;

  // 保存原始标题
  titleEl.dataset.originalTitle = originalTitle;
  titleEl.textContent = "";
  titleEl.appendChild(input);

  // 聚焦并选中文本
  input.focus();
  input.select();

  // 保存标题
  const saveTitle = async () => {
    const newTitle = input.value.trim();

    // 如果标题没变，恢复原状
    if (newTitle === originalTitle || newTitle === "") {
      titleEl.textContent = originalTitle;
      return;
    }

    try {
      const response = await fetch(`/api/ai/session/${sessionId}/title`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: newTitle }),
      });
      const data = await response.json();

      if (data.success) {
        titleEl.textContent = newTitle;
        console.log("会话标题已更新:", newTitle);
      } else {
        titleEl.textContent = originalTitle;
        console.error("更新标题失败:", data);
      }
    } catch (error) {
      titleEl.textContent = originalTitle;
      console.error("更新标题失败:", error);
    }
  };

  // 回车保存
  input.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      saveTitle();
    } else if (e.key === "Escape") {
      // Esc 取消编辑
      titleEl.textContent = originalTitle;
    }
  });

  // 失焦保存
  input.addEventListener("blur", () => {
    saveTitle();
  });
}

// 显示聊天区域加载中提示
function showChatLoading() {
  const messages = getEl("chatMessages");
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
    console.log("正在加载会话历史:", sessionId);
    const pageSize = paginationConfig.messageHistory.defaultSize;
    const response = await fetch(`/api/ai/session/${sessionId}/history`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ page: 1, size: pageSize }),
    });
    const data = await response.json();

    console.log("历史记录响应:", data);

    // 清空当前消息
    clearChatMessages();

    if (data.success && data.data.records && data.data.records.length > 0) {
      const records = data.data.records;
      historyCurrentPage = 1;
      historyHasMore = data.data.hasMore;

      console.log(
        `加载到 ${records.length} 条历史消息，还有更多: ${historyHasMore}`,
      );

      // 渲染消息（接口已按正序返回）
      records.forEach((msg) => {
        addMessage(
          msg.content,
          msg.role === "user" ? "user" : "ai",
          false,
          msg.model,
        );
      });

      // 如果有更多历史消息，显示顶部加载提示
      if (historyHasMore) {
        showHistoryLoadMoreIndicator();
      }

      // 滚动到底部
      const messages = getEl("chatMessages");
      if (messages) {
        messages.scrollTop = messages.scrollHeight;
      }
    } else {
      console.log("没有历史消息");
    }
  } catch (error) {
    console.error("加载历史记录失败:", error);
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
    const response = await fetch(
      `/api/ai/session/${currentSessionId}/history`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ page: nextPage, size: pageSize }),
      },
    );
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
    console.error("加载更多历史消息失败:", error);
  } finally {
    historyIsLoading = false;
  }
}

// 在聊天窗口顶部插入消息（用于加载更早的历史消息）
function prependMessages(records) {
  const messages = getEl("chatMessages");
  if (!messages) return;

  // 记录当前滚动高度，用于加载后保持位置
  const oldScrollHeight = messages.scrollHeight;

  // 移除顶部的加载指示器
  removeHistoryLoadIndicator();

  // 将消息按顺序插入到顶部（欢迎消息之后）
  const welcomeMsg = messages.querySelector(".welcome-message");
  const insertBefore = welcomeMsg
    ? welcomeMsg.nextSibling
    : messages.firstChild;

  // ✅ 关键修复：逆序遍历，确保最早的消息在最上面
  // records 是正序 [早 -> 晚]，需要从后往前插入，这样最早的才会出现在最顶部
  for (let i = records.length - 1; i >= 0; i--) {
    const msg = records[i];
    const messageDiv = document.createElement("div");
    messageDiv.className = `message ${msg.role === "user" ? "user" : "ai"}`;

    const messageContent = document.createElement("div");
    messageContent.className = "message-content";

    if (msg.role === "user") {
      messageContent.textContent = msg.content;
    } else {
      try {
        messageContent.innerHTML = renderMarkdown(msg.content);
      } catch (e) {
        messageContent.textContent = msg.content;
      }
      if (msg.model) {
        const modelLabel = document.createElement("div");
        modelLabel.className = "message-model-label";
        modelLabel.textContent = msg.model;
        messageDiv.appendChild(modelLabel);
      }
    }

    // 添加复制按钮（仅 AI 回复）
    if (msg.role === "ai") {
      const copyBtn = createCopyButton(msg.content);
      messageDiv.appendChild(copyBtn);
    }

    messageDiv.appendChild(messageContent);
    messages.insertBefore(messageDiv, insertBefore);
  }

  // 保持用户当前的阅读位置（不跳到顶部或底部）
  const newScrollHeight = messages.scrollHeight;
  messages.scrollTop =
    oldScrollHeight > 0
      ? messages.scrollTop + (newScrollHeight - oldScrollHeight)
      : 0;
}

// 消息历史 - 顶部"加载更多"指示器
function showHistoryLoadMoreIndicator() {
  removeHistoryLoadIndicator();
  const messages = getEl("chatMessages");
  if (!messages) return;

  const indicator = document.createElement("div");
  indicator.id = "historyLoadMore";
  indicator.className = "history-load-more-indicator";
  indicator.innerHTML = "<span>↑ 滚动加载更早消息</span>";
  indicator.onclick = () => loadMoreHistory();
  messages.insertBefore(indicator, messages.firstChild);
}

function showHistoryLoadingIndicator() {
  removeHistoryLoadIndicator();
  const messages = getEl("chatMessages");
  if (!messages) return;

  const indicator = document.createElement("div");
  indicator.id = "historyLoadMore";
  indicator.className = "history-load-more-indicator";
  indicator.innerHTML = "<span>加载中...</span>";
  messages.insertBefore(indicator, messages.firstChild);
}

function removeHistoryLoadIndicator() {
  const indicator = document.getElementById("historyLoadMore");
  if (indicator) indicator.remove();
}

function showHistoryFullyLoaded() {
  const messages = getEl("chatMessages");
  if (!messages) return;
  // 不再显示任何提示，已加载全部
}

/**
 * 创建消息复制按钮
 */
function createCopyButton(content) {
  const copyBtn = document.createElement("button");
  copyBtn.className = "message-copy-btn";
  copyBtn.title = "复制内容";
  copyBtn.innerHTML =
    '<svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>';

  copyBtn.onclick = async () => {
    try {
      await navigator.clipboard.writeText(content);
      copyBtn.classList.add("copied");
      copyBtn.innerHTML =
        '<svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z"/></svg>';
      setTimeout(() => {
        copyBtn.classList.remove("copied");
        copyBtn.innerHTML =
          '<svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>';
      }, 2000);
    } catch (err) {
      console.error("复制失败:", err);
    }
  };

  return copyBtn;
}

// 清空聊天消息，显示欢迎界面
function clearChatMessages() {
  const messages = getEl("chatMessages");
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
let activeEventSource = null; // 当前活跃的 SSE EventSource（用于终止流式响应）
let isUserStopped = false; // 标记当前流式响应是否被用户主动终止
async function sendMessage() {
  const chatInput = getEl("chatInput");
  const sendButton = getEl("sendButton");
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
    console.log("没有当前会话，创建新会话...");
    const newId = await createNewSession();
    console.log("创建后的 currentSessionId:", newId);
    if (!newId) {
      // createNewSession() 已经显示了错误信息
      if (sendButton) sendButton.disabled = false;
      isSending = false;
      return;
    }
  }

  console.log("准备发送消息，sessionId:", currentSessionId, "消息:", message);

  // 清空欢迎消息
  const messages = getEl("chatMessages");
  if (messages) {
    const welcomeMessage = messages.querySelector(".welcome-message");
    if (welcomeMessage) {
      welcomeMessage.remove();
    }
  }

  // 记录到发送历史
  pushSendHistory(message);

  // 添加用户消息
  addMessage(message, "user");

  // 清空输入框并禁用（防止重复提交）
  chatInput.value = "";
  chatInput.disabled = true;

  // 禁用发送按钮
  if (sendButton) sendButton.disabled = true;

  // 显示打字指示器
  showTypingIndicator();

  try {
    // 调用带上下文的 AI API
    if (!currentSessionId) {
      console.error("错误：currentSessionId 为空！");
      hideTypingIndicator();
      addMessage("错误：会话ID为空，请刷新页面重试", "ai");
      if (sendButton) sendButton.disabled = false;
      if (chatInput) {
        chatInput.disabled = false;
        chatInput.value = message;
        chatInput.focus();
      }
      isSending = false;
      return;
    }

    // 根据配置选择流式或普通模式
    if (chatStreamingEnabled) {
      // 流式模式
      await sendMessageStream(
        currentSessionId,
        message,
        currentModel,
        getCurrentPageContext(),
      );
    } else {
      // 普通模式
      await sendMessageNormal(
        currentSessionId,
        message,
        currentModel,
        getCurrentPageContext(),
      );
    }
  } catch (error) {
    console.error("发送消息失败:", error);
    hideTypingIndicator();
    addMessage("抱歉,网络请求失败: " + error.message, "ai");
    // 失败时恢复输入框内容
    if (chatInput) {
      chatInput.value = message;
    }
  } finally {
    if (sendButton) sendButton.disabled = false;
    if (chatInput) {
      chatInput.disabled = false;
      chatInput.focus();
    }
    // 兜底：确保停止按钮隐藏、发送按钮恢复
    toggleStopButton(false);
    isSending = false;
  }
}

/**
 * 普通模式发送消息（阻塞式）
 */
async function sendMessageNormal(sessionId, message, model, pageContext) {
  const url = `/api/ai/chat/session/${sessionId}`;
  console.log("========== 发送请求（普通模式） ==========");
  console.log("currentSessionId:", sessionId);
  console.log("model:", model);
  console.log("请求 URL:", url);
  console.log("请求消息:", message);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: message,
      model: model,
      pageContext: pageContext,
    }),
  });

  const data = await response.json();
  console.log("响应数据:", data);

  // 隐藏打字指示器
  hideTypingIndicator();

  if (data.success) {
    addMessage(data.data, "ai", true, model);
    // 局部更新当前会话项的标题和时间（不重新加载整个列表）
    updateCurrentSessionItem(sessionId);
  } else {
    addMessage("抱歉,出现了错误: " + data.error, "ai");
  }
}

/**
 * 流式模式发送消息（SSE）
 */
async function sendMessageStream(sessionId, message, model, pageContext) {
  const url = `/api/ai/chat/session/${sessionId}/stream?message=${encodeURIComponent(message)}&model=${encodeURIComponent(model || "")}&pageContext=${encodeURIComponent(pageContext || "")}`;
  console.log("========== 发送请求（流式模式） ==========");
  console.log("currentSessionId:", sessionId);
  console.log("model:", model);
  console.log("请求 URL:", url);

  // 隐藏打字指示器，准备接收流式数据
  hideTypingIndicator();

  // 创建一个空的 AI 消息容器
  const aiMessageDiv = document.createElement("div");
  aiMessageDiv.className = "message ai";

  const messageContent = document.createElement("div");
  messageContent.className = "message-content";
  messageContent.innerHTML = '<span class="typing-cursor">|</span>'; // 显示光标

  // 添加模型标签
  if (model) {
    const modelLabel = document.createElement("div");
    modelLabel.className = "message-model-label";
    modelLabel.textContent = model;
    aiMessageDiv.appendChild(modelLabel);
  }

  aiMessageDiv.appendChild(messageContent);
  const messages = getEl("chatMessages");
  if (messages) {
    messages.appendChild(aiMessageDiv);
    messages.scrollTop = messages.scrollHeight;
  }

  // 使用 EventSource 接收 SSE 流
  return new Promise((resolve, reject) => {
    let fullText = "";
    isUserStopped = false; // 重置终止标志
    const eventSource = new EventSource(url);

    // 保存 EventSource 引用，支持外部终止
    activeEventSource = eventSource;

    // 切换为"停止"按钮
    toggleStopButton(true);

    eventSource.onmessage = function (event) {
      const chunk = event.data;
      if (chunk) {
        fullText += chunk;
        // 实时更新消息内容（带 Markdown 渲染）
        try {
          messageContent.innerHTML =
            renderMarkdown(fullText) + '<span class="typing-cursor">|</span>';
        } catch (e) {
          messageContent.textContent = fullText + "|";
        }
        // 自动滚动
        if (messages) {
          messages.scrollTop = messages.scrollHeight;
        }
      }
    };

    // 监听错误事件（包括内容安全拦截）
    eventSource.addEventListener("error", function (event) {
      // ✅ 静默处理：不打印日志，避免混淆（流式聊天始终使用SSE，与多端同步配置无关）

      // 尝试解析错误数据
      if (event.data) {
        try {
          const errorData = JSON.parse(event.data);
          if (errorData.type === "CONTENT_BLOCKED") {
            // 显示友好的错误提示
            messageContent.innerHTML = `<div class="message-error">${errorData.message}</div>`;
            eventSource.close();
            activeEventSource = null;
            toggleStopButton(false);
            resolve(); // 不算失败，只是被拦截
            return;
          }
        } catch (e) {
          // 静默忽略解析错误
        }
      }

      // 其他错误
      eventSource.close();
      activeEventSource = null;
      toggleStopButton(false);
      reject(new Error("请求被服务器拒绝"));
    });

    eventSource.onerror = function (error) {
      // ✅ 静默处理：流式聊天的SSE连接关闭是正常行为，不打印错误日志

      if (isUserStopped) {
        // 用户主动终止：标记消息为已终止状态
        aiMessageDiv.classList.add("message-stopped");
        messageContent.innerHTML = renderMarkdown(fullText);
        // 添加终止提示标识
        if (fullText && fullText.length > 0) {
          const stoppedTip = document.createElement("div");
          stoppedTip.className = "message-stopped-indicator";
          stoppedTip.textContent = "已停止生成";
          aiMessageDiv.appendChild(stoppedTip);
        }
        eventSource.close();
        activeEventSource = null;

        if (fullText && fullText.length > 0) {
          saveStreamedMessage(sessionId, fullText, model)
            .then(() => updateCurrentSessionItem(sessionId))
            .catch(() => {})
            .finally(() => resolve());
        } else {
          // 完全没有内容，移除空的 AI 消息气泡
          aiMessageDiv.remove();
          resolve();
        }
      } else if (fullText && fullText.length > 0) {
        // 已接收到内容，连接异常断开（非用户主动终止）
        eventSource.close();
        activeEventSource = null;
        // 移除光标
        messageContent.innerHTML = renderMarkdown(fullText);

        // 保存 AI 回复到数据库（后台静默完成）
        saveStreamedMessage(sessionId, fullText, model)
          .then(() => {
            // 局部更新当前会话项的标题和时间（不重新加载整个列表）
            updateCurrentSessionItem(sessionId);
            resolve();
          })
          .catch((error) => {
            // 即使保存失败，也更新会话项
            updateCurrentSessionItem(sessionId);
            resolve();
          });
      } else {
        // 完全没有接收到内容，才是真正的错误
        eventSource.close();
        activeEventSource = null;
        reject(error);
      }
      toggleStopButton(false);
    };

    // 监听服务器发送的"已终止"事件
    eventSource.addEventListener("stopped", function () {
      isUserStopped = true;
      eventSource.close();
      activeEventSource = null;
      // 标记消息为已终止状态
      aiMessageDiv.classList.add("message-stopped");
      // 移除光标，保留已接收的部分内容
      messageContent.innerHTML = renderMarkdown(fullText);
      // 添加终止提示标识
      if (fullText && fullText.length > 0) {
        const stoppedTip = document.createElement("div");
        stoppedTip.className = "message-stopped-indicator";
        stoppedTip.textContent = "已停止生成";
        aiMessageDiv.appendChild(stoppedTip);
      }
      // 保存已生成的部分内容
      if (fullText && fullText.length > 0) {
        saveStreamedMessage(sessionId, fullText, model)
          .then(() => updateCurrentSessionItem(sessionId))
          .catch(() => {});
      }
      // 恢复 UI 状态：隐藏停止按钮，显示发送按钮
      toggleStopButton(false);
      resolve();
    });

    eventSource.addEventListener("complete", function () {
      eventSource.close();
      activeEventSource = null;
      toggleStopButton(false);

      if (!fullText || fullText.trim().length === 0) {
        // 流式完成但内容为空（模型未返回有效内容），显示错误提示
        messageContent.innerHTML = '<div class="message-error">AI 未返回有效内容，请检查模型配置或稍后重试</div>';
        aiMessageDiv.classList.add("message-empty");
        resolve();
        return;
      }

      // 移除光标
      messageContent.innerHTML = renderMarkdown(fullText);

      // 保存 AI 回复到数据库（后台静默完成）
      saveStreamedMessage(sessionId, fullText, model)
        .then(() => {
          // 局部更新当前会话项的标题和时间（不重新加载整个列表）
          updateCurrentSessionItem(sessionId);
          resolve();
        })
        .catch((error) => {
          // 即使保存失败，也更新会话项
          updateCurrentSessionItem(sessionId);
          resolve();
        });
    });
  });
}

/**
 * 终止正在进行的流式会话
 * <p>
 * 用户点击"停止生成"按钮时调用。
 * 先关闭前端 EventSource 连接，再通知后端取消 AI 模型流式输出。
 */
function stopStreaming() {
  // 标记为用户主动终止（onerror 回调会检查此标志）
  isUserStopped = true;

  // 关闭前端 EventSource
  if (activeEventSource) {
    activeEventSource.close();
    activeEventSource = null;
  }

  // 通知后端终止流式订阅
  fetch("/api/ai/chat/stream/stop", { method: "DELETE" })
    .then(() => console.log("[流式] 已通知后端终止流式会话"))
    .catch((e) => console.warn("[流式] 通知后端终止失败:", e));

  // 恢复 UI 状态
  toggleStopButton(false);
}

/**
 * 切换"发送"按钮与"停止"按钮的显示状态
 *
 * @param {boolean} showStop - true 显示停止按钮，false 显示发送按钮
 */
function toggleStopButton(showStop) {
  const sendBtn = getEl("sendButton");
  const stopBtn = getEl("stopButton");
  if (sendBtn) sendBtn.style.display = showStop ? "none" : "";
  if (stopBtn) stopBtn.style.display = showStop ? "" : "none";
}

/**
 * 保存流式输出的 AI 消息到数据库
 */
async function saveStreamedMessage(sessionId, content, model) {
  try {
    const response = await fetch("/api/ai/message/save", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        sessionId: sessionId,
        content: content,
        model: model || null,
      }),
    });

    const data = await response.json();
    if (!data.success) {
      console.warn("[流式] 消息保存失败:", data.msg);
    }
    // ✅ 成功时不打印日志，保持控制台清爽
  } catch (error) {
    console.error("[流式] 保存消息异常:", error);
    throw error;
  }
}

// 添加消息到聊天窗口
function addMessage(text, sender, scroll = true, modelName = null) {
  const messageDiv = document.createElement("div");
  messageDiv.className = `message ${sender}`;

  const messageContent = document.createElement("div");
  messageContent.className = "message-content";

  // 对 AI 消息进行 Markdown 渲染，用户消息保持纯文本
  if (sender === "ai") {
    try {
      messageContent.innerHTML = renderMarkdown(text);
    } catch (error) {
      console.error("Markdown 解析错误:", error);
      messageContent.textContent = text;
    }
    // 显示模型名称
    if (modelName) {
      const modelLabel = document.createElement("div");
      modelLabel.className = "message-model-label";

      // 查找当前模型的配置，判断是否有思考模式
      const modelConfig = availableModels.find(
        (m) => m.modelName === modelName,
      );
      let displayModelName = modelName;
      if (modelConfig && modelConfig.thinkingMode === "thinking") {
        displayModelName += " 🧠"; // 大脑图标表示思考模式
      }

      modelLabel.textContent = displayModelName;
      messageDiv.appendChild(modelLabel);
    }
  } else {
    messageContent.textContent = text;
  }

  messageDiv.appendChild(messageContent);
  const messages = getEl("chatMessages");
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
    return "";
  }

  let date;

  // 处理字符串格式的时间 (ISO 8601)
  if (typeof timeInput === "string") {
    date = new Date(timeInput);
  }
  // 处理数组格式的时间 [year, month, day, hour, minute, second, ...]
  else if (Array.isArray(timeInput) && timeInput.length >= 5) {
    const [year, month, day, hour, minute] = timeInput;
    date = new Date(year, month - 1, day, hour, minute);
  } else {
    return "";
  }

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return "";
  }

  const now = new Date();
  const diff = now - date;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) {
    return "刚刚";
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
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

// 显示打字指示器
function showTypingIndicator() {
  const typingIndicator = getEl("typingIndicator");
  const messages = getEl("chatMessages");
  if (!typingIndicator || !messages) return;
  typingIndicator.classList.add("show");
  messages.appendChild(typingIndicator);
  messages.scrollTop = messages.scrollHeight;
}

// 隐藏打字指示器
function hideTypingIndicator() {
  const typingIndicator = getEl("typingIndicator");
  if (typingIndicator) typingIndicator.classList.remove("show");
}

// 按 Enter 键发送消息
document.addEventListener("keypress", function (e) {
  if (e.key === "Enter") {
    const input = e.target;
    if (input && input.id === "chatInput") {
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
  await loadChatConfig(); // 加载聊天模式配置（流式/普通）
  await loadModels();
  await loadSessions();
}

// 页面常规加载场景
window.addEventListener("load", function () {
  // 若窗口已加载完毕（动态注入场景由 navbar.js 调用 initChatApp），则跳过
  if (document.readyState === "complete") return;
  initChatApp();
});

// 会话列表滚动监听 - 触底自动加载下一页
document.addEventListener(
  "scroll",
  function (e) {
    const list = getEl("sessionList");
    if (!list || e.target !== list) return;
    // 距离底部 30px 时触发加载
    const threshold = 30;
    const isNearBottom =
      list.scrollTop + list.clientHeight >= list.scrollHeight - threshold;

    if (isNearBottom && sessionHasMore && !sessionIsLoading) {
      loadMoreSessions();
    }
  },
  true,
);

// ========== 多端同步（SSE / WebSocket 双模式） ==========

// 统一连接入口：自动探测传输方式并建立连接
async function connectSync() {
  // 清理旧连接和重连定时器
  cleanupAllSyncConnections();

  // 首次连接时从后端获取传输方式
  if (!syncTransport) {
    try {
      const resp = await fetch("/api/ai/sync/transport");
      const data = await resp.json();
      syncTransport =
        data.success && data.data && data.data.transport
          ? data.data.transport
          : "sse";
      console.log("[Sync] 传输方式:", syncTransport);
    } catch (e) {
      console.warn("[Sync] 获取传输方式失败，降级为 SSE:", e);
      syncTransport = "sse";
    }
  }

  if (syncTransport === "websocket") {
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
  if (syncTransport === "websocket") {
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
    syncEventSource = new EventSource("/api/ai/sync/events");

    syncEventSource.addEventListener("connected", function (e) {
      console.log("[SSE] 连接已建立");
    });

    syncEventSource.addEventListener("sync", function (e) {
      try {
        const event = JSON.parse(e.data);
        console.log("[SSE] 收到同步事件:", event);
        handleSyncEvent(event);
      } catch (err) {
        console.warn("[SSE] 解析事件失败:", err);
      }
    });

    syncEventSource.onerror = function () {
      console.warn("[SSE] 连接断开，将自动重连...");
      if (
        syncEventSource &&
        syncEventSource.readyState === EventSource.CLOSED
      ) {
        scheduleReconnect();
      }
    };
  } catch (error) {
    console.warn("[SSE] 创建 EventSource 失败:", error);
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
  fetch("/api/ai/sync/events", { method: "DELETE" }).catch(() => {});
}

// ---------- WebSocket 模式 ----------

// 连接 WebSocket
function connectSyncWebSocket() {
  try {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = protocol + "//" + window.location.host + "/ws/sync";
    syncWebSocket = new WebSocket(wsUrl);

    syncWebSocket.onopen = function () {
      console.log("[WebSocket] 连接已建立");
      // 启动心跳保活
      startWebSocketPing();
    };

    syncWebSocket.onmessage = function (e) {
      try {
        const msg = JSON.parse(e.data);
        console.log("[WebSocket] 收到消息:", msg);

        // 连接确认消息，无需处理
        if (msg.type === "connected") {
          console.log("[WebSocket] 服务端确认连接");
          return;
        }
        // 心跳响应，忽略
        if (msg.type === "pong") {
          return;
        }
        // 同步事件（服务端推送的 sync 消息包含 type 和 sessionId）
        handleSyncEvent(msg);
      } catch (err) {
        console.warn("[WebSocket] 解析消息失败:", err);
      }
    };

    syncWebSocket.onerror = function (error) {
      console.warn("[WebSocket] 连接出错:", error);
    };

    syncWebSocket.onclose = function (event) {
      console.warn("[WebSocket] 连接关闭 (code: " + event.code + ")");
      stopWebSocketPing();
      // 非主动关闭时尝试重连
      if (event.code !== 1000) {
        scheduleReconnect();
      }
    };
  } catch (error) {
    console.warn("[WebSocket] 创建连接失败:", error);
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
      syncWebSocket.send(JSON.stringify({ type: "ping" }));
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
    const chatPopup = getEl("chatPopup");
    if (chatPopup && chatPopup.classList.contains("show")) {
      if (syncTransport === "websocket") {
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

  if (type === "session_created" || type === "session_deleted") {
    await loadSessions();
    if (type === "session_deleted" && sessionId === currentSessionId) {
      currentSessionId = null;
      clearChatMessages();
      if (allSessions.length > 0) {
        await selectSession(allSessions[0].sessionId);
      }
    }
  } else if (type === "message_added") {
    await loadSessions();
    if (sessionId && sessionId === currentSessionId) {
      await loadSessionHistory(sessionId);
    }
  }
}

// 消息历史滚动监听 - 触顶自动加载更早消息
document.addEventListener(
  "scroll",
  function (e) {
    const messages = getEl("chatMessages");
    if (!messages || e.target !== messages) return;
    // 距离顶部 30px 时触发加载
    const threshold = 30;
    const isNearTop = messages.scrollTop <= threshold;

    if (isNearTop && historyHasMore && !historyIsLoading) {
      loadMoreHistory();
    }
  },
  true,
);

// ========== 输入框历史消息（上下键翻页） ==========
/**
 * 输入框键盘事件：上下键浏览历史消息
 */
document.addEventListener("keydown", function (e) {
  const input = e.target;
  if (!input || input.id !== "chatInput") return;

  // Ctrl+Enter 换行
  if (e.key === "Enter" && e.ctrlKey) {
    e.preventDefault();
    const start = input.selectionStart;
    const end = input.selectionEnd;
    input.value =
      input.value.substring(0, start) + "\n" + input.value.substring(end);
    input.selectionStart = input.selectionEnd = start + 1;
    return;
  }

  // Enter 发送消息（无 Shift/Ctrl 修饰键）
  if (e.key === "Enter" && !e.shiftKey && !e.ctrlKey && !e.altKey) {
    e.preventDefault();
    sendMessage();
    return;
  }

  if (e.key === "ArrowUp") {
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
  } else if (e.key === "ArrowDown") {
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
      pendingInput = "";
    }
  }
});

// ========== 消息缓存系统 ==========
const messageCache = new Map();

/**
 * 缓存消息到内存
 */
function cacheMessage(sessionId, messages) {
  if (!sessionId || !messages) return;
  messageCache.set(sessionId, {
    data: [...messages],
    timestamp: Date.now(),
  });
  // 清理过期缓存（保留最近 N 条）
  if (messageCache.size > CHAT_CONFIG.MESSAGE_CACHE_SIZE) {
    const oldestKey = messageCache.keys().next().value;
    messageCache.delete(oldestKey);
  }
}

/**
 * 从缓存获取消息
 */
function getCachedMessages(sessionId, maxAge = 5 * 60 * 1000) {
  if (!sessionId || !messageCache.has(sessionId)) return null;
  const cached = messageCache.get(sessionId);
  // 检查缓存是否过期（默认 5 分钟）
  if (Date.now() - cached.timestamp > maxAge) {
    messageCache.delete(sessionId);
    return null;
  }
  return cached.data;
}

/**
 * 清空指定会话的缓存
 */
function clearMessageCache(sessionId) {
  if (sessionId) {
    messageCache.delete(sessionId);
  } else {
    messageCache.clear();
  }
}

// 发送历史：记录用户发送过的消息（最近 N 条）
const sendHistory = [];
const SEND_HISTORY_MAX = CHAT_CONFIG.MAX_SEND_HISTORY;
// 当前浏览历史的索引（-1 表示未浏览历史，正在输入新内容）
let historyIndex = -1;
// 暂存用户正在输入但还没发送的内容（按上键前输入框的内容）
let pendingInput = "";

/**
 * 将消息加入发送历史（带防抖）
 */
function pushSendHistory(message) {
  if (!message || !message.trim()) return;
  // 去重：如果和最后一条相同，不重复添加
  if (sendHistory.length > 0 && sendHistory[sendHistory.length - 1] === message)
    return;
  sendHistory.push(message);
  // 超过上限则移除最早的
  if (sendHistory.length > SEND_HISTORY_MAX) {
    sendHistory.shift();
  }
  // 重置历史浏览索引
  historyIndex = -1;
  pendingInput = "";
}

/**
 * 快捷提问功能
 */
function quickAsk(question) {
  const input = document.getElementById("chatInput");
  if (input) {
    input.value = question;
    input.focus();
    // 自动发送
    setTimeout(() => sendMessage(), 100);
  }
}
