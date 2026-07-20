package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.common.menu.MenuTreeDTO;
import com.mouhin.brief.wisdom.constants.CachePrefix;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.persistence.repository.RoleMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysRoleRepository;
import com.mouhin.brief.wisdom.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuRepository sysMenuRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final SysRoleRepository sysRoleRepository;

    /**
     * 获取所有可见菜单（扁平列表），按 sort_order 排序
     */
    @Override
    @Cacheable(value = CachePrefix.MENU_PUBLIC_CACHE, key = "'flat'")
    public List<MenuDTO> listVisibleMenus() {
        return sysMenuRepository.findVisibleOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有可见菜单（树形结构，含隐藏子项用于页面 Tab 渲染）
     */
    @Override
    @Cacheable(value = CachePrefix.MENU_PUBLIC_CACHE, key = "'tree'")
    public List<MenuTreeDTO> listVisibleMenuTree() {
        List<SysMenu> menus = new ArrayList<>(sysMenuRepository.findVisibleOrderBySortOrderAsc());
        List<Long> visibleMenuIds = menus.stream().map(SysMenu::getId).toList();
        List<SysMenu> hiddenChildren = sysMenuRepository.findHiddenChildrenByParentIds(visibleMenuIds);
        menus.addAll(hiddenChildren);
        return buildTree(menus);
    }

    /**
     * 根据用户角色获取可见菜单（树形结构）
     */
    @Override
    @Cacheable(value = CachePrefix.MENU_TREE_CACHE, key = "#roleKeys.toString()")
    public List<MenuTreeDTO> getMenuTreeByRoles(List<String> roleKeys) {
        List<SysMenu> menus;

        if (roleKeys.contains("super_admin")) {
            menus = new ArrayList<>(sysMenuRepository.findVisibleOrderBySortOrderAsc());
        } else {
            List<Long> menuIds = getRoleMenuIds(roleKeys);

            List<SysMenu> publicMenus = sysMenuRepository.findPublicVisibleMenus();
            List<Long> publicMenuIds = publicMenus.stream().map(SysMenu::getId).toList();

            List<Long> allMenuIds = new ArrayList<>(menuIds);
            for (Long id : publicMenuIds) {
                if (!allMenuIds.contains(id)) {
                    allMenuIds.add(id);
                }
            }

            if (allMenuIds.isEmpty()) {
                return List.of();
            }
            menus = new ArrayList<>(sysMenuRepository.findByIds(allMenuIds));
        }

        List<Long> visibleMenuIds = menus.stream().map(SysMenu::getId).toList();
        List<SysMenu> hiddenChildren = sysMenuRepository.findHiddenChildrenByParentIds(visibleMenuIds);
        menus.addAll(hiddenChildren);

        return buildTree(menus);
    }

    /**
     * 获取所有菜单（含隐藏，管理页面用）
     */
    @Override
    @Cacheable(value = CachePrefix.MENU_ALL_CACHE, key = "'flat'")
    public List<MenuDTO> listAllMenus() {
        return sysMenuRepository.findAllOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有菜单（树形结构，管理页面用）
     */
    @Override
    @Cacheable(value = CachePrefix.MENU_ALL_CACHE, key = "'tree'")
    public List<MenuTreeDTO> listAllMenuTree() {
        List<SysMenu> menus = sysMenuRepository.findAllOrderBySortOrderAsc();
        return buildTree(menus);
    }

    /**
     * 根据 ID 查询菜单
     */
    @Override
    public SysMenu getMenuById(Long id) {
        return sysMenuRepository.findById(id);
    }

    /**
     * 新增菜单
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_PUBLIC_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_ALL_CACHE, allEntries = true)
    })
    public void createMenu(SysMenu menu) {
        sysMenuRepository.save(menu);
    }

    /**
     * 更新菜单
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_PUBLIC_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_ALL_CACHE, allEntries = true)
    })
    public void updateMenu(SysMenu menu) {
        sysMenuRepository.update(menu);
    }

    /**
     * 删除菜单（逻辑删除）
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_PUBLIC_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_ALL_CACHE, allEntries = true)
    })
    public void deleteMenu(Long id) {
        roleMenuRepository.deleteByMenuId(id);
        sysMenuRepository.deleteById(id);
    }

    /**
     * 切换菜单显示/隐藏状态
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CachePrefix.MENU_TREE_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_PUBLIC_CACHE, allEntries = true),
            @CacheEvict(value = CachePrefix.MENU_ALL_CACHE, allEntries = true)
    })
    public void toggleVisible(Long id) {
        SysMenu menu = sysMenuRepository.findById(id);
        if (menu != null) {
            menu.setIsVisible(menu.getIsVisible() == 1 ? 0 : 1);
            sysMenuRepository.update(menu);
        }
    }

    /**
     * 获取多个角色的菜单 ID 合集
     */
    private List<Long> getRoleMenuIds(List<String> roleKeys) {
        return roleKeys.stream()
                .map(sysRoleRepository::findByRoleKey)
                .filter(Objects::nonNull)
                .flatMap(role -> roleMenuRepository.findMenuIdsByRoleId(role.getId()).stream())
                .distinct()
                .toList();
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeDTO> buildTree(List<SysMenu> menus) {
        List<MenuTreeDTO> dtos = menus.stream().map(this::toMenuTreeDTO).toList();

        Map<Long, List<MenuTreeDTO>> parentMap = dtos.stream()
                .collect(Collectors.groupingBy(dto -> dto.getParentId() != null ? dto.getParentId() : 0L));

        dtos.forEach(dto -> {
            List<MenuTreeDTO> children = parentMap.get(dto.getId());
            dto.setChildren(children != null ? children : new ArrayList<>());
        });

        return dtos.stream()
                .filter(dto -> dto.getParentId() == null || dto.getParentId() == 0L)
                .toList();
    }

    private MenuDTO toMenuDTO(SysMenu m) {
        MenuDTO dto = new MenuDTO();
        dto.setId(m.getId());
        dto.setParentId(m.getParentId());
        dto.setName(m.getName());
        dto.setUrl(m.getUrl());
        dto.setIcon(m.getIcon());
        dto.setTarget(m.getTarget());
        dto.setType(m.getType());
        dto.setPermission(m.getPermission());
        dto.setSortOrder(m.getSortOrder());
        dto.setIsVisible(m.getIsVisible());
        dto.setRequireLogin(m.getRequireLogin());
        return dto;
    }

    private MenuTreeDTO toMenuTreeDTO(SysMenu m) {
        MenuTreeDTO dto = new MenuTreeDTO();
        dto.setId(m.getId());
        dto.setParentId(m.getParentId());
        dto.setName(m.getName());
        dto.setUrl(m.getUrl());
        dto.setIcon(m.getIcon());
        dto.setTarget(m.getTarget());
        dto.setType(m.getType());
        dto.setPermission(m.getPermission());
        dto.setSortOrder(m.getSortOrder());
        dto.setIsVisible(m.getIsVisible());
        dto.setRequireLogin(m.getRequireLogin());
        return dto;
    }
}
