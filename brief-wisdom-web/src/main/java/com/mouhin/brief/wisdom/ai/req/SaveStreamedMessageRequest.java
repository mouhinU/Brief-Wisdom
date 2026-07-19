package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

/**
 * 保存流式消息请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Data
public class SaveStreamedMessageRequest {
    private String sessionId;
    private String content;
    private String model;
    /** 流式响应估算的 Token 数量（前端或后端估算） */
    private Integer tokens;
    /** 流式响应估算的费用（元） */
    private Double cost;
}
