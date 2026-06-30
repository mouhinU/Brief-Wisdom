package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;
/**
 * ChatWithPromptRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Data
public class ChatWithPromptRequest {
    private String systemPrompt;
    private String userMessage;
}
