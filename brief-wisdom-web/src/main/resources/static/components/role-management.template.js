/**
 * 角色管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    /**
     * 生成角色管理的完整 HTML 结构
     * @returns {string} HTML 字符串
     */
    function generateHTML() {
        return `
            <section class="settings-section">
                <div class="section-header">
                    <h2>角色管理</h2>
                    <button class="btn btn-primary" onclick="RoleManagement.showRoleForm()">+ 新增角色</button>
                </div>
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>角色名称</th>
                            <th>角色标识</th>
                            <th>描述</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody id="role-table-body"></tbody>
                    </table>
                </div>
            </section>

            <!-- 角色表单弹窗 -->
            <div id="role-modal" class="modal" style="display:none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="role-modal-title">新增角色</h3>
                        <button class="modal-close" onclick="RoleManagement.closeRoleModal()">×</button>
                    </div>
                    <form id="role-form" class="modal-form" onsubmit="RoleManagement.saveRole(event)">
                        <input type="hidden" id="role-id">
                        <div class="form-group">
                            <label>角色名称 *</label>
                            <input type="text" id="role-name" required placeholder="例如：超级管理员">
                        </div>
                        <div class="form-group">
                            <label>角色标识 *</label>
                            <input type="text" id="role-key" required placeholder="例如：super_admin">
                        </div>
                        <div class="form-group">
                            <label>描述</label>
                            <input type="text" id="role-desc" placeholder="角色描述">
                        </div>
                        <div class="form-group">
                            <label>状态</label>
                            <select id="role-status">
                                <option value="1">启用</option>
                                <option value="0">禁用</option>
                            </select>
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="RoleManagement.closeRoleModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- 角色菜单权限弹窗 -->
            <div id="menu-perm-modal" class="modal" style="display:none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="menu-perm-title">分配菜单权限</h3>
                        <button class="modal-close" onclick="RoleManagement.closeMenuPermModal()">×</button>
                    </div>
                    <div class="modal-form">
                        <div class="menu-tree-actions">
                            <button type="button" class="btn btn-sm btn-secondary" onclick="RoleManagement.checkAllMenus(true)">全选</button>
                            <button type="button" class="btn btn-sm btn-secondary" onclick="RoleManagement.checkAllMenus(false)">取消全选</button>
                        </div>
                        <div id="menu-perm-tree" class="menu-perm-tree"></div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="RoleManagement.closeMenuPermModal()">取消</button>
                            <button type="button" class="btn btn-primary" onclick="RoleManagement.saveMenuPermissions()">保存</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 渲染组件到指定容器
     * @param {string|HTMLElement} container - 容器选择器或 DOM 元素
     */
    function render(container) {
        const target = typeof container === 'string' 
            ? document.querySelector(container) 
            : container;
        
        if (!target) {
            console.error('[RoleManagement.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[RoleManagement.Template] 模板已渲染到:', target);
    }

    // 暴露接口
    window.RoleManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[RoleManagement.Template] 模板生成器已注册');
})();
