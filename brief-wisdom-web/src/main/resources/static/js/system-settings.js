/**
 * 系统设置页面 JS
 */

const MENU_API = '/api/menu';
const USER_API = '/api/user';

// 用户管理分页状态
let userCurrentPage = 1;
let userHasMore = false;

document.addEventListener('DOMContentLoaded', () => {
  loadMenus();
});

// ===== Tab 切换 =====
function switchSettingsTab(tab) {
  document.querySelectorAll('.settings-tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  document.querySelectorAll('.settings-tab-content').forEach(content => {
    content.classList.toggle('active', content.id === `${tab}-tab-content`);
  });
  // 首次切换到用户管理时加载数据
  if (tab === 'user') {
    loadUsers();
  }
}

// ===== 菜单管理 =====

async function loadMenus() {
  try {
    const res = await fetch(`${MENU_API}/all`);
    const result = await res.json();
    if (!result.success) {
      alert('加载菜单失败: ' + result.error);
      return;
    }
    renderMenuTable(result.data);
  } catch (err) {
    console.error('加载菜单异常:', err);
  }
}

function renderMenuTable(menus) {
  const tbody = document.getElementById('menu-table-body');
  if (!menus || menus.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty-state">暂无菜单数据</td></tr>';
    return;
  }
  tbody.innerHTML = menus.map(m => `
    <tr>
      <td>${m.sortOrder ?? 0}</td>
      <td class="icon-cell">${m.icon || '-'}</td>
      <td>${escapeHtml(m.name)}</td>
      <td><code>${escapeHtml(m.url)}</code></td>
      <td>${m.target === '_blank' ? '新窗口' : '当前窗口'}</td>
      <td>
        <span class="badge ${m.isVisible === 1 ? 'badge-show' : 'badge-hide'}">
          ${m.isVisible === 1 ? '显示' : '隐藏'}
        </span>
        <button class="btn btn-toggle-vis" onclick="toggleVisible(${m.id})">
          ${m.isVisible === 1 ? '隐藏' : '显示'}
        </button>
      </td>
      <td>
        <div class="actions">
          <button class="btn btn-edit" onclick='editMenu(${JSON.stringify(m)})'>编辑</button>
          <button class="btn btn-delete" onclick="deleteMenu(${m.id})">删除</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function showMenuForm() {
  document.getElementById('modal-title').textContent = '新增菜单';
  document.getElementById('menu-id').value = '';
  document.getElementById('menu-name').value = '';
  document.getElementById('menu-url').value = '';
  document.getElementById('menu-icon').value = '';
  document.getElementById('menu-sort').value = '0';
  document.getElementById('menu-target').value = '_self';
  document.getElementById('menu-visible').value = '1';
  document.getElementById('modal').style.display = 'flex';
}

function editMenu(menu) {
  document.getElementById('modal-title').textContent = '编辑菜单';
  document.getElementById('menu-id').value = menu.id;
  document.getElementById('menu-name').value = menu.name;
  document.getElementById('menu-url').value = menu.url;
  document.getElementById('menu-icon').value = menu.icon || '';
  document.getElementById('menu-sort').value = menu.sortOrder ?? 0;
  document.getElementById('menu-target').value = menu.target;
  document.getElementById('menu-visible').value = String(menu.isVisible);
  document.getElementById('modal').style.display = 'flex';
}

async function saveMenu(e) {
  e.preventDefault();
  const id = document.getElementById('menu-id').value;
  const payload = {
    name: document.getElementById('menu-name').value.trim(),
    url: document.getElementById('menu-url').value.trim(),
    icon: document.getElementById('menu-icon').value.trim(),
    sortOrder: parseInt(document.getElementById('menu-sort').value) || 0,
    target: document.getElementById('menu-target').value,
    isVisible: parseInt(document.getElementById('menu-visible').value)
  };

  try {
    const isEdit = !!id;
    const method = isEdit ? 'PUT' : 'POST';
    if (isEdit) payload.id = parseInt(id);

    const res = await fetch(MENU_API, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      alert('保存失败: ' + result.error);
      return;
    }
    closeModal();
    loadMenus();
  } catch (err) {
    alert('保存异常: ' + err.message);
  }
}

async function deleteMenu(id) {
  if (!confirm('确定要删除此菜单吗？')) return;
  try {
    const res = await fetch(`${MENU_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      alert('删除失败: ' + result.error);
      return;
    }
    loadMenus();
  } catch (err) {
    alert('删除异常: ' + err.message);
  }
}

async function toggleVisible(id) {
  try {
    const res = await fetch(`${MENU_API}/${id}/toggle`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      alert('切换失败: ' + result.error);
      return;
    }
    loadMenus();
  } catch (err) {
    alert('切换异常: ' + err.message);
  }
}

function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

// ===== 用户管理 =====

async function loadUsers(page) {
  if (page) userCurrentPage = page;
  const level = document.getElementById('user-level-filter').value;
  const keyword = document.getElementById('user-search-input').value.trim();

  let url = `${USER_API}/list?page=${userCurrentPage}&size=20`;
  if (level) url += `&level=${level}`;
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

  try {
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      alert('加载用户失败: ' + result.error);
      return;
    }
    const data = result.data;
    userHasMore = data.hasMore;
    renderUserTable(data.records || []);
    renderUserPagination(data);
  } catch (err) {
    console.error('加载用户异常:', err);
  }
}

function renderUserTable(users) {
  const tbody = document.getElementById('user-table-body');
  if (!users || users.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty-state">暂无用户数据</td></tr>';
    return;
  }
  const levelLabels = { admin: '管理员', vip: '会员', normal: '普通用户' };
  const levelClasses = { admin: 'level-admin', vip: 'level-vip', normal: 'level-normal' };

  tbody.innerHTML = users.map(u => {
    const lvl = u.userLevel || 'normal';
    const time = u.createTime ? formatDateTime(u.createTime) : '-';
    return `
      <tr>
        <td>${escapeHtml(u.username)}</td>
        <td>${escapeHtml(u.nickname) || '-'}</td>
        <td><span class="level-badge ${levelClasses[lvl] || ''}">${levelLabels[lvl] || lvl}</span></td>
        <td>${time}</td>
        <td>
          <div class="actions">
            <button class="btn btn-edit" onclick="showUserLevelModal(${u.id}, '${lvl}')">改级别</button>
            <button class="btn btn-reset-pwd" onclick="resetUserPassword(${u.id}, '${escapeHtml(u.username)}')">重置密码</button>
            <button class="btn btn-delete" onclick="deleteUser(${u.id}, '${escapeHtml(u.username)}')">删除</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderUserPagination(data) {
  const container = document.getElementById('user-pagination');
  if (!data || data.pages <= 1) {
    container.innerHTML = '';
    return;
  }
  let html = '';
  const current = data.page;
  const total = data.pages;

  if (current > 1) {
    html += `<button class="page-btn" onclick="loadUsers(${current - 1})">上一页</button>`;
  }
  for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
    html += `<button class="page-btn ${i === current ? 'active' : ''}" onclick="loadUsers(${i})">${i}</button>`;
  }
  if (current < total) {
    html += `<button class="page-btn" onclick="loadUsers(${current + 1})">下一页</button>`;
  }
  html += `<span class="page-info">共 ${data.total} 条</span>`;
  container.innerHTML = html;
}

function showUserLevelModal(id, currentLevel) {
  document.getElementById('user-level-id').value = id;
  document.getElementById('user-level-select').value = currentLevel;
  document.getElementById('user-level-modal').style.display = 'flex';
}

function closeUserLevelModal() {
  document.getElementById('user-level-modal').style.display = 'none';
}

async function saveUserLevel(e) {
  e.preventDefault();
  const id = document.getElementById('user-level-id').value;
  const level = document.getElementById('user-level-select').value;

  try {
    const res = await fetch(`${USER_API}/${id}/level`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ level })
    });
    const result = await res.json();
    if (!result.success) {
      alert('修改失败: ' + result.error);
      return;
    }
    closeUserLevelModal();
    loadUsers();
  } catch (err) {
    alert('修改异常: ' + err.message);
  }
}

async function resetUserPassword(id, username) {
  if (!confirm(`确定要重置用户 "${username}" 的密码吗？`)) return;
  try {
    const res = await fetch(`${USER_API}/${id}/reset-password`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      alert('重置失败: ' + result.error);
      return;
    }
    alert('密码已重置');
  } catch (err) {
    alert('重置异常: ' + err.message);
  }
}

async function deleteUser(id, username) {
  if (!confirm(`确定要删除用户 "${username}" 吗？`)) return;
  try {
    const res = await fetch(`${USER_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      alert('删除失败: ' + result.error);
      return;
    }
    loadUsers();
  } catch (err) {
    alert('删除异常: ' + err.message);
  }
}

// ===== 工具 =====

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function formatDateTime(timeInput) {
  if (!timeInput) return '-';
  let date;
  if (typeof timeInput === 'string') {
    date = new Date(timeInput);
  } else if (Array.isArray(timeInput) && timeInput.length >= 5) {
    const [year, month, day, hour, minute] = timeInput;
    date = new Date(year, month - 1, day, hour, minute);
  } else {
    return '-';
  }
  if (isNaN(date.getTime())) return '-';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
}
