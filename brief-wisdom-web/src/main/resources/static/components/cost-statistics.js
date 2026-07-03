/**
 * 费用统计组件 - 业务逻辑
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    let currentDays = 7;
    let statsData = null;

    /**
     * 初始化组件
     */
    async function init(options = {}) {
        console.log('[CostStatistics] 组件初始化', options);

        if (window.CostStatisticsTemplate) {
            const containerId = options.containerId || 'cost-statistics-tab-content';
            const container = document.getElementById(containerId);

            if (container) {
                CostStatisticsTemplate.render(container);
                bindEvents(container);
                await loadData();
            } else {
                console.error('[CostStatistics] 容器不存在:', containerId);
            }
        } else {
            console.error('[CostStatistics] 模板未加载');
        }
    }

    /**
     * 绑定事件
     */
    function bindEvents(container) {
        // 时间范围按钮
        container.querySelectorAll('.cost-range-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                container.querySelectorAll('.cost-range-btn').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                currentDays = parseInt(this.dataset.days);
                loadData();
            });
        });
    }

    /**
     * 刷新数据
     */
    async function refresh() {
        await loadData();
    }

    /**
     * 加载统计数据
     */
    async function loadData() {
        try {
            const response = await fetch('/api/ai/manage/cost-statistics?days=' + currentDays);
            if (!response.ok) throw new Error('HTTP error: ' + response.status);

            const result = await response.json();
            statsData = result.data || result;  // 兼容两种响应格式
            renderSummary();
            renderDateChart();
            renderModelChart();
            renderModelTable();
            renderUserTable();
            console.log('[CostStatistics] 数据加载成功', statsData);
        } catch (error) {
            console.error('[CostStatistics] 加载数据失败:', error);
            showEmpty('数据加载失败: ' + error.message);
        }
    }

    /**
     * 渲染总览卡片
     */
    function renderSummary() {
        if (!statsData) return;

        document.getElementById('cost-total-cost').textContent = formatNumber(statsData.totalCost, 2);
        document.getElementById('cost-total-tokens').textContent = formatNumber(statsData.totalTokens, 0);
        document.getElementById('cost-total-messages').textContent = formatNumber(statsData.totalMessages, 0);

        const avgCost = statsData.totalMessages > 0
            ? (statsData.totalCost / statsData.totalMessages)
            : 0;
        document.getElementById('cost-avg-cost').textContent = formatNumber(avgCost, 4);
    }

    /**
     * 渲染每日费用趋势图（纯 CSS 柱状图）
     */
    function renderDateChart() {
        const chartArea = document.getElementById('cost-date-chart');
        if (!chartArea || !statsData || !statsData.byDate || statsData.byDate.length === 0) {
            if (chartArea) chartArea.innerHTML = '<div class="cost-empty">暂无数据</div>';
            return;
        }

        const data = statsData.byDate;
        const maxCost = Math.max(...data.map(d => d.totalCost), 0.001);

        let html = '<div class="cost-bar-chart">';
        data.forEach(item => {
            const height = Math.max((item.totalCost / maxCost) * 100, 2);
            const dateLabel = item.date.substring(5); // MM-DD
            html += `
                <div class="cost-bar-item" title="${item.date}: ¥${formatNumber(item.totalCost, 2)}">
                    <div class="cost-bar" style="height: ${height}%"></div>
                    <div class="cost-bar-label">${dateLabel}</div>
                </div>
            `;
        });
        html += '</div>';
        chartArea.innerHTML = html;
    }

    /**
     * 渲染模型费用分布图（水平条形图）
     */
    function renderModelChart() {
        const chartArea = document.getElementById('cost-model-chart');
        if (!chartArea || !statsData || !statsData.byModel || statsData.byModel.length === 0) {
            if (chartArea) chartArea.innerHTML = '<div class="cost-empty">暂无数据</div>';
            return;
        }

        const data = statsData.byModel;
        const maxCost = Math.max(...data.map(d => d.totalCost), 0.001);

        let html = '<div class="cost-h-bar-chart">';
        data.forEach(item => {
            const width = Math.max((item.totalCost / maxCost) * 100, 2);
            const modelName = item.model || '未知';
            const shortName = modelName.length > 20 ? modelName.substring(0, 18) + '...' : modelName;
            html += `
                <div class="cost-h-bar-item">
                    <div class="cost-h-bar-label" title="${modelName}">${shortName}</div>
                    <div class="cost-h-bar-track">
                        <div class="cost-h-bar" style="width: ${width}%"></div>
                    </div>
                    <div class="cost-h-bar-value">¥${formatNumber(item.totalCost, 2)}</div>
                </div>
            `;
        });
        html += '</div>';
        chartArea.innerHTML = html;
    }

    /**
     * 渲染模型统计表格
     */
    function renderModelTable() {
        const tbody = document.querySelector('#cost-model-table tbody');
        if (!tbody || !statsData) return;

        const data = statsData.byModel || [];
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="cost-empty">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(item => `
            <tr>
                <td title="${item.model || ''}">${truncate(item.model || '未知', 25)}</td>
                <td>${formatNumber(item.messageCount, 0)}</td>
                <td>${formatNumber(item.totalTokens, 0)}</td>
                <td>¥${formatNumber(item.totalCost, 4)}</td>
            </tr>
        `).join('');
    }

    /**
     * 渲染用户统计表格
     */
    function renderUserTable() {
        const tbody = document.querySelector('#cost-user-table tbody');
        if (!tbody || !statsData) return;

        const data = statsData.byUser || [];
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="cost-empty">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(item => `
            <tr>
                <td>${item.userName || item.userId || '未知'}</td>
                <td>${formatNumber(item.messageCount, 0)}</td>
                <td>${formatNumber(item.totalTokens, 0)}</td>
                <td>¥${formatNumber(item.totalCost, 4)}</td>
            </tr>
        `).join('');
    }

    /**
     * 显示空状态
     */
    function showEmpty(message) {
        const summary = document.getElementById('cost-stats-summary');
        if (summary) {
            summary.innerHTML = '<div class="cost-empty">' + message + '</div>';
        }
    }

    // ===== 工具函数 =====

    function formatNumber(value, decimals) {
        if (value == null || isNaN(value)) return '0';
        return Number(value).toLocaleString('zh-CN', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }

    function truncate(str, maxLen) {
        return str.length > maxLen ? str.substring(0, maxLen) + '...' : str;
    }

    // 导出命名空间
    window.CostStatistics = {
        init,
        refresh
    };

    // 注册组件
    if (typeof registerComponent === 'function') {
        registerComponent('cost-statistics', {
            init,
            destroy: function() {
                statsData = null;
            }
        });
    }

    console.log('[CostStatistics] 费用统计组件加载成功');
})();
