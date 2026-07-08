package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

/**
 * Markdown 批量导入结果
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class MarkdownImportResult {

    /** 新增文档数 */
    private int createdCount;

    /** 更新文档数 */
    private int updatedCount;

    /** 失败文件数 */
    private int failedCount;

    public void incrementCreated() {
        createdCount++;
    }

    public void incrementUpdated() {
        updatedCount++;
    }

    public void incrementFailed() {
        failedCount++;
    }

    /**
     * 成功处理总数（新增 + 更新）
     */
    public int getTotalCount() {
        return createdCount + updatedCount;
    }
}
