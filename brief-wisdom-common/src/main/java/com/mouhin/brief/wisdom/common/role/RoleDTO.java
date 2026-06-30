package com.mouhin.brief.wisdom.common.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象
 */
/**
 * RoleDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class RoleDTO implements Serializable {
    private Long id;
    private String roleName;
    private String roleKey;
    private String description;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<Long> menuIds;
}
