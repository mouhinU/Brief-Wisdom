/**
 * 系统设置页面 JS
 */

const MENU_API = '/api/menu';
const USER_API = '/api/user';
const ROLE_API = '/api/role';

// 用户管理分页状态
let userCurrentPage = 1;
let userHasMore = false;

document.addEventListener('DOMContentLoaded', () => {
  // 动态初始化 Tab 导航（数据来源于菜单接口的 children）
  initPageTabs({
    pageUrls: ['system-settings.html'],
    tabContainerSelector: '.settings-tabs',
    tabContentSelector: '.settings-tab-content',
    getContentId: function(child) {
      // 菜单管理 → menu-tab-content, 用户管理 → user-tab-content, 角色管理 → role-tab-content
      var nameMap = { '菜单管理': 'menu-tab-content', '用户管理': 'user-tab-content', '角色管理': 'role-tab-content' };
      return nameMap[child.name];
    },
    onTabSwitch: function(child) {
      if (child.name === '菜单管理') {
        loadMenus();
      } else if (child.name === '用户管理') {
        loadUsers();
      } else if (child.name === '角色管理') {
        loadRoles();
      }
    }
  });
  // 等待权限状态初始化完成后应用权限控制
  setTimeout(() => {
    applyPermissionsToUI();
  }, 1000);
});

/**
 * 根据用户权限控制按钮显示/隐藏
 */
function applyPermissionsToUI() {
  // 菜单管理权限
  if (!hasPermission('menu:manage')) {
    // 隐藏“新增菜单”按钮
    document.querySelectorAll('#menu-tab-content .btn-primary').forEach(btn => btn.style.display = 'none');
    // 隐藏表格中的编辑/删除/切换显示按钮
    document.querySelectorAll('#menu-tab-content .btn-edit, #menu-tab-content .btn-delete, #menu-tab-content .btn-toggle-vis').forEach(btn => btn.style.display = 'none');
  }

  // 用户管理权限
  if (!hasPermission('user:manage')) {
    // 隐藏表格中的操作按钮
    document.querySelectorAll('#user-tab-content .btn-edit, #user-tab-content .btn-role, #user-tab-content .btn-reset-pwd, #user-tab-content .btn-delete').forEach(btn => btn.style.display = 'none');
  }

  // 角色管理权限
  if (!hasPermission('role:manage')) {
    // 隐藏“新增角色”按钮
    document.querySelectorAll('#role-tab-content .btn-primary').forEach(btn => btn.style.display = 'none');
    // 隐藏表格中的操作按钮
    document.querySelectorAll('#role-tab-content .btn-edit, #role-tab-content .btn-role, #role-tab-content .btn-delete').forEach(btn => btn.style.display = 'none');
  }
}

// ===== 菜单管理 =====

// 缓存菜单树数据，用于父级菜单选择器
let allMenuTreeCache = [];

async function loadMenus() {
  try {
    const res = await fetch(`${MENU_API}/all`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载菜单失败: ' + result.error, 'error');
      return;
    }
    renderMenuTable(result.data);
  } catch (err) {
    console.error('加载菜单异常:', err);
  }
}

// 加载菜单树用于父级菜单选择器
async function loadMenuTreeForSelector(selectedParentId) {
  try {
    const res = await fetch(`${MENU_API}/all/tree`);
    const result = await res.json();
    if (!result.success) return;
    allMenuTreeCache = result.data || [];
    renderParentSelect(allMenuTreeCache, selectedParentId);
  } catch (err) {
    console.error('加载菜单树异常:', err);
  }
}

function renderParentSelect(nodes, selectedId, container) {
  const select = container || document.getElementById('menu-parent');
  if (!container) {
    select.innerHTML = '<option value="0">顶级菜单</option>';
  }
  if (!nodes || nodes.length === 0) return;
  nodes.forEach(node => {
    const opt = document.createElement('option');
    opt.value = node.id;
    const typeLabel = node.type === 0 ? '[目录]' : node.type === 2 ? '[按钮]' : '[菜单]';
    opt.textContent = `${typeLabel} ${node.icon || ''} ${node.name}`.trim();
    if (selectedId && String(node.id) === String(selectedId)) opt.selected = true;
    if (!container) select.appendChild(opt);
    else container.appendChild(opt);

    if (node.children && node.children.length > 0) {
      renderParentSelect(node.children, selectedId, select);
    }
  });
}

const typeLabels = { 0: '目录', 1: '菜单', 2: '按钮' };
const typeBadgeClass = { 0: 'type-dir', 1: 'type-menu', 2: 'type-btn' };

function renderMenuTable(menus) {
  const tbody = document.getElementById('menu-table-body');
  if (!menus || menus.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty-state">暂无菜单数据</td></tr>';
    return;
  }
  // 构建 id -> name 映射
  const nameMap = {};
  menus.forEach(m => { nameMap[m.id] = m.name; });

  tbody.innerHTML = menus.map(m => {
    const type = m.type ?? 1;
    const parentName = m.parentId && m.parentId !== 0 ? (nameMap[m.parentId] || '顶级') : '顶级';
    const canManage = hasPermission('menu:manage');
    const toggleVisBtn = canManage ? `<button class="btn btn-toggle-vis" onclick="toggleVisible(${m.id})">${m.isVisible === 1 ? '隐藏' : '显示'}</button>` : '';
    const editBtn = canManage ? `<button class="btn btn-edit" onclick='editMenu(${JSON.stringify(m)})'>\u7F16\u8F91</button>` : '';
    const deleteBtn = canManage ? `<button class="btn btn-delete" onclick="deleteMenu(${m.id})">\u5220\u9664</button>` : '';
    return `
    <tr>
      <td>${m.sortOrder ?? 0}</td>
      <td class="icon-cell">${m.icon || '-'}</td>
      <td>${escapeHtml(m.name)}</td>
      <td><span class="type-badge ${typeBadgeClass[type] || ''}">${typeLabels[type] || '\u83DC\u5355'}</span></td>
      <td>${parentName}</td>
      <td><code>${escapeHtml(m.url) || '-'}</code></td>
      <td>
        <span class="badge ${m.isVisible === 1 ? 'badge-show' : 'badge-hide'}">
          ${m.isVisible === 1 ? '\u663E\u793A' : '\u9690\u85CF'}
        </span>
        ${toggleVisBtn}
      </td>
      <td>
        <div class="actions">
          ${editBtn}
          ${deleteBtn}
        </div>
      </td>
    </tr>
  `;
  }).join('');
}

async function showMenuForm() {
  document.getElementById('modal-title').textContent = '新增菜单';
  document.getElementById('menu-id').value = '';
  document.getElementById('menu-type').value = '1';
  document.getElementById('menu-name').value = '';
  document.getElementById('menu-url').value = '';
  document.getElementById('menu-icon').value = '';
  document.getElementById('menu-sort').value = '0';
  document.getElementById('menu-target').value = '_self';
  document.getElementById('menu-visible').value = '1';
  document.getElementById('menu-require-login').value = '0';
  document.getElementById('menu-permission').value = '';
  await loadMenuTreeForSelector(0);
  onMenuTypeChange();
  document.getElementById('modal').style.display = 'flex';
}

function editMenu(menu) {
  document.getElementById('modal-title').textContent = '编辑菜单';
  document.getElementById('menu-id').value = menu.id;
  document.getElementById('menu-type').value = String(menu.type ?? 1);
  document.getElementById('menu-name').value = menu.name;
  document.getElementById('menu-url').value = menu.url || '';
  document.getElementById('menu-icon').value = menu.icon || '';
  document.getElementById('menu-sort').value = menu.sortOrder ?? 0;
  document.getElementById('menu-target').value = menu.target || '_self';
  document.getElementById('menu-visible').value = String(menu.isVisible);
  document.getElementById('menu-require-login').value = String(menu.requireLogin ?? 0);
  document.getElementById('menu-permission').value = menu.permission || '';
  loadMenuTreeForSelector(menu.parentId || 0).then(() => {
    onMenuTypeChange();
    document.getElementById('modal').style.display = 'flex';
  });
}

// 根据菜单类型显示/隐藏相关字段
function onMenuTypeChange() {
  const type = parseInt(document.getElementById('menu-type').value);
  const urlGroup = document.getElementById('menu-url-group');
  const targetGroup = document.getElementById('menu-target-group');
  const permGroup = document.getElementById('menu-perm-group');

  if (type === 0) {
    // 目录：不需要链接和权限标识
    urlGroup.style.display = 'none';
    targetGroup.style.display = 'none';
    permGroup.style.display = 'none';
  } else if (type === 1) {
    // 菜单：需要链接和打开方式
    urlGroup.style.display = '';
    targetGroup.style.display = '';
    permGroup.style.display = 'none';
  } else {
    // 按钮：不需要链接，需要权限标识
    urlGroup.style.display = 'none';
    targetGroup.style.display = 'none';
    permGroup.style.display = '';
  }
}

async function saveMenu(e) {
  e.preventDefault();
  const id = document.getElementById('menu-id').value;
  const type = parseInt(document.getElementById('menu-type').value);
  const payload = {
    parentId: parseInt(document.getElementById('menu-parent').value) || 0,
    type: type,
    name: document.getElementById('menu-name').value.trim(),
    url: type === 1 ? document.getElementById('menu-url').value.trim() : null,
    icon: document.getElementById('menu-icon').value.trim(),
    sortOrder: parseInt(document.getElementById('menu-sort').value) || 0,
    target: type === 1 ? document.getElementById('menu-target').value : '_self',
    isVisible: parseInt(document.getElementById('menu-visible').value),
    requireLogin: parseInt(document.getElementById('menu-require-login').value),
    permission: type === 2 ? document.getElementById('menu-permission').value.trim() : null
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
      showToast('保存失败: ' + result.error, 'error');
      return;
    }
    closeModal();
    loadMenus();
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

async function deleteMenu(id) {
  if (!await showConfirmDialog('确定要删除此菜单吗？', '🗑️')) return;
  try {
    const res = await fetch(`${MENU_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.error, 'error');
      return;
    }
    loadMenus();
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

async function toggleVisible(id) {
  try {
    const res = await fetch(`${MENU_API}/${id}/toggle`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      showToast('切换失败: ' + result.error, 'error');
      return;
    }
    loadMenus();
  } catch (err) {
    showToast('切换异常: ' + err.message, 'error');
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
      showToast('加载用户失败: ' + result.error, 'error');
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
    const canManage = hasPermission('user:manage');
    const actionBtns = canManage ? `
      <button class="btn btn-edit" onclick="showUserLevelModal(${u.id}, '${lvl}')">\u6539\u7EA7\u522B</button>
      <button class="btn btn-role" onclick="showUserRoleModal('${escapeHtml(u.userId)}', '${escapeHtml(u.username)}')">\u5206\u914D\u89D2\u8272</button>
      <button class="btn btn-reset-pwd" onclick="resetUserPassword(${u.id}, '${escapeHtml(u.username)}')">\u91CD\u7F6E\u5BC6\u7801</button>
      <button class="btn btn-delete" onclick="deleteUser(${u.id}, '${escapeHtml(u.username)}')">\u5220\u9664</button>
    ` : '<span class="no-perm-hint">\u65E0\u64CD\u4F5C\u6743\u9650</span>';
    return `
      <tr>
        <td>${escapeHtml(u.username)}</td>
        <td>${escapeHtml(u.nickname) || '-'}</td>
        <td><span class="level-badge ${levelClasses[lvl] || ''}">${levelLabels[lvl] || lvl}</span></td>
        <td>${time}</td>
        <td>
          <div class="actions">
            ${actionBtns}
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
      showToast('修改失败: ' + result.error, 'error');
      return;
    }
    closeUserLevelModal();
    loadUsers();
  } catch (err) {
    showToast('修改异常: ' + err.message, 'error');
  }
}

async function resetUserPassword(id, username) {
  if (!await showConfirmDialog(`确定要重置用户 "${username}" 的密码吗？`, '🔑')) return;
  try {
    const res = await fetch(`${USER_API}/${id}/reset-password`, { method: 'PUT' });
    const result = await res.json();
    if (!result.success) {
      showToast('重置失败: ' + result.error, 'error');
      return;
    }
    showToast('密码已重置', 'success');
  } catch (err) {
    showToast('重置异常: ' + err.message, 'error');
  }
}

async function deleteUser(id, username) {
  if (!await showConfirmDialog(`确定要删除用户 "${username}" 吗？`, '🗑️')) return;
  try {
    const res = await fetch(`${USER_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.error, 'error');
      return;
    }
    loadUsers();
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

// ===== 角色管理 =====

async function loadRoles() {
  try {
    const res = await fetch(`${ROLE_API}/list`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载角色失败: ' + result.error, 'error');
      return;
    }
    renderRoleTable(result.data);
  } catch (err) {
    console.error('加载角色异常:', err);
  }
}

function renderRoleTable(roles) {
  const tbody = document.getElementById('role-table-body');
  if (!roles || roles.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-state">暂无角色数据</td></tr>';
    return;
  }
  tbody.innerHTML = roles.map(r => {
    const statusBadge = r.status === 1
      ? '<span class="badge badge-show">启用</span>'
      : '<span class="badge badge-hide">禁用</span>';
    const time = r.createTime ? formatDateTime(r.createTime) : '-';
    const canManage = hasPermission('role:manage');
    const actionBtns = canManage ? `
      <button class="btn btn-edit" onclick='editRole(${JSON.stringify(r)})'>\u7F16\u8F91</button>
      <button class="btn btn-role" onclick="showMenuPermModal(${r.id}, '${escapeHtml(r.roleName)}')">\u83DC\u5355\u6743\u9650</button>
      <button class="btn btn-delete" onclick="deleteRole(${r.id}, '${escapeHtml(r.roleName)}')">\u5220\u9664</button>
    ` : '<span class="no-perm-hint">\u65E0\u64CD\u4F5C\u6743\u9650</span>';
    return `
      <tr>
        <td><strong>${escapeHtml(r.roleName)}</strong></td>
        <td><code>${escapeHtml(r.roleKey)}</code></td>
        <td>${escapeHtml(r.description) || '-'}</td>
        <td>${statusBadge}</td>
        <td>${time}</td>
        <td>
          <div class="actions">
            ${actionBtns}
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function showRoleForm() {
  document.getElementById('role-modal-title').textContent = '新增角色';
  document.getElementById('role-id').value = '';
  document.getElementById('role-name').value = '';
  document.getElementById('role-key').value = '';
  document.getElementById('role-desc').value = '';
  document.getElementById('role-status').value = '1';
  document.getElementById('role-modal').style.display = 'flex';
}

function editRole(role) {
  document.getElementById('role-modal-title').textContent = '编辑角色';
  document.getElementById('role-id').value = role.id;
  document.getElementById('role-name').value = role.roleName;
  document.getElementById('role-key').value = role.roleKey;
  document.getElementById('role-desc').value = role.description || '';
  document.getElementById('role-status').value = String(role.status);
  document.getElementById('role-modal').style.display = 'flex';
}

function closeRoleModal() {
  document.getElementById('role-modal').style.display = 'none';
}

async function saveRole(e) {
  e.preventDefault();
  const id = document.getElementById('role-id').value;
  const payload = {
    roleName: document.getElementById('role-name').value.trim(),
    roleKey: document.getElementById('role-key').value.trim(),
    description: document.getElementById('role-desc').value.trim(),
    status: parseInt(document.getElementById('role-status').value)
  };

  try {
    const isEdit = !!id;
    const method = isEdit ? 'PUT' : 'POST';
    if (isEdit) payload.id = parseInt(id);

    const res = await fetch(ROLE_API, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.error, 'error');
      return;
    }
    closeRoleModal();
    loadRoles();
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

async function deleteRole(id, name) {
  if (!await showConfirmDialog(`确定要删除角色"${name}"吗？`, '🗑️')) return;
  try {
    const res = await fetch(`${ROLE_API}/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.error, 'error');
      return;
    }
    loadRoles();
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

// ===== 角色菜单权限分配 =====

let currentPermRoleId = null;

async function showMenuPermModal(roleId, roleName) {
  currentPermRoleId = roleId;
  document.getElementById('menu-perm-title').textContent = `分配菜单权限 - ${roleName}`;

  // 并行加载菜单树和角色已选菜单
  const [treeRes, menuIdsRes] = await Promise.all([
    fetch(`${MENU_API}/all/tree`),
    fetch(`${ROLE_API}/${roleId}/menus`)
  ]);
  const treeResult = await treeRes.json();
  const menuIdsResult = await menuIdsRes.json();

  if (!treeResult.success || !menuIdsResult.success) {
    showToast('加载菜单数据失败', 'error');
    return;
  }

  const checkedIds = new Set(menuIdsResult.data);
  renderMenuPermTree(treeResult.data, checkedIds);
  document.getElementById('menu-perm-modal').style.display = 'flex';
}

function renderMenuPermTree(nodes, checkedIds, container) {
  const root = container || document.getElementById('menu-perm-tree');
  root.innerHTML = '';
  renderMenuPermNodes(nodes, checkedIds, root, 0);
}

function renderMenuPermNodes(nodes, checkedIds, container, depth) {
  if (!nodes || nodes.length === 0) return;
  nodes.forEach(node => {
    const indent = depth * 24;
    const checked = checkedIds.has(node.id) ? 'checked' : '';
    const hasChildren = node.children && node.children.length > 0;
    const typeLabel = node.type === 0 ? '[目录]' : node.type === 2 ? '[按钮]' : '';

    const item = document.createElement('div');
    item.className = 'menu-perm-item';
    item.style.paddingLeft = indent + 'px';
    item.innerHTML = `
      <label class="menu-perm-label">
        <input type="checkbox" class="menu-perm-cb" value="${node.id}" ${checked}>
        <span class="menu-perm-icon">${node.icon || ''}</span>
        <span class="menu-perm-name">${escapeHtml(node.name)}</span>
        ${typeLabel ? `<span class="menu-perm-type">${typeLabel}</span>` : ''}
      </label>
    `;
    container.appendChild(item);

    if (hasChildren) {
      renderMenuPermNodes(node.children, checkedIds, container, depth + 1);
    }
  });
}

function checkAllMenus(checked) {
  document.querySelectorAll('.menu-perm-cb').forEach(cb => {
    cb.checked = checked;
  });
}

function closeMenuPermModal() {
  document.getElementById('menu-perm-modal').style.display = 'none';
  currentPermRoleId = null;
}

async function saveMenuPermissions() {
  if (!currentPermRoleId) return;
  const checkedIds = Array.from(
    document.querySelectorAll('.menu-perm-cb:checked')
  ).map(cb => parseInt(cb.value));

  try {
    const res = await fetch(`${ROLE_API}/${currentPermRoleId}/menus`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(checkedIds)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.error, 'error');
      return;
    }
    closeMenuPermModal();
    showToast('菜单权限已保存', 'success');
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

// ===== 用户角色分配 =====

let currentRoleUserId = null;

async function showUserRoleModal(userId, username) {
  currentRoleUserId = userId;
  document.getElementById('user-role-title').textContent = `分配角色 - ${username}`;

  // 并行加载所有角色和用户已有角色
  const [rolesRes, userRolesRes] = await Promise.all([
    fetch(`${ROLE_API}/enabled`),
    fetch(`${ROLE_API}/user/${userId}`)
  ]);
  const rolesResult = await rolesRes.json();
  const userRolesResult = await userRolesRes.json();

  if (!rolesResult.success || !userRolesResult.success) {
    showToast('加载角色数据失败', 'error');
    return;
  }

  const userRoleIds = new Set(userRolesResult.data.map(r => r.id));
  const container = document.getElementById('user-role-checkboxes');
  container.innerHTML = rolesResult.data.map(r => `
    <label class="role-checkbox-item">
      <input type="checkbox" value="${r.id}" ${userRoleIds.has(r.id) ? 'checked' : ''}>
      <span class="role-cb-name">${escapeHtml(r.roleName)}</span>
      <span class="role-cb-key">(${escapeHtml(r.roleKey)})</span>
    </label>
  `).join('');

  document.getElementById('user-role-modal').style.display = 'flex';
}

function closeUserRoleModal() {
  document.getElementById('user-role-modal').style.display = 'none';
  currentRoleUserId = null;
}

async function saveUserRoles() {
  if (!currentRoleUserId) return;
  const checkedIds = Array.from(
    document.querySelectorAll('#user-role-checkboxes input:checked')
  ).map(cb => parseInt(cb.value));

  try {
    const res = await fetch(`${ROLE_API}/assign/${currentRoleUserId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(checkedIds)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.error, 'error');
      return;
    }
    closeUserRoleModal();
    showToast('用户角色已保存', 'success');
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
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
