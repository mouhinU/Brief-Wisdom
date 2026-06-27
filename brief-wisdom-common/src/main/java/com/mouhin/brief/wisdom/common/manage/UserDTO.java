package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息 DTO
 */
@Data
public class UserDTO implements Serializable {
    private Long id;
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    private String userLevel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private Integer sessionCount;
}
