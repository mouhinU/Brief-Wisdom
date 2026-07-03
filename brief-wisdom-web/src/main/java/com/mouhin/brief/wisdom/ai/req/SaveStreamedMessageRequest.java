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
}
