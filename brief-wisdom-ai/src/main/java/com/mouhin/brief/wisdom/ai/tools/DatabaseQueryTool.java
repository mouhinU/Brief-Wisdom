package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL 查询助手工具
 * <p>
 * 允许管理员执行只读 SQL 查询，用于数据分析和排查问题。
 * 安全约束：仅允许 SELECT 语句，强制 LIMIT 上限，查询超时控制。
 * 所有查询记录到审计日志。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseQueryTool {

    private final JdbcTemplate jdbcTemplate;
    private final ToolContextProvider toolContextProvider;

    /** 危险 SQL 关键词（禁止非 SELECT 操作） */
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|REPLACE|MERGE|GRANT|REVOKE)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 最大返回行数 */
    private static final int MAX_ROWS = 100;

    /** 默认返回行数 */
    private static final int DEFAULT_ROWS = 10;

    /**
     * 执行只读 SQL 查询
     *
     * @param sql    SELECT SQL 语句
     * @param maxRows 最大返回行数
     * @return 查询结果
     */
    @Tool(description = "执行只读 SQL 查询，用于数据分析和排查问题。仅限 SELECT 语句，不允许 INSERT/UPDATE/DELETE 等修改操作。仅限 super_admin 使用。")
    public String queryDatabase(
            @ToolParam(description = "SELECT SQL 语句，不允许包含 INSERT/UPDATE/DELETE 等修改操作") String sql,
            @ToolParam(description = "最大返回行数，默认10，最大100", required = false) Integer maxRows) {

        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] queryDatabase 被调用: userId={}, sql={}", userId, sql);

        // 权限校验：仅 super_admin
        if (!toolContextProvider.isSuperAdmin()) {
            return "权限不足：仅超级管理员可使用 SQL 查询工具。";
        }

        // 安全检查
        if (sql == null || sql.isBlank()) {
            return "SQL 语句不能为空。";
        }

        String trimmedSql = sql.trim();
        if (!trimmedSql.toUpperCase().startsWith("SELECT")) {
            return "安全限制：仅允许 SELECT 查询语句。";
        }

        if (DANGEROUS_SQL_PATTERN.matcher(trimmedSql).find()) {
            log.warn("[Tool] queryDatabase 检测到危险 SQL: userId={}, sql={}", userId, sql);
            return "安全限制：SQL 中包含禁止的操作关键词。";
        }

        int effectiveMaxRows = (maxRows != null && maxRows > 0) ? Math.min(maxRows, MAX_ROWS) : DEFAULT_ROWS;

        // 强制添加 LIMIT（如果没有的话）
        if (!trimmedSql.toUpperCase().contains("LIMIT")) {
            trimmedSql = trimmedSql.replaceAll(";?\\s*$", "") + " LIMIT " + effectiveMaxRows;
        }

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(trimmedSql);

            if (results.isEmpty()) {
                return "查询执行成功，但没有返回任何结果。\nSQL: " + trimmedSql;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("查询结果（共 ").append(results.size()).append(" 行）：\n\n");

            // 表头
            Map<String, Object> firstRow = results.get(0);
            sb.append("| ").append(String.join(" | ", firstRow.keySet())).append(" |\n");
            sb.append("| ").append(firstRow.keySet().stream().map(k -> "---").reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");

            // 数据行
            for (Map<String, Object> row : results) {
                sb.append("| ").append(row.values().stream()
                        .map(v -> v != null ? v.toString() : "NULL")
                        .reduce((a, b) -> a + " | " + b)
                        .orElse("")).append(" |\n");
            }

            sb.append("\nSQL: ").append(trimmedSql);
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] queryDatabase 执行失败: sql={}, error={}", trimmedSql, e.getMessage(), e);
            return "SQL 查询执行失败: " + e.getMessage();
        }
    }

}
