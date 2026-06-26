/**
 * 系统设置页面 JS
 */

const API_BASE = '/api/menu';

document.addEventListener('DOMContentLoaded', () => {
  loadMenus();
});

// ===== 加载菜单列表 =====
async function loadMenus() {
  try {
    const res = await fetch(`${API_BASE}/all`);
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

// ===== 新增菜单 =====
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

// ===== 编辑菜单 =====
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

// ===== 保存菜单 =====
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
    const url = isEdit ? API_BASE : API_BASE;
    const method = isEdit ? 'PUT' : 'POST';
    if (isEdit) payload.id = parseInt(id);

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
    closeModal();
    loadMenus();
  } catch (err) {
    alert('保存异常: ' + err.message);
  }
}

// ===== 删除菜单 =====
async function deleteMenu(id) {
  if (!confirm('确定要删除此菜单吗？')) return;
  try {
    const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
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

// ===== 关闭弹窗 =====
function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

// ===== 工具 =====
function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
