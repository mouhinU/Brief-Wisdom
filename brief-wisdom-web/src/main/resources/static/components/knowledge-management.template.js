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
                            <button class="btn btn-primary" id="knowledge-add-doc-btn" onclick="KnowledgeManagement.showDocForm()" style="display:none;">+ 新建文档</button>
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
                        <div id="knowledge-internal-fields">
                            <div class="knowledge-form-group">
                                <label>文档内容</label>
                                <textarea id="knowledge-doc-content" rows="15" class="knowledge-content-editor" placeholder="请输入文档内容（支持HTML）"></textarea>
                            </div>
                        </div>
                        <div id="knowledge-file-fields" style="display:none;">
                            <div class="knowledge-form-group">
                                <label>文件URL *</label>
                                <input type="text" id="knowledge-doc-file-url" placeholder="文件存储URL">
                            </div>
                            <div class="knowledge-form-group">
                                <label>文件名</label>
                                <input type="text" id="knowledge-doc-file-name" placeholder="显示的文件名">
                            </div>
                        </div>
                        <div id="knowledge-link-fields" style="display:none;">
                            <div class="knowledge-form-group">
                                <label>链接URL *</label>
                                <input type="url" id="knowledge-doc-link-url" required placeholder="https://example.com/document">
                            </div>
                        </div>
                        <div class="knowledge-form-group">
                            <label>标签</label>
                            <input type="text" id="knowledge-doc-tags" placeholder="用逗号分隔多个标签">
                        </div>
                        <div class="knowledge-form-actions">
                            <button type="button" class="btn btn-secondary" onclick="KnowledgeManagement.closeDocModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
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
