/**
 * AI助手管理页面 JS
 * 支持按用户级别查询会话历史
 */

const API_BASE = '/api/ai/manage';

// 当前选中状态
let selectedUserId = null;
let selectedSessionId = null;

document.addEventListener('DOMContentLoaded', () => {
  loadUsers();
});

// ===== 筛选事件 =====

function onLevelFilterChange() {
  loadUsers();
  // 清空右侧
  clearSessions();
  clearMessages();
}

function onQueryModeChange() {
  const mode = document.getElementById('query-mode').value;
  if (mode === 'level') {
    // 按级别查询模式：选择级别后直接查会话
    loadUsers(); // 仍然显示用户列表
  }
  clearSessions();
  clearMessages();
}

// ===== 加载用户列表 =====
async function loadUsers() {
  const level = document.getElementById('level-filter').value;
  const url = level ? `${API_BASE}/users?level=${level}` : `${API_BASE}/users`;

  try {
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      alert('加载用户失败: ' + result.error);
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
         onclick="selectUser('${u.userId}', '${escapeAttr(u.nickname || u.username)}')">
      <div class="user-avatar">${getAvatarEmoji(u.userLevel)}</div>
      <div class="user-info">
        <div class="user-name">${escapeHtml(u.nickname || u.username)}</div>
        <div class="user-meta">
          <span class="level-badge level-${u.userLevel}">${levelLabel(u.userLevel)}</span>
          <span>${u.sessionCount} 个会话</span>
        </div>
      </div>
    </div>
  `).join('');
}

// ===== 选择用户 → 加载会话 =====
async function selectUser(userId, userName) {
  selectedUserId = userId;
  selectedSessionId = null;
  clearMessages();

  // 更新用户列表高亮
  document.querySelectorAll('.user-item').forEach(el => el.classList.remove('active'));
  event.currentTarget.classList.add('active');

  const mode = document.getElementById('query-mode').value;
  document.getElementById('session-title').textContent = `${userName} 的会话`;

  try {
    let url;
    if (mode === 'level') {
      const level = document.getElementById('level-filter').value || 'normal';
      url = `${API_BASE}/sessions/level/${level}`;
    } else {
      url = `${API_BASE}/sessions/user/${userId}`;
    }

    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      alert('加载会话失败: ' + result.error);
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
    <div class="session-item" onclick="selectSession('${s.sessionId}', '${escapeAttr(s.title)}')">
      <div class="session-title">${escapeHtml(s.title)}</div>
      <div class="session-meta">
        <span>${s.messageCount || 0} 条消息</span>
        <span>${formatTime(s.updateTime)}</span>
      </div>
    </div>
  `).join('');
}

// ===== 选择会话 → 加载消息 =====
async function selectSession(sessionId, title) {
  selectedSessionId = sessionId;

  // 更新会话列表高亮
  document.querySelectorAll('.session-item').forEach(el => el.classList.remove('active'));
  event.currentTarget.classList.add('active');

  document.getElementById('message-title').textContent = title || '消息详情';

  try {
    const res = await fetch(`${API_BASE}/session/${sessionId}/messages`);
    const result = await res.json();
    if (!result.success) {
      alert('加载消息失败: ' + result.error);
      return;
    }
    renderMessages(result.data);
  } catch (err) {
    console.error('加载消息异常:', err);
  }
}

function renderMessages(messages) {
  const container = document.getElementById('message-list');
  document.getElementById('message-count').textContent = messages.length;

  if (!messages || messages.length === 0) {
    container.innerHTML = '<div class="empty-hint">暂无消息</div>';
    return;
  }

  container.innerHTML = messages.map(m => `
    <div class="message-item">
      <div>
        <span class="message-role role-${m.role}">${m.role === 'user' ? '用户' : 'AI'}</span>
      </div>
      <div class="message-content">${escapeHtml(m.content)}</div>
      <div class="message-time">${formatTime(m.timestamp)}</div>
      ${m.model ? `<div class="message-model">模型: ${escapeHtml(m.model)}</div>` : ''}
    </div>
  `).join('');
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

function formatTime(timeStr) {
  if (!timeStr) return '-';
  try {
    const d = new Date(timeStr);
    if (isNaN(d.getTime())) return timeStr;
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return timeStr;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function escapeAttr(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// ===== Tab 切换 =====
function switchManageTab(tab) {
  document.querySelectorAll('.manage-tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  document.querySelectorAll('.manage-tab-content').forEach(content => {
    content.classList.toggle('active', content.id === `${tab}-tab-content`);
  });
  if (tab === 'models') {
    loadModels();
  }
}

// ===== 模型管理 =====
const MODEL_API = '/api/ai/models';

async function loadModels() {
  try {
    const res = await fetch(MODEL_API);
    const result = await res.json();
    if (!result.success) {
      alert('加载模型失败: ' + result.error);
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
      <td><code>${escapeHtml(m.modelName)}</code></td>
      <td>${escapeHtml(m.displayName)}</td>
      <td>${escapeHtml(m.provider)}</td>
      <td>${escapeHtml(m.description || '-')}</td>
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
      alert('保存失败: ' + result.error);
      return;
    }
    closeModelModal();
    loadModels();
  } catch (err) {
    alert('保存异常: ' + err.message);
  }
}

async function deleteModel(id) {
  if (!confirm('确定要删除此模型吗？')) return;
  try {
    const res = await fetch(`${MODEL_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      alert('删除失败: ' + result.error);
      return;
    }
    loadModels();
  } catch (err) {
    alert('删除异常: ' + err.message);
  }
}

async function activateModel(id) {
  if (!confirm('确定要激活此模型吗？（其他模型将取消激活）')) return;
  try {
    const res = await fetch(`${MODEL_API}/activate/${id}`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      alert('激活失败: ' + result.error);
      return;
    }
    loadModels();
  } catch (err) {
    alert('激活异常: ' + err.message);
  }
}

async function toggleModel(id, enabled) {
  try {
    const res = await fetch(`${MODEL_API}/${id}/toggle?enabled=${enabled}`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      alert('操作失败: ' + result.error);
      return;
    }
    loadModels();
  } catch (err) {
    alert('操作异常: ' + err.message);
  }
}

function closeModelModal() {
  document.getElementById('model-modal').style.display = 'none';
}
