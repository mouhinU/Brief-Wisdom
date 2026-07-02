/**
 * 知识库管理组件
 * 用于系统设置页面中的知识库管理功能
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
(function() {
    'use strict';

    // 组件状态
    const state = {
        currentBaseId: null,
        currentPage: 1,
        totalPages: 0,
        currentDocType: '',
        allBases: [],
        basePage: 1,
        baseTotalPages: 0,
        baseAllLoaded: false,
        baseLoading: false
    };

    const KNOWLEDGE_API_BASE = '/api/knowledge';

    /**
     * 初始化组件
     */
    function init(options = {}) {
        const containerId = options.containerId || 'knowledge-tab-content';
        console.log('[KnowledgeManagement] 知识库组件初始化开始，容器:', containerId);

        // 如果提供了容器且模板生成器可用，先渲染 HTML 模板
        const container = document.getElementById(containerId);
        if (container && window.KnowledgeManagementTemplate) {
            console.log('[KnowledgeManagement] 使用模板生成器渲染 HTML');
            KnowledgeManagementTemplate.render(container);
        }
        
        loadBases();
        setupScrollListener();
    }

    /**
     * 设置滚动监听
     */
    function setupScrollListener() {
        const baseListEl = document.getElementById('knowledge-base-list');
        if (baseListEl) {
            baseListEl.addEventListener('scroll', () => {
                const { scrollTop, scrollHeight, clientHeight } = baseListEl;
                if (scrollTop + clientHeight >= scrollHeight - 10 && !state.baseLoading && !state.baseAllLoaded) {
                    loadMoreBases();
                }
            });
        }
    }

    /**
     * 加载知识库列表
     */
    async function loadBases() {
        console.log('[KnowledgeManagement] loadBases 开始执行');
        state.basePage = 1;
        state.allBases = [];
        state.baseAllLoaded = false;

        const container = document.getElementById('knowledge-base-list');
        if (!container) {
            console.error('[KnowledgeManagement] 找不到 knowledge-base-list 容器');
            return;
        }

        container.innerHTML = '<div class="knowledge-base-loading">加载中...</div>';

        try {
            const url = `${KNOWLEDGE_API_BASE}/bases/paged?page=${state.basePage}&size=20`;
            console.log('[KnowledgeManagement] 请求 URL:', url);
            
            const res = await fetch(url);
            console.log('[KnowledgeManagement] HTTP 状态码:', res.status);
            
            const result = await res.json();
            console.log('[KnowledgeManagement] API 返回数据:', JSON.stringify(result, null, 2));

            // 兼容两种返回格式：
            // 1. { success: true, data: { records: [...], pages: ... } } - 有 Result 包装
            // 2. { records: [...], pages: ... } - 直接返回 Page 对象
            let data;
            if (result.success !== undefined && result.data) {
                // 有 Result 包装
                data = result.data;
                console.log('[KnowledgeManagement] 检测到 Result 包装格式');
                console.log('[KnowledgeManagement] data 对象:', JSON.stringify(data, null, 2));
            } else {
                // 直接返回 Page 对象
                data = result;
                console.log('[KnowledgeManagement] 检测到直接返回 Page 对象格式');
            }

            console.log('[KnowledgeManagement] data.records:', data.records);
            console.log('[KnowledgeManagement] data.pages:', data.pages);
            console.log('[KnowledgeManagement] records 数量:', data.records ? data.records.length : 'undefined');

            state.allBases = data.records || [];
            state.baseTotalPages = data.pages || 0;
            state.baseAllLoaded = state.basePage >= state.baseTotalPages;

            console.log('[KnowledgeManagement] 处理后的 allBases 数量:', state.allBases.length);
            console.log('[KnowledgeManagement] baseTotalPages:', state.baseTotalPages);

            renderBaseList();
        } catch (err) {
            console.error('[KnowledgeManagement] 加载知识库异常:', err);
            showToast('加载知识库异常: ' + err.message, 'error');
            container.innerHTML = '<div class="knowledge-empty-hint">加载失败，请刷新重试</div>';
        }
    }

    /**
     * 加载更多知识库
     */
    async function loadMoreBases() {
        if (state.baseLoading || state.baseAllLoaded) return;

        state.baseLoading = true;
        state.basePage++;

        const loadingEl = document.createElement('div');
        loadingEl.id = 'base-loading-indicator';
        loadingEl.className = 'knowledge-base-loading';
        loadingEl.textContent = '加载中...';
        const container = document.getElementById('knowledge-base-list');
        container.appendChild(loadingEl);

        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/paged?page=${state.basePage}&size=20`);
            const result = await res.json();

            // 兼容两种返回格式
            let data;
            if (result.success !== undefined && result.data) {
                data = result.data;
            } else {
                data = result;
            }

            const newBases = data.records || [];
            state.allBases = state.allBases.concat(newBases);
            state.baseTotalPages = data.pages || 0;
            state.baseAllLoaded = state.basePage >= state.baseTotalPages;

            removeLoadingIndicator();
            appendBaseListItems(newBases);
        } catch (err) {
            console.error('[KnowledgeManagement] 加载更多知识库异常:', err);
            state.basePage--;
            removeLoadingIndicator();
            showToast('加载更多失败: ' + err.message, 'error');
        }
        state.baseLoading = false;
    }

    /**
     * 移除加载指示器
     */
    function removeLoadingIndicator() {
        const el = document.getElementById('base-loading-indicator');
        if (el) el.remove();
    }

    /**
     * 渲染知识库列表
     */
    function renderBaseList() {
        const container = document.getElementById('knowledge-base-list');
        if (!state.allBases || state.allBases.length === 0) {
            container.innerHTML = '<div class="knowledge-empty-hint">暂无知识库，点击右上角创建</div>';
            return;
        }
        container.innerHTML = renderBaseItems(state.allBases);
        appendLoadMoreHint();
    }

    /**
     * 渲染知识库项
     */
    function renderBaseItems(bases) {
        return bases.map(base => renderSingleBaseItem(base)).join('');
    }

    /**
     * 渲染单个知识库项
     */
    function renderSingleBaseItem(base) {
        const isActive = base.id === state.currentBaseId ? 'active' : '';
        const hasChildren = base.hasChildren === true;
        const expandIcon = hasChildren
            ? `<span class="knowledge-base-expand" onclick="event.stopPropagation();KnowledgeManagement.toggleBaseChildren(${base.id}, this)" title="展开子知识库">▶</span>`
            : `<span class="knowledge-base-expand placeholder"></span>`;

        return `<div class="knowledge-base-item ${isActive}" data-base-id="${base.id}">
            <div class="knowledge-base-item-header" onclick="KnowledgeManagement.selectBase(${base.id}, '${escapeAttr(base.name)}', this.parentElement)">
                ${expandIcon}
                <span class="knowledge-base-icon">${base.icon || '📚'}</span>
                <span class="knowledge-base-name">${escapeHtml(base.name)}</span>
                <span class="knowledge-base-doc-count">${base.documentCount || 0}</span>
            </div>
            ${base.description ? `<div class="knowledge-base-desc" onclick="KnowledgeManagement.selectBase(${base.id}, '${escapeAttr(base.name)}', this.parentElement)">${escapeHtml(base.description)}</div>` : ''}
            <div class="knowledge-base-actions">
                <button class="knowledge-base-action-btn" onclick="event.stopPropagation();KnowledgeManagement.editBase(${base.id})">✏️ 编辑</button>
                <button class="knowledge-base-action-btn delete" onclick="event.stopPropagation();KnowledgeManagement.deleteBase(${base.id})">🗑️ 删除</button>
            </div>
            <div class="knowledge-base-children" id="base-children-${base.id}"></div>
        </div>`;
    }

    /**
     * 追加知识库项
     */
    function appendBaseListItems(bases) {
        const container = document.getElementById('knowledge-base-list');
        removeLoadingIndicator();
        const html = renderBaseItems(bases);
        container.insertAdjacentHTML('beforeend', html);
        appendLoadMoreHint();
    }

    /**
     * 添加加载更多提示
     */
    function appendLoadMoreHint() {
        const container = document.getElementById('knowledge-base-list');
        const oldHint = container.querySelector('.knowledge-base-load-more-hint');
        if (oldHint) oldHint.remove();

        if (!state.baseAllLoaded) {
            const hint = document.createElement('div');
            hint.className = 'knowledge-base-load-more-hint';
            hint.textContent = '滚动加载更多...';
            container.appendChild(hint);
        }
    }

    /**
     * 切换子知识库展开/折叠
     */
    async function toggleBaseChildren(baseId, arrowEl) {
        const childrenContainer = document.getElementById(`base-children-${baseId}`);
        if (!childrenContainer) return;

        const isExpanded = childrenContainer.classList.contains('expanded');
        if (isExpanded) {
            childrenContainer.classList.remove('expanded');
            childrenContainer.innerHTML = '';
            arrowEl.classList.remove('expanded');
            arrowEl.textContent = '▶';
            return;
        }

        childrenContainer.classList.add('expanded');
        arrowEl.classList.add('expanded');
        arrowEl.textContent = '▼';
        childrenContainer.innerHTML = '<div class="knowledge-base-loading">加载中...</div>';

        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/${baseId}/children`);
            const result = await res.json();
            
            // 兼容两种返回格式
            let children;
            if (result.success !== undefined && result.data) {
                children = result.data;
            } else {
                children = result;
            }
            
            if (!children || children.length === 0) {
                childrenContainer.innerHTML = '<div class="knowledge-base-loading">无子知识库</div>';
                return;
            }
            childrenContainer.innerHTML = children.map(child => {
                const isActive = child.id === state.currentBaseId ? 'active' : '';
                return `<div class="knowledge-base-item knowledge-base-child-item ${isActive}"
                             onclick="KnowledgeManagement.selectBase(${child.id}, '${escapeAttr(child.name)}', this)">
                    <div class="knowledge-base-item-header">
                        <span class="knowledge-base-expand placeholder"></span>
                        <span class="knowledge-base-icon">${child.icon || '📚'}</span>
                        <span class="knowledge-base-name">${escapeHtml(child.name)}</span>
                        <span class="knowledge-base-doc-count">${child.documentCount || 0}</span>
                    </div>
                    ${child.description ? `<div class="knowledge-base-desc">${escapeHtml(child.description)}</div>` : ''}
                    <div class="knowledge-base-actions">
                        <button class="knowledge-base-action-btn" onclick="event.stopPropagation();KnowledgeManagement.editBase(${child.id})">✏️ 编辑</button>
                        <button class="knowledge-base-action-btn delete" onclick="event.stopPropagation();KnowledgeManagement.deleteBase(${child.id})">🗑️ 删除</button>
                    </div>
                </div>`;
            }).join('');
        } catch (err) {
            childrenContainer.innerHTML = '<div class="knowledge-base-loading">加载异常</div>';
        }
    }

    /**
     * 选择知识库
     */
    async function selectBase(baseId, baseName, el) {
        state.currentBaseId = baseId;
        state.currentPage = 1;
        state.currentDocType = '';

        document.querySelectorAll('.knowledge-base-item').forEach(e => e.classList.remove('active'));
        if (el) el.classList.add('active');

        document.getElementById('knowledge-current-base-name').textContent = baseName || '知识库';
        document.getElementById('knowledge-add-doc-btn').style.display = '';

        document.querySelectorAll('.knowledge-filter-btn').forEach(b => b.classList.remove('active'));
        document.querySelector('.knowledge-filter-btn[data-type=""]').classList.add('active');

        await loadDocuments();
    }

    /**
     * 显示知识库表单
     */
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

    /**
     * 编辑知识库
     */
    function editBase(id) {
        let base = state.allBases.find(b => b.id === id);
        if (!base) {
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

    /**
     * 保存知识库
     */
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
            // 直接返回 DTO，没有 Result 包装
            closeBaseModal();
            loadBases();
            showToast(isEdit ? '知识库更新成功' : '知识库创建成功');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 删除知识库
     */
    async function deleteBase(id) {
        if (!await showConfirmDialog('确定要删除此知识库吗？其下所有文档也会被删除。', '🗑️')) return;
        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/bases/${id}`, { method: 'DELETE' });
            // 直接返回 Boolean，没有 Result 包装
            if (state.currentBaseId === id) {
                state.currentBaseId = null;
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

    /**
     * 关闭知识库弹窗
     */
    function closeBaseModal() {
        document.getElementById('knowledge-base-modal').style.display = 'none';
    }

    /**
     * 加载文档列表
     */
    async function loadDocuments() {
        if (!state.currentBaseId) return;

        const url = `${KNOWLEDGE_API_BASE}/bases/${state.currentBaseId}/documents?page=${state.currentPage}&size=20${state.currentDocType ? '&docType=' + state.currentDocType : ''}`;
        try {
            const res = await fetch(url);
            const result = await res.json();
            
            // 兼容两种返回格式
            let data;
            if (result.success !== undefined && result.data) {
                data = result.data;
            } else {
                data = result;
            }
            
            renderDocuments(data.records || []);
            renderPagination(data);
        } catch (err) {
            showToast('加载文档异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染文档列表
     */
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

            return `<div class="knowledge-doc-item" onclick="KnowledgeManagement.viewDocument(${doc.id})">
                <div class="knowledge-doc-type-icon ${typeClass}">${typeIcon}</div>
                <div class="knowledge-doc-info">
                    <div class="knowledge-doc-title-text">${escapeHtml(doc.title)}</div>
                    <div class="knowledge-doc-meta">
                        <span>${doc.docTypeDisplay || typeIcon}</span>
                        <span>${formatTime(doc.updateTime)}</span>
                        <span>👁 ${doc.viewCount || 0}</span>
                        ${tags ? `<span class="knowledge-doc-tags">${tags}</span>` : ''}
                    </div>
                </div>
                <span class="knowledge-doc-status ${statusClass}">${statusText}</span>
                <div class="knowledge-doc-actions">
                    <button class="knowledge-doc-action-btn" onclick="event.stopPropagation();KnowledgeManagement.editDocument(${doc.id})">编辑</button>
                    <button class="knowledge-doc-action-btn delete" onclick="event.stopPropagation();KnowledgeManagement.deleteDocument(${doc.id})">删除</button>
                </div>
            </div>`;
        }).join('');
    }

    /**
     * 渲染分页
     */
    function renderPagination(data) {
        const container = document.getElementById('knowledge-pagination');
        state.totalPages = data.pages || 0;

        if (state.totalPages <= 1) {
            container.style.display = 'none';
            return;
        }

        container.style.display = 'flex';
        let html = '';
        html += `<button class="knowledge-page-btn" onclick="KnowledgeManagement.goToPage(${state.currentPage - 1})" ${state.currentPage <= 1 ? 'disabled' : ''}>上一页</button>`;
        html += `<span class="knowledge-page-info">第 ${state.currentPage} / ${state.totalPages} 页，共 ${data.total} 条</span>`;
        html += `<button class="knowledge-page-btn" onclick="KnowledgeManagement.goToPage(${state.currentPage + 1})" ${state.currentPage >= state.totalPages ? 'disabled' : ''}>下一页</button>`;
        container.innerHTML = html;
    }

    /**
     * 跳转到指定页
     */
    function goToPage(page) {
        state.currentPage = page;
        loadDocuments();
    }

    /**
     * 按类型筛选
     */
    function filterByType(type) {
        state.currentDocType = type;
        state.currentPage = 1;
        document.querySelectorAll('.knowledge-filter-btn').forEach(b => b.classList.remove('active'));
        document.querySelector(`.knowledge-filter-btn[data-type="${type}"]`).classList.add('active');
        loadDocuments();
    }

    /**
     * 处理搜索
     */
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

    /**
     * 搜索文档
     */
    async function searchDocuments(keyword) {
        const url = `${KNOWLEDGE_API_BASE}/documents/search?keyword=${encodeURIComponent(keyword)}&page=${state.currentPage}&size=20`;
        try {
            const res = await fetch(url);
            const result = await res.json();
            
            // 兼容两种返回格式
            let data;
            if (result.success !== undefined && result.data) {
                data = result.data;
            } else {
                data = result;
            }
            
            document.getElementById('knowledge-current-base-name').textContent = `搜索: ${keyword}`;
            renderDocuments(data.records || []);
            renderPagination(data);
        } catch (err) {
            showToast('搜索异常: ' + err.message, 'error');
        }
    }

    /**
     * 显示文档表单
     */
    function showDocForm() {
        if (!state.currentBaseId) {
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

    /**
     * 编辑文档
     */
    async function editDocument(id) {
        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`);
            const result = await res.json();
            
            // 兼容两种返回格式
            let doc;
            if (result.success !== undefined && result.data) {
                doc = result.data;
            } else {
                doc = result;
            }
            
            document.getElementById('knowledge-doc-modal-title').textContent = '编辑文档';
            document.getElementById('knowledge-doc-id').value = doc.id;
            // 确保 currentBaseId 与文档一致，避免保存时 baseId 错误
            if (doc.baseId && doc.baseId !== state.currentBaseId) {
                state.currentBaseId = doc.baseId;
            }
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

    /**
     * 保存文档
     */
    async function saveDoc(e) {
        e.preventDefault();
        const id = document.getElementById('knowledge-doc-id').value;
        const docType = document.querySelector('input[name="docType"]:checked').value;

        const payload = {
            baseId: state.currentBaseId,
            title: document.getElementById('knowledge-doc-title').value.trim(),
            docType: docType,
            tags: document.getElementById('knowledge-doc-tags').value.trim(),
            status: parseInt(document.getElementById('knowledge-doc-status').value)
        };

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
            // 直接返回 DTO
            closeDocModal();
            loadDocuments();
            loadBases();
            showToast(isEdit ? '文档更新成功' : '文档创建成功');
        } catch (err) {
            showToast('保存异常: ' + err.message, 'error');
        }
    }

    /**
     * 删除文档
     */
    async function deleteDocument(id) {
        if (!await showConfirmDialog('确定要删除此文档吗？', '🗑️')) return;
        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`, { method: 'DELETE' });
            // 直接返回 Boolean
            loadDocuments();
            loadBases();
            showToast('删除成功');
        } catch (err) {
            showToast('删除异常: ' + err.message, 'error');
        }
    }

    /**
     * 查看文档详情
     */
    async function viewDocument(id) {
        try {
            const res = await fetch(`${KNOWLEDGE_API_BASE}/documents/${id}`);
            const result = await res.json();
            
            // 兼容两种返回格式
            let doc;
            if (result.success !== undefined && result.data) {
                doc = result.data;
            } else {
                doc = result;
            }
            
            renderDocDetail(doc);
        } catch (err) {
            showToast('加载文档异常: ' + err.message, 'error');
        }
    }

    /**
     * 渲染文档详情
     */
    function renderDocDetail(doc) {
        document.getElementById('knowledge-doc-detail-title').textContent = doc.title || '文档详情';

        const typeIcon = doc.docType === 'INTERNAL' ? '📝' : doc.docType === 'FILE' ? '📎' : '🔗';
        const statusText = doc.status === 1 ? '已发布' : doc.status === 0 ? '草稿' : '已归档';

        let html = `<div class="knowledge-doc-detail-meta">
            <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">类型:</span> ${typeIcon} ${doc.docTypeDisplay || doc.docType}</div>
            <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">状态:</span> ${statusText}</div>
            <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">浏览:</span> ${doc.viewCount || 0}</div>
            <div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">更新:</span> ${formatTime(doc.updateTime)}</div>
            ${doc.tags ? `<div class="knowledge-doc-detail-meta-item"><span class="knowledge-doc-detail-meta-label">标签:</span> ${escapeHtml(doc.tags)}</div>` : ''}
        </div>`;

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

    /**
     * 关闭文档详情
     */
    function closeDocDetail() {
        document.getElementById('knowledge-doc-detail-modal').style.display = 'none';
    }

    /**
     * 文档类型变化
     */
    function onDocTypeChange() {
        const docType = document.querySelector('input[name="docType"]:checked').value;
        document.getElementById('knowledge-internal-fields').style.display = docType === 'INTERNAL' ? '' : 'none';
        document.getElementById('knowledge-file-fields').style.display = docType === 'FILE' ? '' : 'none';
        document.getElementById('knowledge-link-fields').style.display = docType === 'LINK' ? '' : 'none';
    }

    /**
     * 关闭文档弹窗
     */
    function closeDocModal() {
        document.getElementById('knowledge-doc-modal').style.display = 'none';
    }

    /**
     * 格式化时间
     */
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

    /**
     * HTML 转义
     */
    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * 属性转义
     */
    function escapeAttr(str) {
        if (!str) return '';
        return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
    }

    /**
     * 显示提示消息
     */
    function showToast(message, type = 'success') {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type);
        } else {
            alert(message);
        }
    }

    /**
     * 显示确认对话框
     */
    async function showConfirmDialog(message, icon = '') {
        if (typeof window.showConfirmDialog === 'function') {
            return await window.showConfirmDialog(message, icon);
        }
        return confirm(message);
    }

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('knowledge-management', { init });
        console.log('[KnowledgeManagement] 组件已注册');
    }

    // 暴露全局对象供 HTML 调用
    window.KnowledgeManagement = {
        init,
        loadBases,
        showBaseForm,
        editBase,
        saveBase,
        deleteBase,
        closeBaseModal,
        selectBase,
        toggleBaseChildren,
        loadDocuments,
        showDocForm,
        editDocument,
        saveDoc,
        deleteDocument,
        viewDocument,
        closeDocDetail,
        onDocTypeChange,
        closeDocModal,
        filterByType,
        handleSearch,
        goToPage
    };

})();
