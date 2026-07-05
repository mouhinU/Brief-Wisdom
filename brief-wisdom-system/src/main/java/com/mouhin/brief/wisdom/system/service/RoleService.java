package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.common.role.RoleDTO;
import com.mouhin.brief.wisdom.persistence.model.SysRole;

import java.util.List;

/**
 * 角色管理服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface RoleService {

    /**
     * 获取所有角色
     *
     * @return 角色 DTO 列表
     */
    List<RoleDTO> listRoles();

    /**
     * 获取所有启用的角色
     *
     * @return 启用的角色 DTO 列表
     */
    List<RoleDTO> listEnabledRoles();

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 角色 DTO
     */
    RoleDTO getRoleById(Long id);

    /**
     * 根据 roleKey 查询角色
     *
     * @param roleKey 角色标识
     * @return 角色实体
     */
    SysRole getRoleByKey(String roleKey);

    /**
     * 创建角色
     *
     * @param roleDTO 角色 DTO
     */
    void createRole(RoleDTO roleDTO);

    /**
     * 更新角色
     *
     * @param roleDTO 角色 DTO
     */
    void updateRole(RoleDTO roleDTO);

    /**
     * 删除角色
     *
     * @param id 角色 ID
     */
    void deleteRole(Long id);

    /**
     * 分配角色菜单权限
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取用户的角色列表
     *
     * @param userId 用户 ID
     * @return 角色实体列表
     */
    List<SysRole> getUserRoles(String userId);

    /**
     * 获取用户的角色 Key 列表
     *
     * @param userId 用户 ID
     * @return 角色 Key 列表
     */
    List<String> getUserRoleKeys(String userId);

    /**
     * 为用户分配角色
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     */
    void assignUserRoles(String userId, List<Long> roleIds);

    /**
     * 为用户分配默认角色（normal 普通用户）
     *
     * @param userId 用户 ID
     */
    void assignDefaultRole(String userId);

    /**
     * 获取角色的菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 获取用户拥有的所有权限标识
     *
     * @param userId 用户ID
     * @return 权限标识列表，super_admin 返回 null
     */
    List<String> getUserPermissions(String userId);

    /**
     * 根据角色 Key 列表获取所有权限标识
     *
     * @param roleKeys 角色 Key 列表
     * @return 权限标识列表，super_admin 返回 null
     */
    List<String> getPermissionsByRoleKeys(List<String> roleKeys);
}
