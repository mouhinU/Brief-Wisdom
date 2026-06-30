package com.mouhin.brief.wisdom.web.req;

import lombok.Data;
/**
 * LoginRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Data
public class LoginRequest {
    private String username;
    private String password;
}
