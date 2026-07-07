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
    /** 手机号（可选，绑定后用户级别为 vip） */
    private String phone;
    /** 短信验证码（绑定手机号时必填） */
    private String smsCode;
}
