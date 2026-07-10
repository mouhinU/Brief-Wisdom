/**
 * 知识库管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    /**
     * 生成知识库管理的完整 HTML 结构
     * @returns {string} HTML 字符串
     */
    function generateHTML() {
        return `
            <div class="knowledge-layout">
                <!-- 左侧：知识库列表 -->
                <div class="knowledge-sidebar">
                    <div class="knowledge-sidebar-header">
                        <h3>知识库</h3>
                        <button class="btn-new-base" onclick="KnowledgeManagement.showBaseForm()">+ 新建</button>
                    </div>
                    <div id="knowledge-base-list" class="knowledge-base-list">
                        <div class="knowledge-empty-hint">加载中...</div>
                    </div>
                </div>

                <!-- 右侧：文档列表 -->
                <div class="knowledge-main-content">
                    <div class="knowledge-content-header">
                        <div class="knowledge-content-title">
                            <h2 id="knowledge-current-base-name">请选择知识库</h2>
                            <span id="knowledge-doc-count" class="knowledge-count-badge">0</span>
                        </div>
                        <div class="knowledge-content-actions">
                            <input type="text" id="knowledge-search-input" class="knowledge-search-input" placeholder="搜索文档..."
                                   onkeyup="KnowledgeManagement.handleSearch(event)">
                            <div class="knowledge-doc-type-filter">
                                <button class="knowledge-filter-btn active" data-type="" onclick="KnowledgeManagement.filterByType('')">全部</button>
                                <button class="knowledge-filter-btn" data-type="INTERNAL" onclick="KnowledgeManagement.filterByType('INTERNAL')">📝 内部文档</button>
                                <button class="knowledge-filter-btn" data-type="FILE" onclick="KnowledgeManagement.filterByType('FILE')">📎 文件</button>
                                <button class="knowledge-filter-btn" data-type="LINK" onclick="KnowledgeManagement.filterByType('LINK')">🔗 外部链接</button>
                            </div>
                            <button class="btn btn-secondary" id="knowledge-import-md-btn" onclick="KnowledgeManagement.showImportMdModal()" style="display:none;">📥 导入 MD</button>
                            <button class="btn btn-primary" id="knowledge-add-doc-btn" onclick="KnowledgeManagement.showDocForm()" style="display:none;">+ 新建文档</button>
                            <button class="btn btn-danger" id="knowledge-batch-delete-btn" onclick="KnowledgeManagement.batchDeleteDocuments()" style="display:none;">🗑️ 批量删除</button>
                        </div>
                    </div>

                    <!-- 文档列表 -->
                    <div id="knowledge-document-list" class="knowledge-document-list">
                        <div class="knowledge-empty-hint">请从左侧选择一个知识库</div>
                    </div>

                    <!-- 分页 -->
                    <div id="knowledge-pagination" class="knowledge-pagination" style="display:none;"></div>
                </div>
            </div>

            <!-- 知识库表单弹窗 -->
            <div id="knowledge-base-modal" class="knowledge-modal" style="display:none;">
                <div class="knowledge-modal-content">
                    <div class="knowledge-modal-header">
                        <h3 id="knowledge-base-modal-title">新建知识库</h3>
                        <button class="knowledge-modal-close" onclick="KnowledgeManagement.closeBaseModal()">&times;</button>
                    </div>
                    <form id="knowledge-base-form" class="knowledge-modal-form" onsubmit="KnowledgeManagement.saveBase(event)">
                        <input type="hidden" id="knowledge-base-id">
                        <div class="knowledge-form-group">
                            <label>名称 *</label>
                            <input type="text" id="knowledge-base-name" required placeholder="知识库名称">
                        </div>
                        <div class="knowledge-form-group">
                            <label>描述</label>
                            <textarea id="knowledge-base-description" rows="3" placeholder="知识库描述"></textarea>
                        </div>
                        <div class="knowledge-form-group">
                            <label>图标</label>
                            <input type="text" id="knowledge-base-icon" value="📚" placeholder="emoji 图标">
                        </div>
                        <div class="knowledge-form-row">
                            <div class="knowledge-form-group">
                                <label>排序</label>
                                <input type="number" id="knowledge-base-sort-order" value="0" min="0">
                            </div>
                            <div class="knowledge-form-group">
                                <label>是否公开</label>
                                <select id="knowledge-base-is-public">
                                    <option value="0">私有</option>
                                    <option value="1">公开</option>
                                </select>
                            </div>
                        </div>
                        <div class="knowledge-form-group">
                            <label>父级知识库</label>
                            <select id="knowledge-base-parent-id">
                                <option value="0">无（顶级知识库）</option>
                            </select>
                        </div>
                        <div class="knowledge-form-actions">
                            <button type="button" class="btn btn-secondary" onclick="KnowledgeManagement.closeBaseModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 文档表单弹窗 -->
            <div id="knowledge-doc-modal" class="knowledge-modal" style="display:none;">
                <div class="knowledge-modal-content knowledge-modal-large">
                    <div class="knowledge-modal-header">
                        <h3 id="knowledge-doc-modal-title">新建文档</h3>
                        <button class="knowledge-modal-close" onclick="KnowledgeManagement.closeDocModal()">&times;</button>
                    </div>
                    <form id="knowledge-doc-form" class="knowledge-modal-form" onsubmit="KnowledgeManagement.saveDoc(event)">
                        <input type="hidden" id="knowledge-doc-id">
                        <div class="knowledge-form-group">
                            <label>文档类型 *</label>
                            <div class="knowledge-doc-type-selector">
                                <label class="knowledge-doc-type-option">
                                    <input type="radio" name="docType" value="INTERNAL" checked onchange="KnowledgeManagement.onDocTypeChange()">
                                    <span class="knowledge-doc-type-card">
                                        <span class="knowledge-doc-type-icon">📝</span>
                                        <span class="knowledge-doc-type-name">内部文档</span>
                                        <span class="knowledge-doc-type-desc">富文本编辑</span>
                                    </span>
                                </label>
                                <label class="knowledge-doc-type-option">
                                    <input type="radio" name="docType" value="FILE" onchange="KnowledgeManagement.onDocTypeChange()">
                                    <span class="knowledge-doc-type-card">
                                        <span class="knowledge-doc-type-icon">📎</span>
                                        <span class="knowledge-doc-type-name">文件上传</span>
                                        <span class="knowledge-doc-type-desc">PDF/Word等</span>
                                    </span>
                                </label>
                                <label class="knowledge-doc-type-option">
                                    <input type="radio" name="docType" value="LINK" onchange="KnowledgeManagement.onDocTypeChange()">
                                    <span class="knowledge-doc-type-card">
                                        <span class="knowledge-doc-type-icon">🔗</span>
                                        <span class="knowledge-doc-type-name">外部链接</span>
                                        <span class="knowledge-doc-type-desc">引用外部文档</span>
                                    </span>
                                </label>
                            </div>
                        </div>
                        <div class="knowledge-form-group">
                            <label>标题 *</label>
                            <input type="text" id="knowledge-doc-title" required placeholder="文档标题">
                        </div>
                        <!-- 内部文档字段 -->
                        <div id="knowledge-internal-fields">
                            <div class="knowledge-form-group">
                                <label>文档内容</label>
                                <textarea id="knowledge-doc-content" rows="15" class="knowledge-content-editor" placeholder="请输入文档内容（支持HTML）"></textarea>
                            </div>
                        </div>
                        <!-- 文件类型字段 -->
                        <div id="knowledge-file-fields" style="display:none;">
                            <div class="knowledge-form-group">
                                <label>文件URL *</label>
                                <input type="text" id="knowledge-doc-file-url" placeholder="文件存储URL">
                            </div>
                            <div class="knowledge-form-row">
                                <div class="knowledge-form-group">
                                    <label>文件名</label>
                                    <input type="text" id="knowledge-doc-file-name" placeholder="原始文件名">
                                </div>
                                <div class="knowledge-form-group">
                                    <label>文件大小(字节)</label>
                                    <input type="number" id="knowledge-doc-file-size" placeholder="文件大小">
                                </div>
                            </div>
                            <div class="knowledge-form-group">
                                <label>文件类型</label>
                                <input type="text" id="knowledge-doc-file-type" placeholder="如: application/pdf">
                            </div>
                        </div>
                        <!-- 外部链接字段 -->
                        <div id="knowledge-link-fields" style="display:none;">
                            <div class="knowledge-form-group">
                                <label>链接URL *</label>
                                <div class="knowledge-link-url-row">
                                    <input type="text" id="knowledge-doc-link-url" placeholder="https://..."
                                           onblur="KnowledgeManagement.autoFetchUrlMetadata()">
                                    <button type="button" class="btn btn-fetch-url" id="knowledge-fetch-url-btn"
                                            onclick="KnowledgeManagement.autoFetchUrlMetadata()" title="自动获取链接描述">
                                        🔗 自动获取
                                    </button>
                                </div>
                                <div id="knowledge-url-fetch-status" class="knowledge-url-fetch-status" style="display:none;"></div>
                            </div>
                            <div class="knowledge-form-group">
                                <label>链接描述 <span class="knowledge-label-hint">（支持 Markdown 格式）</span></label>
                                <textarea id="knowledge-doc-link-desc" rows="6" class="knowledge-link-desc-editor"
                                          placeholder="链接描述，支持 Markdown 格式..."></textarea>
                                <div class="knowledge-markdown-preview-toggle">
                                    <button type="button" class="btn-toggle-preview" onclick="KnowledgeManagement.toggleLinkDescPreview()">
                                        👁 预览
                                    </button>
                                </div>
                                <div id="knowledge-link-desc-preview" class="knowledge-markdown-preview" style="display:none;"></div>
                            </div>
                        </div>
                        <div class="knowledge-form-row">
                            <div class="knowledge-form-group">
                                <label>标签</label>
                                <input type="text" id="knowledge-doc-tags" placeholder="标签，逗号分隔">
                            </div>
                            <div class="knowledge-form-group">
                                <label>状态</label>
                                <select id="knowledge-doc-status">
                                    <option value="1">已发布</option>
                                    <option value="0">草稿</option>
                                    <option value="2">已归档</option>
                                </select>
                            </div>
                        </div>
                        <div class="knowledge-form-actions">
                            <button type="button" class="btn btn-secondary" onclick="KnowledgeManagement.closeDocModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 文档详情弹窗 -->
            <div id="knowledge-doc-detail-modal" class="knowledge-modal" style="display:none;">
                <div class="knowledge-modal-content knowledge-modal-large">
                    <div class="knowledge-modal-header">
                        <h3 id="knowledge-doc-detail-title">文档详情</h3>
                        <button class="knowledge-modal-close" onclick="KnowledgeManagement.closeDocDetail()">&times;</button>
                    </div>
                    <div id="knowledge-doc-detail-body" class="knowledge-doc-detail-body"></div>
                </div>
            </div>

            <!-- Markdown 导入弹窗 -->
            <div id="knowledge-import-md-modal" class="knowledge-modal" style="display:none;">
                <div class="knowledge-modal-content">
                    <div class="knowledge-modal-header">
                        <h3>导入 Markdown 文件</h3>
                        <button class="knowledge-modal-close" onclick="KnowledgeManagement.closeImportMdModal()">&times;</button>
                    </div>
                    <div class="knowledge-import-md-form">
                        <div class="knowledge-form-group">
                            <label>导入方式</label>
                            <select id="knowledge-import-md-mode" onchange="KnowledgeManagement.onImportModeChange()">
                                <option value="docs">导入 docs 目录（推荐）</option>
                                <option value="agents">导入 AGENTS.md</option>
                                <option value="custom">自定义目录</option>
                            </select>
                        </div>
                        <div id="knowledge-import-custom-dir" style="display:none;">
                            <div class="knowledge-form-group">
                                <label>源目录路径</label>
                                <input type="text" id="knowledge-import-source-dir" placeholder="如: docs/guides" value="docs">
                            </div>
                            <div class="knowledge-form-group">
                                <label>
                                    <input type="checkbox" id="knowledge-import-recursive" checked>
                                    递归子目录
                                </label>
                            </div>
                        </div>
                        <div class="knowledge-form-info">
                            <p>💡 提示：</p>
                            <ul>
                                <li>Markdown 文件将作为“内部文档”导入</li>
                                <li>文件名（不含扩展名）将作为文档标题</li>
                                <li>文件内容将原样保存为 Markdown 格式</li>
                            </ul>
                        </div>
                        <div class="knowledge-form-actions">
                            <button type="button" class="btn btn-secondary" onclick="KnowledgeManagement.closeImportMdModal()">取消</button>
                            <button type="button" class="btn btn-primary" onclick="KnowledgeManagement.importMarkdown()">开始导入</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 渲染到指定容器
     * @param {HTMLElement} container - 目标容器元素
     */
    function render(container) {
        if (!container) {
            console.error('[KnowledgeTemplate] 容器不存在');
            return;
        }
        
        // 保存 active 类
        const isActive = container.classList.contains('active');
        
        // 渲染 HTML
        container.innerHTML = generateHTML();
        
        // 恢复 active 类
        if (isActive) {
            container.classList.add('active');
        }
        
        console.log('[KnowledgeTemplate] HTML 渲染完成');
    }

    // 暴露给全局
    window.KnowledgeManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[KnowledgeTemplate] 知识库管理模板加载成功');
})();
