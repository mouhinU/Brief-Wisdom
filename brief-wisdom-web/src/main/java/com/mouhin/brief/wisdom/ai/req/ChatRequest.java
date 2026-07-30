package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

import java.util.List;

/**
 * ChatRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Data
public class ChatRequest {
    private String message;
    private String model;  // 可选：指定使用的模型
    private String pageContext;  // 可选：当前页面上下文（如 /about.html）
    private List<String> attachmentIds;  // 可选：已上传的附件ID列表（多模态）
}
