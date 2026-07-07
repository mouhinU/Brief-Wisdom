package com.mouhin.brief.wisdom.common.knowledge;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识文档创建/更新请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
public class KnowledgeDocumentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private Long baseId;

    /** 文档标题 */
    private String title;

    /** 文档类型：MARKDOWN/LINK/FILE */
    private String docType;

    /** 文档内容（Markdown格式） */
    private String content;

    /** 文件URL */
    private String fileUrl;

    /** 文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型 */
    private String fileType;

    /** 链接URL */
    private String linkUrl;

    /** 链接描述 */
    private String linkDesc;

    /** 标签列表（逗号分隔） */
    private String tags;

    /** 排序值 */
    private Integer sortOrder;

    /** 状态：0-草稿，1-已发布，2-已归档 */
    private Integer status;
}
