package com.mouhin.brief.wisdom.web.req;

import lombok.Data;
/**
 * RegisterRequest
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
}
