package com.mouhin.brief.wisdom.common.manage;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record UserDTO(
        Long id,
        String userId,
        String username,
        String nickname,
        String avatar,
        String userLevel,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
        Integer sessionCount,
        List<String> roleKeys,
        List<String> roleNames
) implements Serializable {

    public Long getId() { return id; }

    public String getUserId() { return userId; }

    public String getUsername() { return username; }

    public String getNickname() { return nickname; }

    public String getAvatar() { return avatar; }

    public String getUserLevel() { return userLevel; }

    public LocalDateTime getCreateTime() { return createTime; }

    public Integer getSessionCount() { return sessionCount; }

    public List<String> getRoleKeys() { return roleKeys; }

    public List<String> getRoleNames() { return roleNames; }
}
