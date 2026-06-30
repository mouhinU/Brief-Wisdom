package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

/**
 * 知识文档创建/更新请求
 */
@Data
public class KnowledgeDocumentRequest {

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
    private Integer sortOrder;
    private Integer status;
}
