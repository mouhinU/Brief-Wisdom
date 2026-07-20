package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

/**
 * SessionCreateRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Data
public class SessionCreateRequest {
    private String pageContext;  // 页面上下文（如 /about.html, /resume-manage.html）
}
