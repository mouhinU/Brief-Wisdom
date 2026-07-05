package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.role.RoleDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import com.mouhin.brief.wisdom.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理 Controller
 */
/**
 * RoleController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
@RequiresPermission("role:manage")
@Tag(name = "角色管理", description = "角色权限管理相关接口")
public class RoleController {

    private final RoleService roleService;

    /**
     * 获取所有角色列表
     */
    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public List<RoleDTO> listRoles() {
        return roleService.listRoles();
    }

    /**
     * 获取所有启用的角色（用于下拉选择）
     */
    @Operation(summary = "获取启用角色列表", description = "获取所有启用的角色，用于下拉选择")
    @GetMapping("/enabled")
    public List<RoleDTO> listEnabledRoles() {
        return roleService.listEnabledRoles();
    }

    /**
     * 根据 ID 获取角色详情
     */
    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public RoleDTO getRole(
            @Parameter(description = "角色ID", required = true) @PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    /**
     * 创建角色
     */
    @Operation(summary = "创建角色")
    @PostMapping
    public Boolean createRole(@RequestBody RoleDTO roleDTO) {
        roleService.createRole(roleDTO);
        return true;
    }

    /**
     * 更新角色
     */
    @Operation(summary = "更新角色")
    @PutMapping
    public Boolean updateRole(@RequestBody RoleDTO roleDTO) {
        roleService.updateRole(roleDTO);
        return true;
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Boolean deleteRole(
            @Parameter(description = "角色ID", required = true) @PathVariable Long id) {
        roleService.deleteRole(id);
        return true;
    }

    /**
     * 分配角色菜单权限
     */
    @Operation(summary = "分配角色菜单权限")
    @PutMapping("/{id}/menus")
    public Boolean assignMenus(
            @Parameter(description = "角色ID", required = true) @PathVariable Long id,
            @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return true;
    }

    /**
     * 获取角色的菜单 ID 列表
     */
    @Operation(summary = "获取角色菜单", description = "获取角色的菜单 ID 列表")
    @GetMapping("/{id}/menus")
    public List<Long> getRoleMenus(
            @Parameter(description = "角色ID", required = true) @PathVariable Long id) {
        return roleService.getRoleMenuIds(id);
    }

    /**
     * 为用户分配角色
     */
    @Operation(summary = "分配用户角色")
    @PutMapping("/assign/{userId}")
    public Boolean assignUserRoles(
            @Parameter(description = "用户ID", required = true) @PathVariable String userId,
            @RequestBody List<Long> roleIds) {
        roleService.assignUserRoles(userId, roleIds);
        return true;
    }

    /**
     * 获取用户的角色列表
     */
    @Operation(summary = "获取用户角色", description = "获取指定用户的角色列表")
    @GetMapping("/user/{userId}")
    public List<RoleDTO> getUserRoles(
            @Parameter(description = "用户ID", required = true) @PathVariable String userId) {
        return roleService.getUserRoles(userId).stream()
                .map(this::toSimpleRoleDTO)
                .collect(Collectors.toList());
    }

    private RoleDTO toSimpleRoleDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleKey(role.getRoleKey());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        return dto;
    }
}
