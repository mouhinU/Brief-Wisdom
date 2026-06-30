/**
 * AI助手管理页面 JS
 * 支持按用户级别查询会话历史
 */

const API_BASE = '/api/ai/manage';

// 当前选中状态
let selectedUserId = null;
let selectedSessionId = null;
let currentMessages = []; // 缓存当前消息列表

document.addEventListener('DOMContentLoaded', () => {
  // 动态初始化 Tab 导航（数据来源于菜单接口的 children）
  initPageTabs({
    pageUrls: ['ai-manage.html'],
    tabContainerSelector: '.manage-tabs',
    tabContentSelector: '.manage-tab-content',
    getContentId: function(child) {
      // 知识库 → knowledge-tab-content, 模型管理 → models-tab-content, 会话历史 → sessions-tab-content
      var nameMap = { '知识库': 'knowledge-tab-content', '模型管理': 'models-tab-content', '会话历史': 'sessions-tab-content' };
      return nameMap[child.name];
    },
    onTabSwitch: function(child) {
      if (child.name === '知识库') {
        initKnowledge();
      } else if (child.name === '模型管理') {
        loadManageModels();
      } else if (child.name === '会话历史') {
        loadUsers();
      }
    }
  });
});

// ===== 筛选事件 =====

function onLevelFilterChange() {
  const mode = document.getElementById('query-mode').value;
  if (mode === 'level') {
    // 按级别查询模式：选级别直接加载会话
    loadSessionsByLevel();
  } else {
    loadUsers();
    clearSessions();
  }
  clearMessages();
}

function onQueryModeChange() {
  const mode = document.getElementById('query-mode').value;
  if (mode === 'level') {
    // 按级别查询模式：选级别后直接加载会话
    loadSessionsByLevel();
  } else {
    loadUsers();
    clearSessions();
  }
  clearMessages();
}

// ===== 按级别直接加载会话 =====
async function loadSessionsByLevel() {
  const level = document.getElementById('level-filter').value;
  if (!level) {
    clearSessions();
    clearMessages();
    return;
  }
  // 同时加载用户列表（仅供参考）
  loadUsers();

  const levelLabels = { admin: '管理员', vip: '会员', normal: '普通用户' };
  document.getElementById('session-title').textContent = `${levelLabels[level] || level} 的会话`;

  const url = `${API_BASE}/sessions/level/${level}`;
  try {
    const res = await fetch(url);
    if (!res.ok) {
      showToast(`加载会话失败 (HTTP ${res.status})`, 'error');
      return;
    }
    const result = await res.json();
    if (!result.success) {
      showToast('加载会话失败: ' + (result.error || result.msg || '未知错误'), 'error');
      return;
    }
    renderSessions(result.data);
  } catch (err) {
    console.error('[loadSessionsByLevel] error:', err);
    showToast('加载会话异常: ' + err.message, 'error');
  }
}

// ===== 加载用户列表 =====
async function loadUsers() {
  const level = document.getElementById('level-filter').value;
  const url = level ? `${API_BASE}/users?level=${level}` : `${API_BASE}/users`;

  try {
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      showToast('加载用户失败: ' + result.error, 'error');
      return;
    }
    renderUsers(result.data);
  } catch (err) {
    console.error('加载用户异常:', err);
  }
}

function renderUsers(users) {
  const container = document.getElementById('user-list');
  document.getElementById('user-count').textContent = users.length;

  if (!users || users.length === 0) {
    container.innerHTML = '<div class="empty-hint">暂无用户数据</div>';
    return;
  }

  container.innerHTML = users.map(u => `
    <div class="user-item ${selectedUserId === u.userId ? 'active' : ''}"
         onclick="selectUser('${u.userId}', '${escapeAttr(u.nickname || u.username)}', this)">
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

// ===== 选择用户 → 加载会话 =====
async function selectUser(userId, userName, el) {
  selectedUserId = userId;
  selectedSessionId = null;
  clearMessages();

  // 更新用户列表高亮
  document.querySelectorAll('.user-item').forEach(e => e.classList.remove('active'));
  if (el) el.classList.add('active');

  const mode = document.getElementById('query-mode').value;
  document.getElementById('session-title').textContent = `${userName} 的会话`;

  try {
    const url = `${API_BASE}/sessions/user/${userId}`;
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      showToast('加载会话失败: ' + result.error, 'error');
      return;
    }
    renderSessions(result.data);
  } catch (err) {
    console.error('加载会话异常:', err);
  }
}

function renderSessions(sessions) {
  const container = document.getElementById('session-list');
  document.getElementById('session-count').textContent = sessions.length;

  if (!sessions || sessions.length === 0) {
    container.innerHTML = '<div class="empty-hint">暂无会话数据</div>';
    return;
  }

  container.innerHTML = sessions.map(s => `
    <div class="session-item" onclick="manageSelectSession('${s.sessionId}', '${escapeAttr(s.title)}', this)">
      <div class="session-title">${manageEscapeHtml(s.title)}</div>
      <div class="session-meta">
        <span>${s.messageCount || 0} 条消息</span>
        <span>${manageFormatTime(s.updateTime)}</span>
      </div>
    </div>
  `).join('');
}

// ===== 选择会话 → 加载消息 =====
async function manageSelectSession(sessionId, title, el) {
  selectedSessionId = sessionId;

  // 更新会话列表高亮
  document.querySelectorAll('.session-item').forEach(e => e.classList.remove('active'));
  if (el) el.classList.add('active');

  document.getElementById('message-title').textContent = title || '消息详情';

  try {
    const res = await fetch(`${API_BASE}/session/${sessionId}/messages`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载消息失败: ' + result.error, 'error');
      return;
    }
    renderMessages(result.data);
  } catch (err) {
    console.error('加载消息异常:', err);
  }
}

function renderMessages(messages) {
  currentMessages = messages || [];
  const container = document.getElementById('message-list');
  document.getElementById('message-count').textContent = currentMessages.length;

  if (!currentMessages.length) {
    container.innerHTML = '<div class="empty-hint">暂无消息</div>';
    return;
  }

  container.innerHTML = currentMessages.map((m, i) => `
    <div class="message-item message-clickable" onclick="showMessageDetail(${i})">
      <div>
        <span class="message-role role-${m.role}">${m.role === 'user' ? '用户' : 'AI'}</span>
      </div>
      <div class="message-content">${manageEscapeHtml(m.content)}</div>
      <div class="message-time">${manageFormatTime(m.timestamp)}</div>
      ${m.model ? `<div class="message-model">模型: ${manageEscapeHtml(m.model)}</div>` : ''}
    </div>
  `).join('');
}

// ===== 消息详情弹窗 =====
function showMessageDetail(index) {
  const m = currentMessages[index];
  if (!m) return;

  const roleLabel = m.role === 'user' ? '用户' : 'AI';
  document.getElementById('detail-modal-title').textContent = `${roleLabel} 消息详情`;

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

  document.getElementById('detail-modal-body').innerHTML = html;
  document.getElementById('detail-modal').style.display = 'flex';
}

/**
 * 检测内容类型并格式化渲染
 * 支持: JSON, XML/HTML, Markdown, 纯文本
 */
function detectAndFormatContent(content) {
  const trimmed = content.trim();

  // 1. 检测 JSON
  if ((trimmed.startsWith('{') && trimmed.endsWith('}')) ||
      (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
    try {
      const parsed = JSON.parse(trimmed);
      const pretty = JSON.stringify(parsed, null, 2);
      return {
        type: 'JSON',
        cssClass: 'content-code',
        html: `<pre class="detail-pre">${manageEscapeHtml(pretty)}</pre>`
      };
    } catch (_) { /* not valid JSON, fall through */ }
  }

  // 2. 检测 XML（以 < 开头且包含标签结构）
  if (trimmed.startsWith('<') && /<\/?[a-zA-Z][\s\S]*>/.test(trimmed)) {
    // 检查是否像 HTML/XML 而非 Markdown
    if (trimmed.startsWith('<?xml') || trimmed.startsWith('<!') ||
        /<\/[a-zA-Z]+>/.test(trimmed) ||
        (/<[a-zA-Z][^>]*\/>/.test(trimmed) && !trimmed.includes('```'))) {
      const pretty = formatXml(trimmed);
      return {
        type: 'XML',
        cssClass: 'content-code',
        html: `<pre class="detail-pre">${manageEscapeHtml(pretty)}</pre>`
      };
    }
  }

  // 3. 检测 Markdown（含 ```代码块、# 标题、- 列表、**粗体** 等特征）
  if (/^#{1,6}\s/m.test(trimmed) ||
      /```/.test(trimmed) ||
      /^\s*[-*+]\s/m.test(trimmed) ||
      /\*\*[^*]+\*\*/.test(trimmed) ||
      /^\s*\d+\.\s/m.test(trimmed) ||
      /\[.+\]\(.+\)/.test(trimmed)) {
    return {
      type: 'Markdown',
      cssClass: 'content-markdown',
      html: renderMarkdown(trimmed)
    };
  }

  // 4. 默认: 纯文本
  return {
    type: 'Text',
    cssClass: '',
    html: manageEscapeHtml(content)
  };
}

/**
 * 简易 XML 格式化
 */
function formatXml(xml) {
  let formatted = '';
  let indent = 0;
  // 按标签拆分
  const parts = xml.replace(/>\s*</g, '>\n<').split('\n');
  parts.forEach(part => {
    const line = part.trim();
    if (!line) return;
    // 关闭标签减少缩进
    if (/^<\//.test(line)) indent = Math.max(indent - 1, 0);
    formatted += '  '.repeat(indent) + line + '\n';
    // 开标签增加缩进（排除自闭合和关闭标签）
    if (/^<[^\/!?][^>]*[^\/]>$/.test(line) && !/^<[^>]*\/>$/.test(line)) {
      indent++;
    }
  });
  return formatted.trim();
}

/**
 * 渲染 Markdown → HTML（优先使用 marked.js，降级为简单转换）
 */
function renderMarkdown(md) {
  if (typeof marked !== 'undefined' && marked.parse) {
    // 使用 marked.js（navbar.js 已动态加载）
    return marked.parse(md);
  }
  // 降级：简易 Markdown 转换
  let html = manageEscapeHtml(md);
  // 代码块
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="detail-pre"><code>$2</code></pre>');
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="detail-inline-code">$1</code>');
  // 标题
  html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>');
  html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>');
  html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>');
  html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>');
  // 粗体/斜体
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
  // 链接
  html = html.replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank">$1</a>');
  // 无序列表
  html = html.replace(/^\s*[-*+]\s+(.+)$/gm, '<li>$1</li>');
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');
  // 换行
  html = html.replace(/\n/g, '<br>');
  return html;
}

function closeDetailModal() {
  document.getElementById('detail-modal').style.display = 'none';
}

// ===== 清空方法 =====
function clearSessions() {
  document.getElementById('session-list').innerHTML = '<div class="empty-hint">请选择用户查看会话</div>';
  document.getElementById('session-count').textContent = '0';
  document.getElementById('session-title').textContent = '会话列表';
  selectedUserId = null;
}

function clearMessages() {
  document.getElementById('message-list').innerHTML = '<div class="empty-hint">请选择会话查看消息</div>';
  document.getElementById('message-count').textContent = '0';
  document.getElementById('message-title').textContent = '消息详情';
  selectedSessionId = null;
}

// ===== 工具函数 =====
function levelLabel(level) {
  const map = { admin: '管理员', vip: '会员', normal: '普通' };
  return map[level] || level;
}

function getAvatarEmoji(level) {
  const map = { admin: '👑', vip: '⭐', normal: '👤' };
  return map[level] || '👤';
}

function manageFormatTime(timeStr) {
  if (!timeStr) return '-';
  try {
    const d = new Date(timeStr);
    if (isNaN(d.getTime())) return String(timeStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  } catch {
    return String(timeStr);
  }
}

function manageEscapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function escapeHtml(str) {
  return manageEscapeHtml(str);
}

function escapeAttr(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// ===== 模型管理 =====
const MODEL_API = '/api/ai/models';

async function loadManageModels() {
  try {
    const res = await fetch(MODEL_API);
    const result = await res.json();
    if (!result.success) {
      showToast('加载模型失败: ' + result.error, 'error');
      return;
    }
    renderModelTable(result.data);
  } catch (err) {
    console.error('加载模型异常:', err);
  }
}

function renderModelTable(models) {
  const tbody = document.getElementById('model-table-body');
  if (!tbody) return;
  if (!models || models.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty-hint">暂无模型数据</td></tr>';
    return;
  }
  tbody.innerHTML = models.map(m => `
    <tr>
      <td>${m.sortOrder ?? 0}</td>
      <td><code>${manageEscapeHtml(m.modelName)}</code></td>
      <td>${manageEscapeHtml(m.displayName)}</td>
      <td>${manageEscapeHtml(m.provider)}</td>
      <td>${manageEscapeHtml(m.description || '-')}</td>
      <td>
        <span class="status-badge ${m.isEnabled === 1 ? 'status-enabled' : 'status-disabled'}">
          ${m.isEnabled === 1 ? '启用' : '禁用'}
        </span>
        <button class="btn-sm btn-toggle" onclick="toggleModel(${m.id}, ${m.isEnabled !== 1})">
          ${m.isEnabled === 1 ? '禁用' : '启用'}
        </button>
      </td>
      <td>
        ${m.isActive === 1
          ? '<span class="status-badge status-active">当前激活</span>'
          : `<button class="btn-sm btn-activate" onclick="activateModel(${m.id})">激活</button>`
        }
      </td>
      <td>
        <button class="btn-sm btn-edit-sm" onclick='editModel(${JSON.stringify(m)})'>编辑</button>
        <button class="btn-sm btn-delete-sm" onclick="deleteModel(${m.id})">删除</button>
      </td>
    </tr>
  `).join('');
}

function showModelForm() {
  document.getElementById('model-modal-title').textContent = '新增模型';
  document.getElementById('model-id').value = '';
  document.getElementById('model-name-input').value = '';
  document.getElementById('model-display-name').value = '';
  document.getElementById('model-provider').value = 'dashscope';
  document.getElementById('model-description').value = '';
  document.getElementById('model-sort-order').value = '0';
  document.getElementById('model-modal').style.display = 'flex';
}

function editModel(model) {
  document.getElementById('model-modal-title').textContent = '编辑模型';
  document.getElementById('model-id').value = model.id;
  document.getElementById('model-name-input').value = model.modelName;
  document.getElementById('model-display-name').value = model.displayName;
  document.getElementById('model-provider').value = model.provider;
  document.getElementById('model-description').value = model.description || '';
  document.getElementById('model-sort-order').value = model.sortOrder ?? 0;
  document.getElementById('model-modal').style.display = 'flex';
}

async function saveModel(e) {
  e.preventDefault();
  const id = document.getElementById('model-id').value;
  const payload = {
    modelName: document.getElementById('model-name-input').value.trim(),
    displayName: document.getElementById('model-display-name').value.trim(),
    provider: document.getElementById('model-provider').value.trim(),
    description: document.getElementById('model-description').value.trim(),
    sortOrder: parseInt(document.getElementById('model-sort-order').value) || 0
  };

  try {
    const isEdit = !!id;
    if (isEdit) payload.id = parseInt(id);
    const url = MODEL_API;
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.error, 'error');
      return;
    }
    closeModelModal();
    loadManageModels();
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

async function deleteModel(id) {
  if (!await showConfirmDialog('确定要删除此模型吗？', '🗑️')) return;
  try {
    const res = await fetch(`${MODEL_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.error, 'error');
      return;
    }
    loadManageModels();
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

async function activateModel(id) {
  if (!await showConfirmDialog('确定要激活此模型吗？（其他模型将取消激活）', '⚡')) return;
  try {
    const res = await fetch(`${MODEL_API}/activate/${id}`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      showToast('激活失败: ' + result.error, 'error');
      return;
    }
    loadManageModels();
  } catch (err) {
    showToast('激活异常: ' + err.message, 'error');
  }
}

async function toggleModel(id, enabled) {
  try {
    const res = await fetch(`${MODEL_API}/${id}/toggle?enabled=${enabled}`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      showToast('操作失败: ' + result.error, 'error');
      return;
    }
    loadManageModels();
  } catch (err) {
    showToast('操作异常: ' + err.message, 'error');
  }
}

function closeModelModal() {
  document.getElementById('model-modal').style.display = 'none';
}
