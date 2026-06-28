package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

@Data
public class ChatWithPromptRequest {
    private String systemPrompt;
    private String userMessage;
}
