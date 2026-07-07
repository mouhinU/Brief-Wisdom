/**
 * AI 安全审计日志组件 - 业务逻辑
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let currentPage = 1;
    const pageSize = 20;

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[AuditLog] 组件初始化', options);

        if (window.AuditLogTemplate) {
            const containerId = options.containerId || 'audit-log-tab-content';
            const container = document.getElementById(containerId);

            if (container) {
                AuditLogTemplate.render(container);
                await loadStatistics();
                await loadLogs();
            } else {
                console.error('[AuditLog] 容器不存在:', containerId);
            }
        } else {
            console.error('[AuditLog] 模板未加载');
        }
    }

    /**
     * 加载统计数据
     */
    async function loadStatistics() {
        try {
            const response = await fetch('/api/ai/audit/statistics');
            const data = await response.json();
            
            if (data.success && data.data) {
                const stats = data.data;
                updateElement('audit-total-count', stats.totalCount || '-');
                updateElement('audit-input-blocked-count', stats.inputBlockedCount || '-');
                updateElement('audit-output-filtered-count', stats.outputFilteredCount || '-');
                updateElement('audit-high-risk-count', (stats.highRiskCount + stats.criticalRiskCount) || '-');
            }
        } catch (error) {
            console.error('[AuditLog] 加载统计数据失败:', error);
        }
    }

    /**
     * 加载审计日志
     */
    async function loadLogs(page = 1) {
        currentPage = page;
        const auditType = getElementValue('audit-filter-type');
        const riskLevel = getElementValue('audit-filter-level');
        const userId = getElementValue('audit-filter-user');
        
        // 确保 page 是数字类型
        const pageNum = parseInt(page) || 1;
        
        const url = `/api/ai/audit/logs?page=${pageNum}&size=${pageSize}` +
                   (auditType ? `&auditType=${auditType}` : '') +
                   (riskLevel ? `&riskLevel=${riskLevel}` : '') +
                   (userId ? `&userId=${encodeURIComponent(userId)}` : '');
        
        try {
            const response = await fetch(url);
            const data = await response.json();
            
            if (!data.success || !data.data) {
                throw new Error(data.msg || '加载失败');
            }
            
            renderTable(data.data.records);
            renderPagination(data.data);
        } catch (error) {
            console.error('[AuditLog] 加载审计日志失败:', error);
            const tbody = document.getElementById('audit-table-body');
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="7" class="audit-empty">加载失败: ${escapeHtml(error.message)}</td></tr>`;
            }
        }
    }

    /**
     * 渲染表格
     */
    function renderTable(records) {
        const tbody = document.getElementById('audit-table-body');
        if (!tbody) return;
        
        if (!records || records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="audit-empty">暂无数据</td></tr>';
            return;
        }
        
        tbody.innerHTML = records.map(log => `
            <tr>
                <td>${formatTime(log.createTime)}</td>
                <td><span class="audit-type-badge">${getAuditTypeName(log.auditType)}</span></td>
                <td><span class="audit-risk-badge audit-risk-${log.riskLevel.toLowerCase()}">${getRiskLevelName(log.riskLevel)}</span></td>
                <td>${escapeHtml(log.triggerKeyword || '-')}</td>
                <td>${getActionName(log.actionTaken)}</td>
                <td title="${escapeHtml(log.sessionId)}">${truncate(log.sessionId, 12)}</td>
                <td title="${escapeHtml(log.userId)}">${truncate(log.userId, 12)}</td>
            </tr>
        `).join('');
    }

    /**
     * 渲染分页
     */
    function renderPagination(pageData) {
        const pagination = document.getElementById('audit-pagination');
        if (!pagination) return;
        
        const { page: current, pages } = pageData;
        
        if (pages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        
        let html = '';
        
        // 上一页
        html += `<button ${current === 1 ? 'disabled' : ''} onclick="AuditLog.loadLogs(${current - 1})">上一页</button>`;
        
        // 页码
        const startPage = Math.max(1, current - 2);
        const endPage = Math.min(pages, current + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            html += `<button class="${i === current ? 'active' : ''}" onclick="AuditLog.loadLogs(${i})">${i}</button>`;
        }
        
        // 下一页
        html += `<button ${current === pages ? 'disabled' : ''} onclick="AuditLog.loadLogs(${current + 1})">下一页</button>`;
        
        pagination.innerHTML = html;
    }

    /**
     * 刷新数据
     */
    async function refresh() {
        await loadStatistics();
        await loadLogs(currentPage);
    }

    // ==================== 工具函数 ====================

    function updateElement(id, value) {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    }

    function getElementValue(id) {
        const el = document.getElementById(id);
        return el ? el.value : '';
    }

    function formatTime(timeStr) {
        if (!timeStr) return '-';
        try {
            const date = new Date(timeStr);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        } catch (e) {
            return timeStr;
        }
    }

    function getAuditTypeName(type) {
        const types = {
            'INPUT_BLOCKED': '输入拦截',
            'OUTPUT_FILTERED': '输出过滤',
            'RISK_DETECTED': '风险检测'
        };
        return types[type] || type;
    }

    function getRiskLevelName(level) {
        const levels = {
            'LOW': '低',
            'MEDIUM': '中',
            'HIGH': '高',
            'CRITICAL': '严重'
        };
        return levels[level] || level;
    }

    function getActionName(action) {
        const actions = {
            'BLOCKED': '拦截',
            'FILTERED': '过滤',
            'WARNED': '警告',
            'ALLOWED': '放行'
        };
        return actions[action] || action;
    }

    function escapeHtml(text) {
        if (!text) return '-';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function truncate(str, len) {
        if (!str) return '-';
        return str.length > len ? str.substring(0, len) + '...' : str;
    }

    // 暴露全局 API
    window.AuditLog = {
        init,
        loadLogs,
        loadStatistics,
        refresh
    };

    // 自动注册到组件系统（如果 component-loader.js 已加载）
    if (typeof registerComponent === 'function') {
        registerComponent('audit-log', {
            init: function(options) {
                return init(options);
            },
            destroy: function() {
                console.log('[AuditLog] 组件销毁');
            }
        });
    }

    console.log('[AuditLog] AI 安全审计日志组件加载成功');
})();
