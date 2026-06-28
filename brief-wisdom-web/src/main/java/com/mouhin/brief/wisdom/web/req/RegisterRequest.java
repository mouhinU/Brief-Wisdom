package com.mouhin.brief.wisdom.web.req;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
}
