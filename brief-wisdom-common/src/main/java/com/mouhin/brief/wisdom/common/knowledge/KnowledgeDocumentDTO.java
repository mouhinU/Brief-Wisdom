package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文档 DTO
 */
/**
 * KnowledgeDocumentDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class KnowledgeDocumentDTO {

    private Long id;
    private Long baseId;
    private String title;

    /**
     * 文档类型: INTERNAL-内部文档, FILE-文件, LINK-外部链接
     */
    private String docType;

    /**
     * 文档内容（INTERNAL类型使用，富文本HTML）
     */
    private String content;

    /**
     * 文件URL（FILE类型使用）
     */
    private String fileUrl;

    private String fileName;
    private Long fileSize;
    private String fileType;

    /**
     * 外部链接URL（LINK类型使用）
     */
    private String linkUrl;

    private String linkDesc;
    private String tags;
    private Integer viewCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 所属知识库名称（非数据库字段，用于展示）
     */
    private String baseName;

    /**
     * 文档类型显示名称
     */
    public String getDocTypeDisplay() {
        if (docType == null) return "";
        return switch (docType) {
            case "INTERNAL" -> "内部文档";
            case "FILE" -> "文件";
            case "LINK" -> "外部链接";
            default -> docType;
        };
    }

    /**
     * 状态显示名称
     */
    public String getStatusDisplay() {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已归档";
            default -> String.valueOf(status);
        };
    }

    /**
     * 文件大小格式化显示
     */
    public String getFileSizeDisplay() {
        if (fileSize == null || fileSize == 0) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
