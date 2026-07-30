package com.mouhin.brief.wisdom.common.team;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库共享授权传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public record KbShareDTO(
        Long id,
        Long kbId,
        String kbName,
        String targetType,
        String targetId,
        String targetName,
        String permission,
        String sharedBy,
        LocalDateTime createTime
) implements Serializable {

    public Long getId() { return id; }

    public Long getKbId() { return kbId; }

    public String getKbName() { return kbName; }

    public String getTargetType() { return targetType; }

    public String getTargetId() { return targetId; }

    public String getTargetName() { return targetName; }

    public String getPermission() { return permission; }

    public String getSharedBy() { return sharedBy; }

    public LocalDateTime getCreateTime() { return createTime; }
}
