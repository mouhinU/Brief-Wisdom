package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

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
}
