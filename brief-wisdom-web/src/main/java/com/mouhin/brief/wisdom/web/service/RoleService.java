package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.constants.CachePrefix;
import com.mouhin.brief.wisdom.common.role.RoleDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import com.mouhin.brief.wisdom.persistence.model.UserRole;
import com.mouhin.brief.wisdom.persistence.repository.RoleMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysRoleRepository;
import com.mouhin.brief.wisdom.persistence.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 角色管理服务
 */
/**
 * RoleService
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleRepository sysRoleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final SysMenuRepository sysMenuRepository;

    /**
     * 获取所有角色
     */
    @Cacheable(value = CachePrefix.USER_ROLE_LIST_CACHE, key = "'all'")
    public List<RoleDTO> listRoles() {
        return sysRoleRepository.findAll().stream().map(this::toRoleDTO).toList();
    }

    /**
     * 获取所有启用的角色
     */
    public List<RoleDTO> listEnabledRoles() {
        return sysRoleRepository.findAllEnabled().stream().map(this::toRoleDTO).toList();
    }

    /**
     * 根据 ID 查询角色
     */
    public RoleDTO getRoleById(Long id) {
        SysRole role = sysRoleRepository.findById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return toRoleDTO(role);
    }

    /**
     * 根据 roleKey 查询角色
     */
    @Cacheable(value = CachePrefix.USER_ROLE_CACHE, key = "#roleKey", unless = "#result == null")
    public SysRole getRoleByKey(String roleKey) {
        return sysRoleRepository.findByRoleKey(roleKey);
    }

    /**
     * 创建角色
     */
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void createRole(RoleDTO roleDTO) {
        // 检查 roleKey 是否已存在
        SysRole existing = sysRoleRepository.findByRoleKey(roleDTO.getRoleKey());
        if (existing != null) {
            throw new IllegalArgumentException("角色标识已存在: " + roleDTO.getRoleKey());
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
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLE_CACHE, key = "#roleDTO.roleKey"),
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void updateRole(RoleDTO roleDTO) {
        SysRole role = sysRoleRepository.findById(roleDTO.getId());
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        role.setRoleName(roleDTO.getRoleName());
        role.setDescription(roleDTO.getDescription());
        role.setStatus(roleDTO.getStatus());
        sysRoleRepository.update(role);
        log.info("更新角色: id={}, roleName={}", role.getId(), role.getRoleName());
    }

    /**
     * 系统预置角色 Key 列表（不可删除）
     */
    private static final List<String> SYSTEM_ROLE_KEYS = List.of("super_admin", "admin", "normal");

    /**
     * 删除角色
     * <p>
     * 规则：
     * 1. 系统预置角色（super_admin, admin, normal）不可删除
     * 2. 已有用户使用的角色不可删除
     */
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
            throw new IllegalArgumentException("角色不存在");
        }

        // 1. 检查是否为系统预置角色
        if (SYSTEM_ROLE_KEYS.contains(role.getRoleKey())) {
            throw new IllegalArgumentException("系统预置角色【" + role.getRoleName() + "】不可删除");
        }

        // 2. 检查是否有用户正在使用该角色
        long userCount = userRoleRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new IllegalArgumentException("角色【" + role.getRoleName() + "】正在被 " + userCount + " 个用户使用，无法删除");
        }

        // 3. 删除角色-菜单关联
        roleMenuRepository.deleteByRoleId(id);
        // 4. 删除角色
        sysRoleRepository.deleteById(id);
        log.info("删除角色: id={}, roleKey={}", id, role.getRoleKey());
    }

    /**
     * 分配角色菜单权限
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.USER_ROLE_LIST_CACHE, allEntries = true)
    })
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = sysRoleRepository.findById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        // 先删除原有菜单关联
        roleMenuRepository.deleteByRoleId(roleId);
        // 添加新的菜单关联
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
    public List<SysRole> getUserRoles(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return List.of();
        }
        return userRoles.stream()
                .map(ur -> sysRoleRepository.findById(ur.getRoleId()))
                .filter(role -> role != null && role.getStatus() == 1)
                .toList();
    }

    /**
     * 获取用户的角色 Key 列表
     */
    @Cacheable(value = CachePrefix.USER_ROLES_CACHE, key = "#userId")
    public List<String> getUserRoleKeys(String userId) {
        return getUserRoles(userId).stream().map(SysRole::getRoleKey).toList();
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.USER_ROLES_CACHE, key = "#userId"),
            @CacheEvict(value = CachePrefix.USER_PERMS_CACHE, key = "#userId"),
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true)
    })
    public void assignUserRoles(String userId, List<Long> roleIds) {
        // 先删除原有角色
        userRoleRepository.deleteByUserId(userId);
        // 添加新角色
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
    @Transactional
    public void assignDefaultRole(String userId) {
        SysRole normalRole = sysRoleRepository.findByRoleKey("normal");
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
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuRepository.findMenuIdsByRoleId(roleId);
    }

    /**
     * 获取用户拥有的所有权限标识（基于角色的菜单 permission 字段）
     * <p>
     * super_admin 返回 null（表示拥有所有权限，无需检查）
     * 其他角色返回其关联菜单中非空的 permission 集合
     *
     * @param userId 用户ID
     * @return 权限标识列表，super_admin 返回 null
     */
    @Cacheable(value = CachePrefix.USER_PERMS_CACHE, key = "#userId", unless = "#result == null")
    public List<String> getUserPermissions(String userId) {
        List<String> roleKeys = getUserRoleKeys(userId);
        return getPermissionsByRoleKeys(roleKeys);
    }

    /**
     * 根据角色 Key 列表获取所有权限标识
     *
     * @param roleKeys 角色 Key 列表
     * @return 权限标识列表，super_admin 返回 null
     */
    @Cacheable(value = CachePrefix.USER_PERMS_CACHE, key = "#roleKeys.toString()", unless = "#result == null")
    public List<String> getPermissionsByRoleKeys(List<String> roleKeys) {
        if (roleKeys.contains("super_admin")) {
            return null; // super_admin 拥有所有权限
        }

        // 获取所有角色关联的菜单 ID
        List<Long> menuIds = roleKeys.stream()
                .map(sysRoleRepository::findByRoleKey)
                .filter(Objects::nonNull)
                .flatMap(role -> roleMenuRepository.findMenuIdsByRoleId(role.getId()).stream())
                .distinct()
                .toList();

        if (menuIds.isEmpty()) {
            return List.of();
        }

        // 获取菜单中非空的 permission 字段
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
