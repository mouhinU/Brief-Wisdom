/**
 * 团队管理组件 - 逻辑
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
(function () {
    'use strict';

    let currentTeamId = null;
    let currentShareKbId = null;

    const API_BASE = '/api/team';

    async function apiFetch(url, options = {}) {
        const resp = await fetch(url, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        return resp.json();
    }

    // ===== 初始化 =====

    function init() {
        const container = document.getElementById('team-tab-content');
        if (!container) return;
        if (window.TeamManagementTemplate) {
            container.innerHTML = window.TeamManagementTemplate.generateHTML();
        }
        loadMyTeams();
    }

    // ===== 团队列表 =====

    async function loadMyTeams() {
        const listEl = document.getElementById('teamList');
        if (!listEl) return;
        try {
            const result = await apiFetch(`${API_BASE}/my`);
            if (result.success && result.data) {
                if (result.data.length === 0) {
                    listEl.innerHTML = '<div class="empty-state">暂无团队，点击右上角创建</div>';
                    return;
                }
                listEl.innerHTML = result.data.map(team => `
                    <div class="team-card" onclick="TeamManagement.openTeam(${team.id})">
                        <span class="team-avatar">${team.avatar || '👥'}</span>
                        <div class="team-card-info">
                            <div class="team-card-name">${escapeHtml(team.name)}</div>
                            <div class="team-card-meta">${team.memberCount || 0} 成员 · ${team.kbCount || 0} 知识库 · ${team.myRole || ''}</div>
                        </div>
                    </div>
                `).join('');
            } else {
                listEl.innerHTML = '<div class="empty-state">加载失败</div>';
            }
        } catch (e) {
            listEl.innerHTML = '<div class="empty-state">网络错误</div>';
        }
    }

    // ===== 团队详情 =====

    async function openTeam(teamId) {
        currentTeamId = teamId;
        document.getElementById('teamList').style.display = 'none';
        document.querySelector('.team-header').style.display = 'none';
        document.getElementById('teamDetailPanel').style.display = 'block';

        try {
            const result = await apiFetch(`${API_BASE}/${teamId}`);
            if (!result.success) return;
            const team = result.data;

            document.getElementById('teamDetailName').textContent = `${team.avatar || ''} ${team.name}`;
            document.getElementById('teamDetailRole').textContent = roleLabel(team.myRole);

            const isAdmin = team.myRole === 'owner' || team.myRole === 'admin';
            document.getElementById('btnAddMember').style.display = isAdmin ? 'inline-block' : 'none';
            document.getElementById('btnCreateKb').style.display = isAdmin ? 'inline-block' : 'none';

            // 渲染成员
            renderMembers(team.members || []);

            // 加载团队知识库
            loadTeamKbs(teamId);

            // 加载共享给我的
            loadSharedWithMe();
        } catch (e) {
            console.error('[TeamManagement] 加载团队详情失败:', e);
        }
    }

    function renderMembers(members) {
        document.getElementById('teamMemberCount').textContent = members.length;
        const listEl = document.getElementById('teamMemberList');
        listEl.innerHTML = members.map(m => `
            <div class="member-item">
                <span class="member-id">${escapeHtml(m.userId)}</span>
                <span class="member-role role-${m.role}">${roleLabel(m.role)}</span>
            </div>
        `).join('');
    }

    async function loadTeamKbs(teamId) {
        try {
            const result = await apiFetch(`${API_BASE}/${teamId}/knowledge-bases`);
            const kbs = result.success ? result.data : [];
            document.getElementById('teamKbCount').textContent = kbs.length;
            const listEl = document.getElementById('teamKbList');
            if (kbs.length === 0) {
                listEl.innerHTML = '<div class="empty-state">暂无知识库</div>';
                return;
            }
            listEl.innerHTML = kbs.map(kb => `
                <div class="kb-item">
                    <span class="kb-icon">${kb.icon || '📚'}</span>
                    <span class="kb-name">${escapeHtml(kb.name)}</span>
                    <button class="btn-xs" onclick="TeamManagement.showShareKb(${kb.id})">共享</button>
                </div>
            `).join('');
        } catch (e) {
            console.error('[TeamManagement] 加载团队知识库失败:', e);
        }
    }

    async function loadSharedWithMe() {
        try {
            const result = await apiFetch(`${API_BASE}/knowledge-bases/shared-with-me`);
            const kbs = result.success ? result.data : [];
            const listEl = document.getElementById('teamSharedList');
            if (kbs.length === 0) {
                listEl.innerHTML = '<div class="empty-state">暂无共享知识库</div>';
                return;
            }
            listEl.innerHTML = kbs.map(kb => `
                <div class="kb-item shared">
                    <span class="kb-icon">${kb.icon || '📚'}</span>
                    <span class="kb-name">${escapeHtml(kb.name)}</span>
                    <span class="kb-badge">共享</span>
                </div>
            `).join('');
        } catch (e) {
            console.error('[TeamManagement] 加载共享知识库失败:', e);
        }
    }

    function backToList() {
        currentTeamId = null;
        document.getElementById('teamDetailPanel').style.display = 'none';
        document.getElementById('teamList').style.display = 'block';
        document.querySelector('.team-header').style.display = 'flex';
        loadMyTeams();
    }

    // ===== 创建团队 =====

    function showCreateTeam() { document.getElementById('createTeamModal').style.display = 'flex'; }
    function closeCreateTeam() { document.getElementById('createTeamModal').style.display = 'none'; }

    async function doCreateTeam() {
        const name = document.getElementById('teamNameInput').value.trim();
        if (!name) { alert('请输入团队名称'); return; }
        const description = document.getElementById('teamDescInput').value.trim();
        const avatar = document.getElementById('teamAvatarInput').value.trim() || '👥';

        try {
            const result = await apiFetch(API_BASE, {
                method: 'POST',
                body: JSON.stringify({ name, description, avatar })
            });
            if (result.success) {
                closeCreateTeam();
                document.getElementById('teamNameInput').value = '';
                document.getElementById('teamDescInput').value = '';
                loadMyTeams();
            } else {
                alert(result.error || '创建失败');
            }
        } catch (e) {
            alert('网络错误: ' + e.message);
        }
    }

    // ===== 添加成员 =====

    let allUsersCache = [];
    let selectedMemberUserId = null;

    async function showAddMember() {
        document.getElementById('addMemberModal').style.display = 'flex';
        selectedMemberUserId = null;
        document.getElementById('memberSearchInput').value = '';
        await loadUserList();
    }

    function closeAddMember() { document.getElementById('addMemberModal').style.display = 'none'; }

    async function loadUserList() {
        const listEl = document.getElementById('memberUserList');
        try {
            const resp = await fetch('/api/user/list?page=1&size=100');
            const result = await resp.json();
            if (result.success && result.data && result.data.records) {
                allUsersCache = result.data.records;
            } else if (result.data && result.data.list) {
                allUsersCache = result.data.list;
            } else {
                allUsersCache = [];
            }
            renderUserList(allUsersCache);
        } catch (e) {
            listEl.innerHTML = '<div class="empty-state">加载用户列表失败</div>';
        }
    }

    function renderUserList(users) {
        const listEl = document.getElementById('memberUserList');
        if (!users || users.length === 0) {
            listEl.innerHTML = '<div class="empty-state">无匹配用户</div>';
            return;
        }
        listEl.innerHTML = users.map(u => `
            <div class="user-select-item ${selectedMemberUserId === u.userId ? 'selected' : ''}"
                 onclick="TeamManagement.selectMember('${escapeHtml(u.userId)}')"
                 data-userid="${escapeHtml(u.userId)}">
                <span class="user-avatar-sm">${u.avatar || '👤'}</span>
                <span class="user-nick">${escapeHtml(u.nickname || u.username || u.userId)}</span>
                <span class="user-id-tag">${escapeHtml(u.userId)}</span>
            </div>
        `).join('');
    }

    function selectMember(userId) {
        selectedMemberUserId = userId;
        // 更新选中样式
        document.querySelectorAll('#memberUserList .user-select-item').forEach(el => {
            el.classList.toggle('selected', el.dataset.userid === userId);
        });
    }

    function filterUserList() {
        const keyword = (document.getElementById('memberSearchInput').value || '').toLowerCase().trim();
        if (!keyword) {
            renderUserList(allUsersCache);
            return;
        }
        const filtered = allUsersCache.filter(u =>
            (u.nickname || '').toLowerCase().includes(keyword) ||
            (u.username || '').toLowerCase().includes(keyword) ||
            (u.userId || '').toLowerCase().includes(keyword)
        );
        renderUserList(filtered);
    }

    async function doAddMember() {
        if (!selectedMemberUserId) { alert('请从列表中选择用户'); return; }
        const role = document.getElementById('memberRoleSelect').value;

        try {
            const result = await apiFetch(`${API_BASE}/${currentTeamId}/members`, {
                method: 'POST',
                body: JSON.stringify({ userId: selectedMemberUserId, role })
            });
            if (result.success) {
                closeAddMember();
                openTeam(currentTeamId);
            } else {
                alert(result.error || '添加失败');
            }
        } catch (e) {
            alert('网络错误: ' + e.message);
        }
    }

    // ===== 创建知识库 =====

    function showCreateKb() { document.getElementById('createKbModal').style.display = 'flex'; }
    function closeCreateKb() { document.getElementById('createKbModal').style.display = 'none'; }

    async function doCreateKb() {
        const name = document.getElementById('kbNameInput').value.trim();
        if (!name) { alert('请输入知识库名称'); return; }
        const description = document.getElementById('kbDescInput').value.trim();
        const icon = document.getElementById('kbIconInput').value.trim() || '📚';

        try {
            const result = await apiFetch(`${API_BASE}/${currentTeamId}/knowledge-bases`, {
                method: 'POST',
                body: JSON.stringify({ name, description, icon })
            });
            if (result.success) {
                closeCreateKb();
                document.getElementById('kbNameInput').value = '';
                document.getElementById('kbDescInput').value = '';
                loadTeamKbs(currentTeamId);
            } else {
                alert(result.error || '创建失败');
            }
        } catch (e) {
            alert('网络错误: ' + e.message);
        }
    }

    // ===== 共享知识库 =====

    function showShareKb(kbId) {
        currentShareKbId = kbId;
        document.getElementById('shareKbModal').style.display = 'flex';
    }
    function closeShareKb() { document.getElementById('shareKbModal').style.display = 'none'; }

    async function doShareKb() {
        const targetType = document.getElementById('shareTargetType').value;
        const targetId = document.getElementById('shareTargetIdInput').value.trim();
        if (!targetId) { alert('请输入目标ID'); return; }

        try {
            const result = await apiFetch(`${API_BASE}/knowledge-bases/${currentShareKbId}/shares`, {
                method: 'POST',
                body: JSON.stringify({ targetType, targetId })
            });
            if (result.success) {
                closeShareKb();
                document.getElementById('shareTargetIdInput').value = '';
                alert('共享成功');
            } else {
                alert(result.error || '共享失败');
            }
        } catch (e) {
            alert('网络错误: ' + e.message);
        }
    }

    // ===== 工具函数 =====

    function roleLabel(role) {
        const map = { owner: '拥有者', admin: '管理员', member: '成员' };
        return map[role] || role || '';
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ===== 导出 =====

    window.TeamManagement = {
        init,
        showCreateTeam,
        closeCreateTeam,
        doCreateTeam,
        openTeam,
        backToList,
        showAddMember,
        closeAddMember,
        doAddMember,
        selectMember,
        filterUserList,
        showCreateKb,
        closeCreateKb,
        doCreateKb,
        showShareKb,
        closeShareKb,
        doShareKb
    };

    // 自动初始化（组件加载后调用）
    if (document.getElementById('team-tab-content')) {
        init();
    }
})();
