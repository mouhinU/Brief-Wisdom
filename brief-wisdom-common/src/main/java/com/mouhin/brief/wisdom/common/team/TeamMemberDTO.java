package com.mouhin.brief.wisdom.common.team;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队成员传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public record TeamMemberDTO(
        Long id,
        Long teamId,
        String userId,
        String role,
        LocalDateTime createTime
) implements Serializable {

    public Long getId() { return id; }

    public Long getTeamId() { return teamId; }

    public String getUserId() { return userId; }

    public String getRole() { return role; }

    public LocalDateTime getCreateTime() { return createTime; }
}
