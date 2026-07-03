/**
 * 费用统计组件 - HTML 模板生成器
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <div class="cost-stats-container">
                <!-- 时间范围选择 -->
                <div class="cost-stats-toolbar">
                    <h3>费用统计</h3>
                    <div class="cost-stats-range">
                        <button class="cost-range-btn active" data-days="7">近7天</button>
                        <button class="cost-range-btn" data-days="30">近30天</button>
                        <button class="cost-range-btn" data-days="90">近90天</button>
                    </div>
                    <button class="cost-refresh-btn" onclick="CostStatistics.refresh()">刷新</button>
                </div>

                <!-- 总览卡片 -->
                <div class="cost-stats-summary" id="cost-stats-summary">
                    <div class="cost-summary-card">
                        <div class="cost-summary-label">总费用</div>
                        <div class="cost-summary-value" id="cost-total-cost">--</div>
                        <div class="cost-summary-unit">元</div>
                    </div>
                    <div class="cost-summary-card">
                        <div class="cost-summary-label">总 Token</div>
                        <div class="cost-summary-value" id="cost-total-tokens">--</div>
                        <div class="cost-summary-unit">tokens</div>
                    </div>
                    <div class="cost-summary-card">
                        <div class="cost-summary-label">AI 回复数</div>
                        <div class="cost-summary-value" id="cost-total-messages">--</div>
                        <div class="cost-summary-unit">次</div>
                    </div>
                    <div class="cost-summary-card">
                        <div class="cost-summary-label">平均费用/次</div>
                        <div class="cost-summary-value" id="cost-avg-cost">--</div>
                        <div class="cost-summary-unit">元</div>
                    </div>
                </div>

                <!-- 图表区域 -->
                <div class="cost-stats-charts">
                    <div class="cost-chart-panel">
                        <h4>每日费用趋势</h4>
                        <div class="cost-chart-area" id="cost-date-chart"></div>
                    </div>
                    <div class="cost-chart-panel">
                        <h4>模型费用分布</h4>
                        <div class="cost-chart-area" id="cost-model-chart"></div>
                    </div>
                </div>

                <!-- 详细表格 -->
                <div class="cost-stats-tables">
                    <div class="cost-table-panel">
                        <h4>按模型统计</h4>
                        <table class="cost-table" id="cost-model-table">
                            <thead>
                                <tr>
                                    <th>模型</th>
                                    <th>消息数</th>
                                    <th>Token 数</th>
                                    <th>费用（元）</th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                    <div class="cost-table-panel">
                        <h4>按用户统计</h4>
                        <table class="cost-table" id="cost-user-table">
                            <thead>
                                <tr>
                                    <th>用户</th>
                                    <th>消息数</th>
                                    <th>Token 数</th>
                                    <th>费用（元）</th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[CostStatisticsTemplate] 容器不存在');
            return;
        }
        container.innerHTML = generateHTML();
        console.log('[CostStatisticsTemplate] HTML 渲染完成');
    }

    window.CostStatisticsTemplate = {
        generateHTML,
        render
    };

    console.log('[CostStatisticsTemplate] 费用统计模板加载成功');
})();
