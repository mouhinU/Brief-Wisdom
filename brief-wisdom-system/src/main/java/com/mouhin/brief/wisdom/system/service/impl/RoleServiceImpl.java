package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.common.role.RoleDTO;
import com.mouhin.brief.wisdom.exception.SystemSettingsException;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import com.mouhin.brief.wisdom.persistence.model.UserRole;
import com.mouhin.brief.wisdom.persistence.repository.RoleMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysRoleRepository;
import com.mouhin.brief.wisdom.persistence.repository.UserRoleRepository;
import com.mouhin.brief.wisdom.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mouhin.brief.wisdom.constants.CachePrefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 角色管理服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleRepository sysRoleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final SysMenuRepository sysMenuRepository;

    /**
     * 获取所有角色
     */
    @Override
    @Cacheable(value = CachePrefix.USER_ROLE_LIST_CACHE, key = "'all'")
    public List<RoleDTO> listRoles() {
        return sysRoleRepository.findAll().stream().map(this::toRoleDTO).toList();
    }

    /**
     * 获取所有启用的角色
     */
    @Override
    public List<RoleDTO> listEnabledRoles() {
        return sysRoleRepository.findAllEnabled().stream().map(this::toRoleDTO).toList();
    }

    /**
     * 根据 ID 查询角色
     */
    @Override
    public RoleDTO getRoleById(Long id) {
        SysRole role = sysRoleRepository.findById(id);
        if (role == null) {
            throw new SystemSettingsException("角色不存在");
        }
        return toRoleDTO(role);
    }

    /**
     * 根据 roleKey 查询角色
     */
    @Override
    @Cacheable(value = CachePrefix.USER_ROLE_CACHE, key = "#roleKey", unless = "#result == null")
    public SysRole getRoleByKey(String roleKey) {
        return sysRoleRepository.findByRoleKey(roleKey);
    }

    /**
     * 创建角色
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void createRole(RoleDTO roleDTO) {
        SysRole existing = sysRoleRepository.findByRoleKey(roleDTO.getRoleKey());
        if (existing != null) {
            throw new SystemSettingsException("角色标识已存在: " + roleDTO.getRoleKey());
        }

        SysRole role = new SysRole();
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleKey(roleDTO.getRoleKey());
        role.setDescription(roleDTO.getDescription());
        role.setStatus(roleDTO.getStatus() != null ? roleDTO.getStatus() : 1);
        sysRoleRepository.save(role);
        log.info("创建角色: roleName={}, roleKey={}", role.getRoleName(), role.getRoleKey());
    }

    /**
     * 更新角色
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLE_CACHE, key = "#roleDTO.roleKey"),
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void updateRole(RoleDTO roleDTO) {
        SysRole role = sysRoleRepository.findById(roleDTO.getId());
        if (role == null) {
            throw new SystemSettingsException("角色不存在");
        }

        role.setRoleName(roleDTO.getRoleName());
        role.setDescription(roleDTO.getDescription());
        role.setStatus(roleDTO.getStatus());
        sysRoleRepository.update(role);
        log.info("更新角色: id={}, roleName={}", role.getId(), role.getRoleName());
    }

    /** 系统预置角色 Key 列表（不可删除） */
    private static final List<String> SYSTEM_ROLE_KEYS = List.of("super_admin", "admin", "normal");

    /** 默认角色 Key */
    private static final String DEFAULT_ROLE_KEY = "normal";

    /**
     * 删除角色
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLES_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void deleteRole(Long id) {
        SysRole role = sysRoleRepository.findById(id);
        if (role == null) {
            throw new SystemSettingsException("角色不存在");
        }

        if (SYSTEM_ROLE_KEYS.contains(role.getRoleKey())) {
            throw new SystemSettingsException("系统预置角色【" + role.getRoleName() + "】不可删除");
        }

        long userCount = userRoleRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new SystemSettingsException("角色【" + role.getRoleName() + "】正在被 " + userCount + " 个用户使用，无法删除");
        }

        roleMenuRepository.deleteByRoleId(id);
        sysRoleRepository.deleteById(id);
        log.info("删除角色: id={}, roleKey={}", id, role.getRoleKey());
    }

    /**
     * 分配角色菜单权限
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = sysRoleRepository.findById(roleId);
        if (role == null) {
            throw new SystemSettingsException("角色不存在");
        }

        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                roleMenuRepository.save(roleId, menuId);
            }
        }
        log.info("分配角色菜单: roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    /**
     * 获取用户的角色列表
     */
    @Override
    public List<SysRole> getUserRoles(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return List.of();
        }
        // 批量查询角色，避免 N+1
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        List<SysRole> roles = sysRoleRepository.findByIds(roleIds);
        return roles.stream()
                .filter(role -> role.getStatus() == 1)
                .toList();
    }

    /**
     * 获取用户的角色 Key 列表
     */
    @Override
    @Cacheable(value = CachePrefix.USER_ROLES_CACHE, key = "#userId")
    public List<String> getUserRoleKeys(String userId) {
        return getUserRoles(userId).stream().map(SysRole::getRoleKey).toList();
    }

    /**
     * 为用户分配角色
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLES_CACHE, key = "#userId"),
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, key = "#userId"),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true)
    })
    public void assignUserRoles(String userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userRoleRepository.save(userId, roleId);
            }
        }
        log.info("分配用户角色: userId={}, roleCount={}", userId, roleIds != null ? roleIds.size() : 0);
    }

    /**
     * 为用户分配默认角色（normal 普通用户）
     */
    @Override
    @Transactional
    public void assignDefaultRole(String userId) {
        SysRole normalRole = sysRoleRepository.findByRoleKey(DEFAULT_ROLE_KEY);
        if (normalRole != null) {
            userRoleRepository.save(userId, normalRole.getId());
            log.info("[角色分配] 为用户分配默认角色 normal: userId={}", userId);
        } else {
            log.warn("[角色分配] 未找到 normal 角色，跳过默认角色分配");
        }
    }

    /**
     * 获取角色的菜单 ID 列表
     */
    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuRepository.findMenuIdsByRoleId(roleId);
    }

    /**
     * 获取用户拥有的所有权限标识
     */
    @Override
    @Cacheable(value = CachePrefix.USER_PERMS_CACHE, key = "#userId", unless = "#result == null")
    public List<String> getUserPermissions(String userId) {
        List<String> roleKeys = getUserRoleKeys(userId);
        return getPermissionsByRoleKeys(roleKeys);
    }

    /**
     * 根据角色 Key 列表获取所有权限标识
     */
    @Override
    @Cacheable(value = CachePrefix.USER_PERMS_CACHE, key = "#roleKeys.toString()", unless = "#result == null")
    public List<String> getPermissionsByRoleKeys(List<String> roleKeys) {
        if (roleKeys.contains("super_admin")) {
            return null;
        }

        List<Long> menuIds = roleKeys.stream()
                .map(sysRoleRepository::findByRoleKey)
                .filter(Objects::nonNull)
                .flatMap(role -> roleMenuRepository.findMenuIdsByRoleId(role.getId()).stream())
                .distinct()
                .toList();

        if (menuIds.isEmpty()) {
            return List.of();
        }

        List<SysMenu> menus = sysMenuRepository.findByIds(menuIds);
        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .toList();
    }

    private RoleDTO toRoleDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleKey(role.getRoleKey());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        dto.setCreateTime(role.getCreateTime());
        dto.setMenuIds(roleMenuRepository.findMenuIdsByRoleId(role.getId()));
        return dto;
    }
}
