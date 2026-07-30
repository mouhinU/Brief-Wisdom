package com.mouhin.brief.wisdom.common.team;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public record TeamDTO(
        Long id,
        String name,
        String description,
        String avatar,
        String ownerId,
        String myRole,
        Integer memberCount,
        Integer kbCount,
        LocalDateTime createTime,
        List<TeamMemberDTO> members
) implements Serializable {

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getAvatar() { return avatar; }

    public String getOwnerId() { return ownerId; }

    public String getMyRole() { return myRole; }

    public Integer getMemberCount() { return memberCount; }

    public Integer getKbCount() { return kbCount; }

    public LocalDateTime getCreateTime() { return createTime; }

    public List<TeamMemberDTO> getMembers() { return members; }
}
