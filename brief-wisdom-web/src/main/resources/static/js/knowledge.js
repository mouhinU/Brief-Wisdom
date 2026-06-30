/**
 * 知识库管理页面 JS
 */

const API_BASE = '/api/knowledge';

// 当前状态
let currentBaseId = null;
let currentDocType = '';
let currentPage = 1;
let totalPages = 0;
let allBases = [];

document.addEventListener('DOMContentLoaded', () => {
  loadBases();
});

// ==================== 知识库 ====================

async function loadBases() {
  try {
    const res = await fetch(`${API_BASE}/bases`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载知识库失败: ' + result.msg, 'error');
      return;
    }
    allBases = result.data || [];
    renderBaseList();
    updateParentOptions();
  } catch (err) {
    console.error('加载知识库异常:', err);
    showToast('加载知识库异常', 'error');
  }
}

function renderBaseList() {
  const container = document.getElementById('base-list');
  if (!allBases || allBases.length === 0) {
    container.innerHTML = '<div class="empty-hint">暂无知识库，点击右上角创建</div>';
    return;
  }

  // 构建树形结构
  const tree = buildTree(allBases, 0);
  container.innerHTML = renderTreeNodes(tree);
}

function buildTree(items, parentId) {
  const children = items.filter(b => b.parentId === parentId);
  return children.map(b => ({
    ...b,
    children: buildTree(items, b.id)
  }));
}

function renderTreeNodes(nodes, level = 0) {
  return nodes.map(base => {
    const indent = level > 0 ? `padding-left: ${18 + level * 16}px;` : '';
    const isActive = base.id === currentBaseId ? 'active' : '';
    const hasChildren = base.children && base.children.length > 0;

    let html = `<div class="base-item ${isActive}" style="${indent}" onclick="selectBase(${base.id}, '${escapeAttr(base.name)}', this)">
      <div class="base-item-header">
        <span class="base-icon">${base.icon || '📚'}</span>
        <span class="base-name">${escapeHtml(base.name)}</span>
        <span class="base-doc-count">${base.documentCount || 0}</span>
      </div>
      ${base.description ? `<div class="base-desc">${escapeHtml(base.description)}</div>` : ''}
      <div class="base-actions">
        <button class="base-action-btn" onclick="event.stopPropagation();editBase(${base.id})">编辑</button>
        <button class="base-action-btn delete" onclick="event.stopPropagation();deleteBase(${base.id})">删除</button>
      </div>
    </div>`;

    if (hasChildren) {
      html += renderTreeNodes(base.children, level + 1);
    }
    return html;
  }).join('');
}

async function selectBase(baseId, baseName, el) {
  currentBaseId = baseId;
  currentPage = 1;
  currentDocType = '';

  // 更新高亮
  document.querySelectorAll('.base-item').forEach(e => e.classList.remove('active'));
  if (el) el.classList.add('active');

  // 更新标题
  document.getElementById('current-base-name').textContent = baseName || '知识库';
  document.getElementById('add-doc-btn').style.display = '';

  // 重置类型筛选
  document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  document.querySelector('.filter-btn[data-type=""]').classList.add('active');

  await loadDocuments();
}

function showBaseForm(parentId) {
  document.getElementById('base-modal-title').textContent = '新建知识库';
  document.getElementById('base-id').value = '';
  document.getElementById('base-name').value = '';
  document.getElementById('base-description').value = '';
  document.getElementById('base-icon').value = '📚';
  document.getElementById('base-sort-order').value = '0';
  document.getElementById('base-is-public').value = '0';
  document.getElementById('base-parent-id').value = parentId || '0';
  document.getElementById('base-modal').style.display = 'flex';
}

function editBase(id) {
  const base = allBases.find(b => b.id === id);
  if (!base) return;

  document.getElementById('base-modal-title').textContent = '编辑知识库';
  document.getElementById('base-id').value = base.id;
  document.getElementById('base-name').value = base.name || '';
  document.getElementById('base-description').value = base.description || '';
  document.getElementById('base-icon').value = base.icon || '📚';
  document.getElementById('base-sort-order').value = base.sortOrder || 0;
  document.getElementById('base-is-public').value = base.isPublic || 0;
  document.getElementById('base-parent-id').value = base.parentId || 0;
  document.getElementById('base-modal').style.display = 'flex';
}

async function saveBase(e) {
  e.preventDefault();
  const id = document.getElementById('base-id').value;
  const payload = {
    name: document.getElementById('base-name').value.trim(),
    description: document.getElementById('base-description').value.trim(),
    icon: document.getElementById('base-icon').value.trim(),
    parentId: parseInt(document.getElementById('base-parent-id').value) || 0,
    sortOrder: parseInt(document.getElementById('base-sort-order').value) || 0,
    isPublic: parseInt(document.getElementById('base-is-public').value)
  };

  try {
    const isEdit = !!id;
    const url = isEdit ? `${API_BASE}/bases/${id}` : `${API_BASE}/bases`;
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.msg, 'error');
      return;
    }
    closeBaseModal();
    loadBases();
    showToast(isEdit ? '知识库更新成功' : '知识库创建成功');
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

async function deleteBase(id) {
  if (!await showConfirmDialog('确定要删除此知识库吗？其下所有文档也会被删除。', '🗑️')) return;
  try {
    const res = await fetch(`${API_BASE}/bases/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.msg, 'error');
      return;
    }
    if (currentBaseId === id) {
      currentBaseId = null;
      document.getElementById('current-base-name').textContent = '请选择知识库';
      document.getElementById('document-list').innerHTML = '<div class="empty-hint">请从左侧选择一个知识库</div>';
      document.getElementById('add-doc-btn').style.display = 'none';
    }
    loadBases();
    showToast('删除成功');
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

function closeBaseModal() {
  document.getElementById('base-modal').style.display = 'none';
}

function updateParentOptions() {
  const select = document.getElementById('base-parent-id');
  const current = select.value;
  select.innerHTML = '<option value="0">无（顶级知识库）</option>';
  allBases.forEach(b => {
    select.innerHTML += `<option value="${b.id}">${b.icon || '📚'} ${escapeHtml(b.name)}</option>`;
  });
  select.value = current || '0';
}

// ==================== 文档 ====================

async function loadDocuments() {
  if (!currentBaseId) return;

  const url = `${API_BASE}/bases/${currentBaseId}/documents?page=${currentPage}&size=20${currentDocType ? '&docType=' + currentDocType : ''}`;
  try {
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      showToast('加载文档失败: ' + result.msg, 'error');
      return;
    }
    const data = result.data;
    renderDocuments(data.records || []);
    renderPagination(data);
  } catch (err) {
    showToast('加载文档异常: ' + err.message, 'error');
  }
}

function renderDocuments(docs) {
  const container = document.getElementById('document-list');
  document.getElementById('doc-count').textContent = docs.length;

  if (!docs || docs.length === 0) {
    container.innerHTML = '<div class="empty-hint">暂无文档，点击右上角创建</div>';
    return;
  }

  container.innerHTML = docs.map(doc => {
    const typeIcon = doc.docType === 'INTERNAL' ? '📝' : doc.docType === 'FILE' ? '📎' : '🔗';
    const typeClass = doc.docType.toLowerCase();
    const statusClass = doc.status === 1 ? 'published' : doc.status === 0 ? 'draft' : 'archived';
    const statusText = doc.status === 1 ? '已发布' : doc.status === 0 ? '草稿' : '已归档';
    const tags = doc.tags ? doc.tags.split(',').filter(t => t.trim()).map(t => `<span class="doc-tag">${escapeHtml(t.trim())}</span>`).join('') : '';

    return `<div class="doc-item" onclick="viewDocument(${doc.id})">
      <div class="doc-type-icon ${typeClass}">${typeIcon}</div>
      <div class="doc-info">
        <div class="doc-title-text">${escapeHtml(doc.title)}</div>
        <div class="doc-meta">
          <span>${doc.getDocTypeDisplay ? doc.getDocTypeDisplay() : typeIcon}</span>
          <span>${formatTime(doc.updateTime)}</span>
          <span>👁 ${doc.viewCount || 0}</span>
          ${tags ? `<span class="doc-tags">${tags}</span>` : ''}
        </div>
      </div>
      <span class="doc-status ${statusClass}">${statusText}</span>
      <div class="doc-actions">
        <button class="doc-action-btn" onclick="event.stopPropagation();editDocument(${doc.id})">编辑</button>
        <button class="doc-action-btn delete" onclick="event.stopPropagation();deleteDocument(${doc.id})">删除</button>
      </div>
    </div>`;
  }).join('');
}

function renderPagination(data) {
  const container = document.getElementById('pagination');
  totalPages = data.pages || 0;

  if (totalPages <= 1) {
    container.style.display = 'none';
    return;
  }

  container.style.display = 'flex';
  let html = '';
  html += `<button class="page-btn" onclick="goToPage(${currentPage - 1})" ${currentPage <= 1 ? 'disabled' : ''}>上一页</button>`;
  html += `<span class="page-info">第 ${currentPage} / ${totalPages} 页，共 ${data.total} 条</span>`;
  html += `<button class="page-btn" onclick="goToPage(${currentPage + 1})" ${currentPage >= totalPages ? 'disabled' : ''}>下一页</button>`;
  container.innerHTML = html;
}

function goToPage(page) {
  currentPage = page;
  loadDocuments();
}

function filterByType(type) {
  currentDocType = type;
  currentPage = 1;
  document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  document.querySelector(`.filter-btn[data-type="${type}"]`).classList.add('active');
  loadDocuments();
}

function handleSearch(event) {
  if (event.key === 'Enter') {
    const keyword = document.getElementById('search-input').value.trim();
    if (keyword) {
      searchDocuments(keyword);
    } else {
      loadDocuments();
    }
  }
}

async function searchDocuments(keyword) {
  const url = `${API_BASE}/documents/search?keyword=${encodeURIComponent(keyword)}&page=${currentPage}&size=20`;
  try {
    const res = await fetch(url);
    const result = await res.json();
    if (!result.success) {
      showToast('搜索失败: ' + result.msg, 'error');
      return;
    }
    document.getElementById('current-base-name').textContent = `搜索: ${keyword}`;
    renderDocuments(result.data.records || []);
    renderPagination(result.data);
  } catch (err) {
    showToast('搜索异常: ' + err.message, 'error');
  }
}

// ===== 文档表单 =====

function showDocForm() {
  if (!currentBaseId) {
    showToast('请先选择一个知识库', 'error');
    return;
  }
  document.getElementById('doc-modal-title').textContent = '新建文档';
  document.getElementById('doc-id').value = '';
  document.getElementById('doc-title').value = '';
  document.getElementById('doc-content').value = '';
  document.getElementById('doc-file-url').value = '';
  document.getElementById('doc-file-name').value = '';
  document.getElementById('doc-file-size').value = '';
  document.getElementById('doc-file-type').value = '';
  document.getElementById('doc-link-url').value = '';
  document.getElementById('doc-link-desc').value = '';
  document.getElementById('doc-tags').value = '';
  document.getElementById('doc-status').value = '1';
  document.querySelector('input[name="docType"][value="INTERNAL"]').checked = true;
  onDocTypeChange();
  document.getElementById('doc-modal').style.display = 'flex';
}

async function editDocument(id) {
  try {
    const res = await fetch(`${API_BASE}/documents/${id}`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载文档失败: ' + result.msg, 'error');
      return;
    }
    const doc = result.data;
    document.getElementById('doc-modal-title').textContent = '编辑文档';
    document.getElementById('doc-id').value = doc.id;
    document.getElementById('doc-title').value = doc.title || '';
    document.querySelector(`input[name="docType"][value="${doc.docType}"]`).checked = true;
    document.getElementById('doc-content').value = doc.content || '';
    document.getElementById('doc-file-url').value = doc.fileUrl || '';
    document.getElementById('doc-file-name').value = doc.fileName || '';
    document.getElementById('doc-file-size').value = doc.fileSize || '';
    document.getElementById('doc-file-type').value = doc.fileType || '';
    document.getElementById('doc-link-url').value = doc.linkUrl || '';
    document.getElementById('doc-link-desc').value = doc.linkDesc || '';
    document.getElementById('doc-tags').value = doc.tags || '';
    document.getElementById('doc-status').value = doc.status || 1;
    onDocTypeChange();
    document.getElementById('doc-modal').style.display = 'flex';
  } catch (err) {
    showToast('加载文档异常: ' + err.message, 'error');
  }
}

async function saveDoc(e) {
  e.preventDefault();
  const id = document.getElementById('doc-id').value;
  const docType = document.querySelector('input[name="docType"]:checked').value;

  const payload = {
    baseId: currentBaseId,
    title: document.getElementById('doc-title').value.trim(),
    docType: docType,
    tags: document.getElementById('doc-tags').value.trim(),
    status: parseInt(document.getElementById('doc-status').value)
  };

  // 根据类型填充字段
  if (docType === 'INTERNAL') {
    payload.content = document.getElementById('doc-content').value;
  } else if (docType === 'FILE') {
    payload.fileUrl = document.getElementById('doc-file-url').value.trim();
    payload.fileName = document.getElementById('doc-file-name').value.trim();
    payload.fileSize = document.getElementById('doc-file-size').value ? parseInt(document.getElementById('doc-file-size').value) : null;
    payload.fileType = document.getElementById('doc-file-type').value.trim();
  } else if (docType === 'LINK') {
    payload.linkUrl = document.getElementById('doc-link-url').value.trim();
    payload.linkDesc = document.getElementById('doc-link-desc').value.trim();
  }

  try {
    const isEdit = !!id;
    const url = isEdit ? `${API_BASE}/documents/${id}` : `${API_BASE}/documents`;
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('保存失败: ' + result.msg, 'error');
      return;
    }
    closeDocModal();
    loadDocuments();
    loadBases(); // 刷新知识库列表（文档数量可能变化）
    showToast(isEdit ? '文档更新成功' : '文档创建成功');
  } catch (err) {
    showToast('保存异常: ' + err.message, 'error');
  }
}

async function deleteDocument(id) {
  if (!await showConfirmDialog('确定要删除此文档吗？', '🗑️')) return;
  try {
    const res = await fetch(`${API_BASE}/documents/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.msg, 'error');
      return;
    }
    loadDocuments();
    loadBases();
    showToast('删除成功');
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

async function viewDocument(id) {
  try {
    const res = await fetch(`${API_BASE}/documents/${id}`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载文档失败: ' + result.msg, 'error');
      return;
    }
    const doc = result.data;
    renderDocDetail(doc);
  } catch (err) {
    showToast('加载文档异常: ' + err.message, 'error');
  }
}

function renderDocDetail(doc) {
  document.getElementById('doc-detail-title').textContent = doc.title || '文档详情';

  const typeIcon = doc.docType === 'INTERNAL' ? '📝' : doc.docType === 'FILE' ? '📎' : '🔗';
  const statusText = doc.status === 1 ? '已发布' : doc.status === 0 ? '草稿' : '已归档';

  let html = `<div class="doc-detail-meta">
    <div class="doc-detail-meta-item"><span class="doc-detail-meta-label">类型:</span> ${typeIcon} ${doc.getDocTypeDisplay ? doc.getDocTypeDisplay() : doc.docType}</div>
    <div class="doc-detail-meta-item"><span class="doc-detail-meta-label">状态:</span> ${statusText}</div>
    <div class="doc-detail-meta-item"><span class="doc-detail-meta-label">浏览:</span> ${doc.viewCount || 0}</div>
    <div class="doc-detail-meta-item"><span class="doc-detail-meta-label">更新:</span> ${formatTime(doc.updateTime)}</div>
    ${doc.tags ? `<div class="doc-detail-meta-item"><span class="doc-detail-meta-label">标签:</span> ${escapeHtml(doc.tags)}</div>` : ''}
  </div>`;

  // 根据类型渲染内容
  if (doc.docType === 'INTERNAL') {
    html += `<div class="doc-detail-content">${doc.content || '<p style="color:#999;">暂无内容</p>'}</div>`;
  } else if (doc.docType === 'FILE') {
    html += `<div class="doc-detail-content">
      <p><strong>文件名:</strong> ${escapeHtml(doc.fileName || '-')}</p>
      <p><strong>文件大小:</strong> ${doc.fileSizeDisplay ? doc.fileSizeDisplay : (doc.fileSize || '-')}</p>
      <p><strong>文件类型:</strong> ${escapeHtml(doc.fileType || '-')}</p>
      <p><strong>下载地址:</strong> <a href="${escapeHtml(doc.fileUrl)}" target="_blank">${escapeHtml(doc.fileUrl)}</a></p>
    </div>`;
  } else if (doc.docType === 'LINK') {
    html += `<div class="doc-detail-content">
      <p><strong>链接描述:</strong> ${escapeHtml(doc.linkDesc || '-')}</p>
      <p><strong>链接地址:</strong> <a href="${escapeHtml(doc.linkUrl)}" target="_blank">${escapeHtml(doc.linkUrl)}</a></p>
    </div>`;
  }

  document.getElementById('doc-detail-body').innerHTML = html;
  document.getElementById('doc-detail-modal').style.display = 'flex';
}

function closeDocDetail() {
  document.getElementById('doc-detail-modal').style.display = 'none';
}

function onDocTypeChange() {
  const docType = document.querySelector('input[name="docType"]:checked').value;
  document.getElementById('internal-fields').style.display = docType === 'INTERNAL' ? '' : 'none';
  document.getElementById('file-fields').style.display = docType === 'FILE' ? '' : 'none';
  document.getElementById('link-fields').style.display = docType === 'LINK' ? '' : 'none';
}

function closeDocModal() {
  document.getElementById('doc-modal').style.display = 'none';
}

// ==================== 工具函数 ====================

function formatTime(timeStr) {
  if (!timeStr) return '-';
  try {
    const d = new Date(timeStr);
    if (isNaN(d.getTime())) return String(timeStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return String(timeStr);
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
