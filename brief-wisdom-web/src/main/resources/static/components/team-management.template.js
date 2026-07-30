/**
 * 团队管理组件 - 模板
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
(function () {
    'use strict';

    function generateHTML() {
        return `
        <div class="team-management">
            <div class="team-header">
                <h3>👥 团队管理</h3>
                <button class="btn-primary" onclick="TeamManagement.showCreateTeam()">+ 创建团队</button>
            </div>

            <!-- 团队列表 -->
            <div class="team-list" id="teamList">
                <div class="loading-placeholder">加载中...</div>
            </div>

            <!-- 团队详情面板（点击团队后展示） -->
            <div class="team-detail-panel" id="teamDetailPanel" style="display:none;">
                <div class="team-detail-header">
                    <button class="btn-back" onclick="TeamManagement.backToList()">← 返回</button>
                    <h4 id="teamDetailName"></h4>
                    <span class="team-role-badge" id="teamDetailRole"></span>
                </div>

                <!-- 团队信息 -->
                <div class="team-info-section" id="teamInfoSection"></div>

                <!-- 成员管理 -->
                <div class="team-section">
                    <div class="section-header">
                        <h5>成员 (<span id="teamMemberCount">0</span>)</h5>
                        <button class="btn-sm" id="btnAddMember" onclick="TeamManagement.showAddMember()" style="display:none;">+ 添加成员</button>
                    </div>
                    <div class="team-member-list" id="teamMemberList"></div>
                </div>

                <!-- 团队知识库 -->
                <div class="team-section">
                    <div class="section-header">
                        <h5>知识库 (<span id="teamKbCount">0</span>)</h5>
                        <button class="btn-sm" id="btnCreateKb" onclick="TeamManagement.showCreateKb()" style="display:none;">+ 创建知识库</button>
                    </div>
                    <div class="team-kb-list" id="teamKbList"></div>
                </div>

                <!-- 共享管理 -->
                <div class="team-section">
                    <div class="section-header">
                        <h5>共享给我的知识库</h5>
                    </div>
                    <div class="team-shared-list" id="teamSharedList"></div>
                </div>
            </div>

            <!-- 创建团队弹窗 -->
            <div class="modal-overlay" id="createTeamModal" style="display:none;">
                <div class="modal-content">
                    <h4>创建团队</h4>
                    <div class="form-group">
                        <label>团队名称 *</label>
                        <input type="text" id="teamNameInput" placeholder="输入团队名称" maxlength="100">
                    </div>
                    <div class="form-group">
                        <label>描述</label>
                        <textarea id="teamDescInput" placeholder="团队描述（可选）" rows="3"></textarea>
                    </div>
                    <div class="form-group">
                        <label>图标</label>
                        <input type="text" id="teamAvatarInput" placeholder="输入 emoji 图标，如 🚀" value="👥" maxlength="10">
                    </div>
                    <div class="modal-actions">
                        <button class="btn-cancel" onclick="TeamManagement.closeCreateTeam()">取消</button>
                        <button class="btn-primary" onclick="TeamManagement.doCreateTeam()">创建</button>
                    </div>
                </div>
            </div>

            <!-- 添加成员弹窗 -->
            <div class="modal-overlay" id="addMemberModal" style="display:none;">
                <div class="modal-content modal-wide">
                    <h4>添加成员</h4>
                    <div class="form-group">
                        <label>搜索用户</label>
                        <input type="text" id="memberSearchInput" placeholder="输入昵称/用户名搜索..." oninput="TeamManagement.filterUserList()">
                    </div>
                    <div class="form-group">
                        <label>选择用户 *</label>
                        <div class="user-select-list" id="memberUserList">
                            <div class="loading-placeholder">加载中...</div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>角色</label>
                        <select id="memberRoleSelect">
                            <option value="member">成员</option>
                            <option value="admin">管理员</option>
                        </select>
                    </div>
                    <div class="modal-actions">
                        <button class="btn-cancel" onclick="TeamManagement.closeAddMember()">取消</button>
                        <button class="btn-primary" onclick="TeamManagement.doAddMember()">添加</button>
                    </div>
                </div>
            </div>

            <!-- 创建知识库弹窗 -->
            <div class="modal-overlay" id="createKbModal" style="display:none;">
                <div class="modal-content">
                    <h4>创建团队知识库</h4>
                    <div class="form-group">
                        <label>名称 *</label>
                        <input type="text" id="kbNameInput" placeholder="知识库名称" maxlength="100">
                    </div>
                    <div class="form-group">
                        <label>描述</label>
                        <input type="text" id="kbDescInput" placeholder="知识库描述（可选）">
                    </div>
                    <div class="form-group">
                        <label>图标</label>
                        <input type="text" id="kbIconInput" value="📚" maxlength="10">
                    </div>
                    <div class="modal-actions">
                        <button class="btn-cancel" onclick="TeamManagement.closeCreateKb()">取消</button>
                        <button class="btn-primary" onclick="TeamManagement.doCreateKb()">创建</button>
                    </div>
                </div>
            </div>

            <!-- 共享知识库弹窗 -->
            <div class="modal-overlay" id="shareKbModal" style="display:none;">
                <div class="modal-content">
                    <h4>共享知识库</h4>
                    <div class="form-group">
                        <label>授权类型</label>
                        <select id="shareTargetType">
                            <option value="team">共享给团队</option>
                            <option value="user">共享给个人</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>目标ID *</label>
                        <input type="text" id="shareTargetIdInput" placeholder="团队ID 或 用户ID">
                    </div>
                    <div class="modal-actions">
                        <button class="btn-cancel" onclick="TeamManagement.closeShareKb()">取消</button>
                        <button class="btn-primary" onclick="TeamManagement.doShareKb()">确认共享</button>
                    </div>
                </div>
            </div>
        </div>
        `;
    }

    window.TeamManagementTemplate = {
        generateHTML: generateHTML
    };
})();
