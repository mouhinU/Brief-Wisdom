/**
 * AI 安全审计日志组件 - HTML 模板生成器
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */

(function() {
    'use strict';

    function generateHTML() {
        return `
            <div class="audit-log-container">
                <!-- 统计卡片 -->
                <div class="audit-stats-grid" id="audit-stats-grid">
                    <div class="audit-stat-card">
                        <h3>总审计事件</h3>
                        <div class="value" id="audit-total-count">-</div>
                    </div>
                    <div class="audit-stat-card">
                        <h3>输入拦截</h3>
                        <div class="value" id="audit-input-blocked-count">-</div>
                    </div>
                    <div class="audit-stat-card audit-high">
                        <h3>输出过滤</h3>
                        <div class="value" id="audit-output-filtered-count">-</div>
                    </div>
                    <div class="audit-stat-card audit-critical">
                        <h3>高风险事件</h3>
                        <div class="value" id="audit-high-risk-count">-</div>
                    </div>
                </div>
                
                <!-- 筛选栏 -->
                <div class="audit-filter-bar">
                    <select id="audit-filter-type">
                        <option value="">所有类型</option>
                        <option value="INPUT_BLOCKED">输入拦截</option>
                        <option value="OUTPUT_FILTERED">输出过滤</option>
                        <option value="RISK_DETECTED">风险检测</option>
                    </select>
                    <select id="audit-filter-level">
                        <option value="">所有等级</option>
                        <option value="LOW">低</option>
                        <option value="MEDIUM">中</option>
                        <option value="HIGH">高</option>
                        <option value="CRITICAL">严重</option>
                    </select>
                    <input type="text" id="audit-filter-user" placeholder="用户ID（可选）">
                    <button onclick="AuditLog.loadLogs()">查询</button>
                </div>
                
                <!-- 表格 -->
                <div class="audit-table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>时间</th>
                                <th>审计类型</th>
                                <th>风险等级</th>
                                <th>触发关键词</th>
                                <th>动作</th>
                                <th>会话ID</th>
                                <th>用户ID</th>
                            </tr>
                        </thead>
                        <tbody id="audit-table-body">
                            <tr><td colspan="7" class="audit-loading">加载中...</td></tr>
                        </tbody>
                    </table>
                    
                    <!-- 分页 -->
                    <div class="audit-pagination" id="audit-pagination"></div>
                </div>
            </div>
        `;
    }

    function render(container) {
        if (!container) {
            console.error('[AuditLogTemplate] 容器不存在');
            return;
        }
        container.innerHTML = generateHTML();
        console.log('[AuditLogTemplate] HTML 渲染完成');
    }

    window.AuditLogTemplate = {
        generateHTML,
        render
    };

    console.log('[AuditLogTemplate] AI 安全审计日志模板加载成功');
})();
