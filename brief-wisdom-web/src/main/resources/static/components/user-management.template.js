/**
 * 用户管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    /**
     * 生成用户管理的完整 HTML 结构
     * @returns {string} HTML 字符串
     */
    function generateHTML() {
        return `
            <section class="settings-section">
                <div class="section-header">
                    <h2>用户管理</h2>
                    <div class="user-filter-bar">
                        <select id="user-level-filter" onchange="UserManagement.loadUsers()">
                            <option value="">全部级别</option>
                            <option value="admin">管理员</option>
                            <option value="vip">会员</option>
                            <option value="normal">普通用户</option>
                        </select>
                        <input type="text" id="user-search-input" placeholder="搜索用户名/昵称"
                               onkeydown="if(event.key==='Enter')UserManagement.loadUsers()">
                        <button class="btn btn-primary btn-sm" onclick="UserManagement.loadUsers()">搜索</button>
                    </div>
                </div>
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>用户名</th>
                            <th>昵称</th>
                            <th>级别</th>
                            <th>注册时间</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody id="user-table-body"></tbody>
                    </table>
                </div>
                <!-- 分页 -->
                <div class="pagination-bar" id="user-pagination"></div>
            </section>

            <!-- 用户角色分配弹窗 -->
            <div id="user-role-modal" class="modal" style="display:none;">
                <div class="modal-content modal-sm">
                    <div class="modal-header">
                        <h3 id="user-role-title">分配角色</h3>
                        <button class="modal-close" onclick="UserManagement.closeUserRoleModal()">×</button>
                    </div>
                    <div class="modal-form">
                        <div id="user-role-checkboxes" class="role-checkbox-list"></div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="UserManagement.closeUserRoleModal()">取消</button>
                            <button type="button" class="btn btn-primary" onclick="UserManagement.saveUserRoles()">保存</button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 用户级别修改弹窗 -->
            <div id="user-level-modal" class="modal" style="display:none;">
                <div class="modal-content modal-sm">
                    <div class="modal-header">
                        <h3>修改用户级别</h3>
                        <button class="modal-close" onclick="UserManagement.closeUserLevelModal()">×</button>
                    </div>
                    <form class="modal-form" onsubmit="UserManagement.saveUserLevel(event)">
                        <input type="hidden" id="user-level-id">
                        <div class="form-group">
                            <label>用户级别</label>
                            <select id="user-level-select">
                                <option value="admin">管理员</option>
                                <option value="vip">会员</option>
                                <option value="normal">普通用户</option>
                            </select>
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="UserManagement.closeUserLevelModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
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
            console.error('[UserManagement.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[UserManagement.Template] 模板已渲染到:', target);
    }

    // 暴露接口
    window.UserManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[UserManagement.Template] 模板生成器已注册');
})();
