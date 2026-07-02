/**
 * 菜单管理组件 - HTML 模板生成器
 * 
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <section class="settings-section">
                <div class="section-header">
                    <h2>菜单管理</h2>
                    <button class="btn btn-primary" onclick="MenuManagement.showMenuForm()">+ 新增菜单</button>
                </div>
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <th>排序</th>
                            <th>图标</th>
                            <th>菜单名称</th>
                            <th>类型</th>
                            <th>父级菜单</th>
                            <th>链接</th>
                            <th>显示</th>
                            <th>操作</th>
                        </tr>
                        </thead>
                        <tbody id="menu-table-body"></tbody>
                    </table>
                </div>
            </section>

            <!-- 菜单表单弹窗 -->
            <div id="modal" class="modal" style="display:none;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3 id="modal-title">新增菜单</h3>
                        <button class="modal-close" onclick="MenuManagement.closeModal()">×</button>
                    </div>
                    <form id="menu-form" class="modal-form" onsubmit="MenuManagement.saveMenu(event)">
                        <input type="hidden" id="menu-id">
                        <div class="form-group">
                            <label>菜单类型</label>
                            <select id="menu-type" onchange="MenuManagement.onMenuTypeChange()">
                                <option value="0">目录</option>
                                <option value="1" selected>菜单</option>
                                <option value="2">按钮</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>父级菜单</label>
                            <select id="menu-parent">
                                <option value="0">顶级菜单</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>菜单名称 *</label>
                            <input type="text" id="menu-name" required placeholder="例如：首页">
                        </div>
                        <div class="form-group" id="menu-url-group">
                            <label>链接</label>
                            <input type="text" id="menu-url" placeholder="例如：/ 或 /about.html">
                        </div>
                        <div class="form-group" id="menu-perm-group" style="display:none;">
                            <label>权限标识</label>
                            <input type="text" id="menu-permission" placeholder="例如：user:create">
                        </div>
                        <div class="form-group">
                            <label>图标</label>
                            <input type="text" id="menu-icon" placeholder="Emoji 或图标类名，例如：🏠">
                        </div>
                        <div class="form-group">
                            <label>排序序号</label>
                            <input type="number" id="menu-sort" value="0" min="0">
                        </div>
                        <div class="form-group" id="menu-target-group">
                            <label>打开方式</label>
                            <select id="menu-target">
                                <option value="_self">当前窗口</option>
                                <option value="_blank">新窗口</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>是否显示</label>
                            <select id="menu-visible">
                                <option value="1">显示</option>
                                <option value="0">隐藏</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>需要登录</label>
                            <select id="menu-require-login">
                                <option value="0">否</option>
                                <option value="1">是</option>
                            </select>
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="MenuManagement.closeModal()">取消</button>
                            <button type="submit" class="btn btn-primary">保存</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
    }

    function render(container) {
        const target = typeof container === 'string' 
            ? document.querySelector(container) 
            : container;
        
        if (!target) {
            console.error('[MenuManagement.Template] 找不到容器:', container);
            return;
        }
        
        target.innerHTML = generateHTML();
        console.log('[MenuManagement.Template] 模板已渲染到:', target);
    }

    window.MenuManagementTemplate = {
        generateHTML,
        render
    };

    console.log('[MenuManagement.Template] 模板生成器已注册');
})();
