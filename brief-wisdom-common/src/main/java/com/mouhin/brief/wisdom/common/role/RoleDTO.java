package com.mouhin.brief.wisdom.common.role;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record RoleDTO(
        Long id,
        String roleName,
        String roleKey,
        String description,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
        List<Long> menuIds
) implements Serializable {

    public Long getId() { return id; }

    public String getRoleName() { return roleName; }

    public String getRoleKey() { return roleKey; }

    public String getDescription() { return description; }

    public Integer getStatus() { return status; }

    public LocalDateTime getCreateTime() { return createTime; }

    public List<Long> getMenuIds() { return menuIds; }
}
