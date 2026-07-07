/**
 * 知识库管理页面 JS
 */

const KNOWLEDGE_API_BASE = '/api/knowledge';
const BASE_PAGE_SIZE = 20;

// 当前状态
let currentBaseId = null;
let currentDocType = '';
let currentPage = 1;
let totalPages = 0;
let allBases = [];

// 知识库左侧滚动分页状态
let basePage = 1;
let baseTotalPages = 0;
let baseLoading = false;
let baseAllLoaded = false;

// 知识库模块初始化标志，避免重复初始化
let knowledgeInitialized = false;

function initKnowledge() {
  if (!knowledgeInitialized) {
    loadBases();
    // 监听左侧知识库列表滚动事件，触底加载更多
    const baseListEl = document.getElementById('knowledge-base-list');
    if (baseListEl) {
      baseListEl.addEventListener('scroll', onBaseListScroll);
    }
  }
}

function onBaseListScroll() {
  const el = document.getElementById('knowledge-base-list');
  if (!el || baseLoading || baseAllLoaded) return;
  // 距离底部 50px 时触发加载
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 50) {
    loadMoreBases();
  }
}

// ==================== 知识库 ====================

async function loadBases() {
  basePage = 1;
  baseAllLoaded = false;
  allBases = [];
  const container = document.getElementById('knowledge-base-list');
  container.innerHTML = '<div class="knowledge-base-loading">加载中...</div>';

  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/paged`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ page: 1, size: BASE_PAGE_SIZE })
    });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const result = await res.json();
    if (!result.success) {
      showToast('加载知识库失败: ' + result.msg, 'error');
      container.innerHTML = '<div class="knowledge-empty-hint">加载失败，请刷新重试</div>';
      return;
    }
    const data = result.data;
    allBases = data.records || [];
    baseTotalPages = data.pages || 0;
    baseAllLoaded = basePage >= baseTotalPages;
    renderBaseList();
    knowledgeInitialized = true;
  } catch (err) {
    console.error('加载知识库异常:', err);
    showToast('加载知识库异常: ' + err.message, 'error');
    container.innerHTML = '<div class="knowledge-empty-hint">加载失败，请刷新重试</div>';
    return;
  }
  try { updateParentOptions(); } catch {}
}

async function loadMoreBases() {
  if (baseLoading || baseAllLoaded) return;
  baseLoading = true;
  basePage++;

  // 显示加载提示
  const container = document.getElementById('knowledge-base-list');
  const loadingEl = document.createElement('div');
  loadingEl.className = 'knowledge-base-loading';
  loadingEl.id = 'base-loading-indicator';
  loadingEl.textContent = '加载更多...';
  container.appendChild(loadingEl);

  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/paged`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ page: basePage, size: BASE_PAGE_SIZE })
    });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const result = await res.json();
    if (!result.success) {
      showToast('加载更多知识库失败', 'error');
      basePage--;
      removeLoadingIndicator();
      baseLoading = false;
      return;
    }
    const data = result.data;
    const newBases = data.records || [];
    allBases = allBases.concat(newBases);
    baseTotalPages = data.pages || 0;
    baseAllLoaded = basePage >= baseTotalPages;

    // 追加渲染新条目
    removeLoadingIndicator();
    appendBaseListItems(newBases);
  } catch (err) {
    console.error('加载更多知识库异常:', err);
    basePage--;
    removeLoadingIndicator();
    showToast('加载更多失败: ' + err.message, 'error');
  }
  baseLoading = false;
}

function removeLoadingIndicator() {
  const el = document.getElementById('base-loading-indicator');
  if (el) el.remove();
}

function renderBaseList() {
  const container = document.getElementById('knowledge-base-list');
  if (!allBases || allBases.length === 0) {
    container.innerHTML = '<div class="knowledge-empty-hint">暂无知识库，点击右上角创建</div>';
    return;
  }
  container.innerHTML = renderBaseItems(allBases);
  appendLoadMoreHint();
}

function renderBaseItems(bases) {
  return bases.map(base => renderSingleBaseItem(base)).join('');
}

function renderSingleBaseItem(base) {
  const isActive = base.id === currentBaseId ? 'active' : '';
  const hasChildren = base.hasChildren === true;
  const expandIcon = hasChildren
    ? `<span class="knowledge-base-expand" onclick="event.stopPropagation();toggleBaseChildren(${base.id}, this)" title="展开子知识库">▶</span>`
    : `<span class="knowledge-base-expand placeholder"></span>`;

  return `<div class="knowledge-base-item ${isActive}" data-base-id="${base.id}">
    <div class="knowledge-base-item-header" onclick="selectBase(${base.id}, '${escapeAttr(base.name)}', this.parentElement)">
      ${expandIcon}
      <span class="knowledge-base-icon">${base.icon || '📚'}</span>
      <span class="knowledge-base-name">${escapeHtml(base.name)}</span>
      <span class="knowledge-base-doc-count">${base.documentCount || 0}</span>
    </div>
    ${base.description ? `<div class="knowledge-base-desc" onclick="selectBase(${base.id}, '${escapeAttr(base.name)}', this.parentElement)">${escapeHtml(base.description)}</div>` : ''}
    <div class="knowledge-base-actions">
      <button class="knowledge-base-action-btn" onclick="event.stopPropagation();editBase(${base.id})">✏️ 编辑</button>
      <button class="knowledge-base-action-btn delete" onclick="event.stopPropagation();deleteBase(${base.id})">🗑️ 删除</button>
    </div>
    <div class="knowledge-base-children" id="base-children-${base.id}"></div>
  </div>`;
}

function appendBaseListItems(bases) {
  const container = document.getElementById('knowledge-base-list');
  removeLoadingIndicator();
  const html = renderBaseItems(bases);
  container.insertAdjacentHTML('beforeend', html);
  appendLoadMoreHint();
}

function appendLoadMoreHint() {
  const container = document.getElementById('knowledge-base-list');
  // 移除旧的提示
  const oldHint = container.querySelector('.knowledge-base-load-more-hint');
  if (oldHint) oldHint.remove();

  if (!baseAllLoaded) {
    const hint = document.createElement('div');
    hint.className = 'knowledge-base-load-more-hint';
    hint.textContent = '滚动加载更多...';
    container.appendChild(hint);
  }
}

async function toggleBaseChildren(baseId, arrowEl) {
  const childrenContainer = document.getElementById(`base-children-${baseId}`);
  if (!childrenContainer) return;

  const isExpanded = childrenContainer.classList.contains('expanded');
  if (isExpanded) {
    // 折叠
    childrenContainer.classList.remove('expanded');
    childrenContainer.innerHTML = '';
    arrowEl.classList.remove('expanded');
    arrowEl.textContent = '▶';
    return;
  }

  // 展开：加载子知识库
  childrenContainer.classList.add('expanded');
  arrowEl.classList.add('expanded');
  arrowEl.textContent = '▼';
  childrenContainer.innerHTML = '<div class="knowledge-base-loading">加载中...</div>';

  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/${baseId}/children`);
    const result = await res.json();
    if (!result.success) {
      childrenContainer.innerHTML = '<div class="knowledge-base-loading">加载失败</div>';
      return;
    }
    const children = result.data || [];
    if (children.length === 0) {
      childrenContainer.innerHTML = '<div class="knowledge-base-loading">无子知识库</div>';
      return;
    }
    childrenContainer.innerHTML = children.map(child => {
      const isActive = child.id === currentBaseId ? 'active' : '';
      return `<div class="knowledge-base-item knowledge-base-child-item ${isActive}"
                   onclick="selectBase(${child.id}, '${escapeAttr(child.name)}', this)">
        <div class="knowledge-base-item-header">
          <span class="knowledge-base-expand placeholder"></span>
          <span class="knowledge-base-icon">${child.icon || '📚'}</span>
          <span class="knowledge-base-name">${escapeHtml(child.name)}</span>
          <span class="knowledge-base-doc-count">${child.documentCount || 0}</span>
        </div>
        ${child.description ? `<div class="knowledge-base-desc">${escapeHtml(child.description)}</div>` : ''}
        <div class="knowledge-base-actions">
          <button class="knowledge-base-action-btn" onclick="event.stopPropagation();editBase(${child.id})">✏️ 编辑</button>
          <button class="knowledge-base-action-btn delete" onclick="event.stopPropagation();deleteBase(${child.id})">🗑️ 删除</button>
        </div>
      </div>`;
    }).join('');
  } catch (err) {
    childrenContainer.innerHTML = '<div class="knowledge-base-loading">加载异常</div>';
  }
}

async function selectBase(baseId, baseName, el) {
  currentBaseId = baseId;
  currentPage = 1;
  currentDocType = '';

  // 更新高亮（包括顶级和子知识库）
  document.querySelectorAll('.knowledge-base-item').forEach(e => e.classList.remove('active'));
  if (el) el.classList.add('active');

  // 更新标题
  document.getElementById('knowledge-current-base-name').textContent = baseName || '知识库';
  document.getElementById('knowledge-add-doc-btn').style.display = '';

  // 重置类型筛选
  document.querySelectorAll('.knowledge-filter-btn').forEach(b => b.classList.remove('active'));
  document.querySelector('.knowledge-filter-btn[data-type=""]').classList.add('active');

  await loadDocuments();
}

function showBaseForm(parentId) {
  document.getElementById('knowledge-base-modal-title').textContent = '新建知识库';
  document.getElementById('knowledge-base-id').value = '';
  document.getElementById('knowledge-base-name').value = '';
  document.getElementById('knowledge-base-description').value = '';
  document.getElementById('knowledge-base-icon').value = '📚';
  document.getElementById('knowledge-base-sort-order').value = '0';
  document.getElementById('knowledge-base-is-public').value = '0';
  document.getElementById('knowledge-base-parent-id').value = parentId || '0';
  document.getElementById('knowledge-base-modal').style.display = 'flex';
}

function editBase(id) {
  // 先从已加载的列表中查找
  let base = allBases.find(b => b.id === id);
  if (!base) {
    // 未加载时直接打开弹窗，由用户重新选择
    showToast('请先加载该知识库数据', 'warning');
    return;
  }

  document.getElementById('knowledge-base-modal-title').textContent = '编辑知识库';
  document.getElementById('knowledge-base-id').value = base.id;
  document.getElementById('knowledge-base-name').value = base.name || '';
  document.getElementById('knowledge-base-description').value = base.description || '';
  document.getElementById('knowledge-base-icon').value = base.icon || '📚';
  document.getElementById('knowledge-base-sort-order').value = base.sortOrder || 0;
  document.getElementById('knowledge-base-is-public').value = base.isPublic || 0;
  document.getElementById('knowledge-base-parent-id').value = base.parentId || 0;
  document.getElementById('knowledge-base-modal').style.display = 'flex';
}

async function saveBase(e) {
  e.preventDefault();
  const id = document.getElementById('knowledge-base-id').value;
  const payload = {
    name: document.getElementById('knowledge-base-name').value.trim(),
    description: document.getElementById('knowledge-base-description').value.trim(),
    icon: document.getElementById('knowledge-base-icon').value.trim(),
    parentId: parseInt(document.getElementById('knowledge-base-parent-id').value) || 0,
    sortOrder: parseInt(document.getElementById('knowledge-base-sort-order').value) || 0,
    isPublic: parseInt(document.getElementById('knowledge-base-is-public').value)
  };

  try {
    const isEdit = !!id;
    const url = isEdit ? `${KNOWLEDGE_API_BASE}/bases/${id}` : `${KNOWLEDGE_API_BASE}/bases`;
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
    const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/${id}`, { method: 'DELETE' });
    const result = await res.json();
    if (!result.success) {
      showToast('删除失败: ' + result.msg, 'error');
      return;
    }
    if (currentBaseId === id) {
      currentBaseId = null;
      document.getElementById('knowledge-current-base-name').textContent = '请选择知识库';
      document.getElementById('knowledge-document-list').innerHTML = '<div class="knowledge-empty-hint">请从左侧选择一个知识库</div>';
      document.getElementById('knowledge-add-doc-btn').style.display = 'none';
    }
    loadBases();
    showToast('删除成功');
  } catch (err) {
    showToast('删除异常: ' + err.message, 'error');
  }
}

function closeBaseModal() {
  document.getElementById('knowledge-base-modal').style.display = 'none';
}

function updateParentOptions() {
  const select = document.getElementById('knowledge-base-parent-id');
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

  // 确保 page 是数字类型
  currentPage = parseInt(currentPage) || 1;

  const payload = {
    page: currentPage,
    size: 20
  };
  if (currentDocType) {
    payload.docType = currentDocType;
  }

  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/${currentBaseId}/documents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
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
  const container = document.getElementById('knowledge-document-list');
  document.getElementById('knowledge-doc-count').textContent = docs.length;

  if (!docs || docs.length === 0) {
    container.innerHTML = '<div class="knowledge-empty-hint">暂无文档，点击右上角创建</div>';
    return;
  }

  container.innerHTML = docs.map(doc => {
    const typeIcon = doc.docType === 'INTERNAL' ? '📝' : doc.docType === 'FILE' ? '📎' : '🔗';
    const typeClass = doc.docType.toLowerCase();
    const statusClass = doc.status === 1 ? 'published' : doc.status === 0 ? 'draft' : 'archived';
    const statusText = doc.status === 1 ? '已发布' : doc.status === 0 ? '草稿' : '已归档';
    const tags = doc.tags ? doc.tags.split(',').filter(t => t.trim()).map(t => `<span class="knowledge-doc-tag">${escapeHtml(t.trim())}</span>`).join('') : '';

    return `<div class="knowledge-doc-item" onclick="viewDocument(${doc.id})">
      <div class="knowledge-doc-type-icon ${typeClass}">${typeIcon}</div>
      <div class="knowledge-doc-info">
        <div class="knowledge-doc-title-text">${escapeHtml(doc.title)}</div>
        <div class="knowledge-doc-meta">
          <span>${doc.getDocTypeDisplay ? doc.getDocTypeDisplay() : typeIcon}</span>
          <span>${formatTime(doc.updateTime)}</span>
          <span>👁 ${doc.viewCount || 0}</span>
          ${tags ? `<span class="knowledge-doc-tags">${tags}</span>` : ''}
        </div>
      </div>
      <span class="knowledge-doc-status ${statusClass}">${statusText}</span>
      <div class="knowledge-doc-actions">
        <button class="knowledge-doc-action-btn" onclick="event.stopPropagation();editDocument(${doc.id})">编辑</button>
        <button class="knowledge-doc-action-btn delete" onclick="event.stopPropagation();deleteDocument(${doc.id})">删除</button>
      </div>
    </div>`;
  }).join('');
}

function renderPagination(data) {
  const container = document.getElementById('knowledge-pagination');
  totalPages = data.pages || 0;

  if (totalPages <= 1) {
    container.style.display = 'none';
    return;
  }

  container.style.display = 'flex';
  let html = '';
  html += `<button class="knowledge-page-btn" onclick="goToPage(${currentPage - 1})" ${currentPage <= 1 ? 'disabled' : ''}>上一页</button>`;
  html += `<span class="knowledge-page-info">第 ${currentPage} / ${totalPages} 页，共 ${data.total} 条</span>`;
  html += `<button class="knowledge-page-btn" onclick="goToPage(${currentPage + 1})" ${currentPage >= totalPages ? 'disabled' : ''}>下一页</button>`;
  container.innerHTML = html;
}

function goToPage(page) {
  currentPage = page;
  loadDocuments();
}

function filterByType(type) {
  currentDocType = type;
  currentPage = 1;
  document.querySelectorAll('.knowledge-filter-btn').forEach(b => b.classList.remove('active'));
  document.querySelector(`.knowledge-filter-btn[data-type="${type}"]`).classList.add('active');
  loadDocuments();
}

function handleSearch(event) {
  if (event.key === 'Enter') {
    const keyword = document.getElementById('knowledge-search-input').value.trim();
    if (keyword) {
      searchDocuments(keyword);
    } else {
      loadDocuments();
    }
  }
}

async function searchDocuments(keyword) {
  // 检查是否登录
  if (!isLoggedIn) {
    showGlobalToast('需要登录后才能检索知识库内容', 'login');
    return;
  }

  const payload = {
    keyword: keyword,
    page: currentPage,
    size: 20
  };

  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!result.success) {
      showToast('搜索失败: ' + result.msg, 'error');
      return;
    }
    document.getElementById('knowledge-current-base-name').textContent = `搜索: ${keyword}`;
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
  document.getElementById('knowledge-doc-modal-title').textContent = '新建文档';
  document.getElementById('knowledge-doc-id').value = '';
  document.getElementById('knowledge-doc-title').value = '';
  document.getElementById('knowledge-doc-content').value = '';
  document.getElementById('knowledge-doc-file-url').value = '';
  document.getElementById('knowledge-doc-file-name').value = '';
  document.getElementById('knowledge-doc-file-size').value = '';
  document.getElementById('knowledge-doc-file-type').value = '';
  document.getElementById('knowledge-doc-link-url').value = '';
  document.getElementById('knowledge-doc-link-desc').value = '';
  document.getElementById('knowledge-doc-tags').value = '';
  document.getElementById('knowledge-doc-status').value = '1';
  document.querySelector('input[name="docType"][value="INTERNAL"]').checked = true;
  onDocTypeChange();
  document.getElementById('knowledge-doc-modal').style.display = 'flex';
}

async function editDocument(id) {
  try {
    const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`);
    const result = await res.json();
    if (!result.success) {
      showToast('加载文档失败: ' + result.msg, 'error');
      return;
    }
    const doc = result.data;
    document.getElementById('knowledge-doc-modal-title').textContent = '编辑文档';
    document.getElementById('knowledge-doc-id').value = doc.id;
    document.getElementById('knowledge-doc-title').value = doc.title || '';
    document.querySelector(`input[name="docType"][value="${doc.docType}"]`).checked = true;
    document.getElementById('knowledge-doc-content').value = doc.content || '';
    document.getElementById('knowledge-doc-file-url').value = doc.fileUrl || '';
    document.getElementById('knowledge-doc-file-name').value = doc.fileName || '';
    document.getElementById('knowledge-doc-file-size').value = doc.fileSize || '';
    document.getElementById('knowledge-doc-file-type').value = doc.fileType || '';
    document.getElementById('knowledge-doc-link-url').value = doc.linkUrl || '';
    document.getElementById('knowledge-doc-link-desc').value = doc.linkDesc || '';
    document.getElementById('knowledge-doc-tags').value = doc.tags || '';
    document.getElementById('knowledge-doc-status').value = doc.status || 1;
    onDocTypeChange();
    document.getElementById('knowledge-doc-modal').style.display = 'flex';
  } catch (err) {
    showToast('加载文档异常: ' + err.message, 'error');
  }
}

async function saveDoc(e) {
  e.preventDefault();
  const id = document.getElementById('knowledge-doc-id').value;
  const docType = document.querySelector('input[name="docType"]:checked').value;

  const payload = {
    baseId: currentBaseId,
    title: document.getElementById('knowledge-doc-title').value.trim(),
    docType: docType,
    tags: document.getElementById('knowledge-doc-tags').value.trim(),
    status: parseInt(document.getElementById('knowledge-doc-status').value)
  };

  // 根据类型填充字段
  if (docType === 'INTERNAL') {
    payload.content = document.getElementById('knowledge-doc-content').value;
  } else if (docType === 'FILE') {
    payload.fileUrl = document.getElementById('knowledge-doc-file-url').value.trim();
    payload.fileName = document.getElementById('knowledge-doc-file-name').value.trim();
    payload.fileSize = document.getElementById('knowledge-doc-file-size').value ? parseInt(document.getElementById('knowledge-doc-file-size').value) : null;
    payload.fileType = document.getElementById('knowledge-doc-file-type').value.trim();
  } else if (docType === 'LINK') {
    payload.linkUrl = document.getElementById('knowledge-doc-link-url').value.trim();
    payload.linkDesc = document.getElementById('knowledge-doc-link-desc').value.trim();
  }

  try {
    const isEdit = !!id;
    const url = isEdit ? `${KNOWLEDGE_API_BASE}/documents/${id}` : `${KNOWLEDGE_API_BASE}/documents`;
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
    const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`, { method: 'DELETE' });
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
    const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`);
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
  document.getElementById('knowledge-doc-detail-title').textContent = doc.title || '文档详情';

  const typeIcon = doc.docType === 'INTERNAL' ? '📝' : doc.docType === 'FILE' ? '📎' : '🔗';
  const statusText = doc.status === 1 ? '已发布' : doc.status === 0 ? '草稿' : '已归档';

  let html = `<div class="knowledge-doc-detail-meta">
    <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">类型:</span> ${typeIcon} ${doc.getDocTypeDisplay ? doc.getDocTypeDisplay() : doc.docType}</div>
    <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">状态:</span> ${statusText}</div>
    <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">浏览:</span> ${doc.viewCount || 0}</div>
    <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">更新:</span> ${formatTime(doc.updateTime)}</div>
    ${doc.tags ? `<div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">标签:</span> ${escapeHtml(doc.tags)}</div>` : ''}
  </div>`;

  // 根据类型渲染内容
  if (doc.docType === 'INTERNAL') {
    html += `<div class="knowledge-doc-detail-content">${doc.content || '<p style="color:#999;">暂无内容</p>'}</div>`;
  } else if (doc.docType === 'FILE') {
    html += `<div class="knowledge-doc-detail-content">
      <p><strong>文件名:</strong> ${escapeHtml(doc.fileName || '-')}</p>
      <p><strong>文件大小:</strong> ${doc.fileSizeDisplay ? doc.fileSizeDisplay : (doc.fileSize || '-')}</p>
      <p><strong>文件类型:</strong> ${escapeHtml(doc.fileType || '-')}</p>
      <p><strong>下载地址:</strong> <a href="${escapeHtml(doc.fileUrl)}" target="_blank">${escapeHtml(doc.fileUrl)}</a></p>
    </div>`;
  } else if (doc.docType === 'LINK') {
    html += `<div class="knowledge-doc-detail-content">
      <p><strong>链接描述:</strong> ${escapeHtml(doc.linkDesc || '-')}</p>
      <p><strong>链接地址:</strong> <a href="${escapeHtml(doc.linkUrl)}" target="_blank">${escapeHtml(doc.linkUrl)}</a></p>
    </div>`;
  }

  document.getElementById('knowledge-doc-detail-body').innerHTML = html;
  document.getElementById('knowledge-doc-detail-modal').style.display = 'flex';
}

function closeDocDetail() {
  document.getElementById('knowledge-doc-detail-modal').style.display = 'none';
}

function onDocTypeChange() {
  const docType = document.querySelector('input[name="docType"]:checked').value;
  document.getElementById('knowledge-internal-fields').style.display = docType === 'INTERNAL' ? '' : 'none';
  document.getElementById('knowledge-file-fields').style.display = docType === 'FILE' ? '' : 'none';
  document.getElementById('knowledge-link-fields').style.display = docType === 'LINK' ? '' : 'none';
}

function closeDocModal() {
  document.getElementById('knowledge-doc-modal').style.display = 'none';
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
