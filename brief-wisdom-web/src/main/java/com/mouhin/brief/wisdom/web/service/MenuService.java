package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.common.menu.MenuTreeDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.persistence.repository.RoleMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysMenuRepository;
import com.mouhin.brief.wisdom.persistence.repository.SysRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuRepository sysMenuRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final SysRoleRepository sysRoleRepository;

    /**
     * 获取所有可见菜单（扁平列表），按 sort_order 排序
     */
    public List<MenuDTO> listVisibleMenus() {
        return sysMenuRepository.findVisibleOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有可见菜单（树形结构，含隐藏子项用于页面 Tab 渲染）
     */
    public List<MenuTreeDTO> listVisibleMenuTree() {
        List<SysMenu> menus = new ArrayList<>(sysMenuRepository.findVisibleOrderBySortOrderAsc());
        // 补充隐藏的子项（用于页面 Tab 渲染）
        List<Long> visibleMenuIds = menus.stream().map(SysMenu::getId).toList();
        List<SysMenu> hiddenChildren = sysMenuRepository.findHiddenChildrenByParentIds(visibleMenuIds);
        menus.addAll(hiddenChildren);
        return buildTree(menus);
    }

    /**
     * 根据用户角色获取可见菜单（树形结构）
     * <p>
     * 规则：
     * - 超级管理员：返回所有可见菜单 + 隐藏的子项（用于页面 Tab 渲染）
     * - 其他用户：角色分配的菜单 + 公开菜单 + 隐藏的子项
     * <p>
     * 隐藏子项（is_visible=0）虽然不在导航栏显示，但会作为 children 返回，
     * 供页面动态渲染 Tab 导航。
     */
    public List<MenuTreeDTO> getMenuTreeByRoles(List<String> roleKeys) {
        List<SysMenu> menus;

        // 超级管理员拥有所有权限
        if (roleKeys.contains("super_admin")) {
            menus = new ArrayList<>(sysMenuRepository.findVisibleOrderBySortOrderAsc());
        } else {
            // 获取用户所有角色的菜单 ID
            List<Long> menuIds = getRoleMenuIds(roleKeys);

            // 始终包含无需权限控制的公开菜单（首页、简历、AI助手等）
            List<SysMenu> publicMenus = sysMenuRepository.findPublicVisibleMenus();
            List<Long> publicMenuIds = publicMenus.stream().map(SysMenu::getId).toList();

            // 合并去重
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

        // 补充隐藏的子项（用于页面 Tab 渲染，不在导航栏显示）
        List<Long> visibleMenuIds = menus.stream().map(SysMenu::getId).toList();
        List<SysMenu> hiddenChildren = sysMenuRepository.findHiddenChildrenByParentIds(visibleMenuIds);
        menus.addAll(hiddenChildren);

        return buildTree(menus);
    }

    /**
     * 获取所有菜单（含隐藏，管理页面用）
     */
    public List<MenuDTO> listAllMenus() {
        return sysMenuRepository.findAllOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有菜单（树形结构，管理页面用）
     */
    public List<MenuTreeDTO> listAllMenuTree() {
        List<SysMenu> menus = sysMenuRepository.findAllOrderBySortOrderAsc();
        return buildTree(menus);
    }

    /**
     * 根据 ID 查询菜单
     */
    public SysMenu getMenuById(Long id) {
        return sysMenuRepository.findById(id);
    }

    /**
     * 新增菜单
     */
    public void createMenu(SysMenu menu) {
        sysMenuRepository.save(menu);
    }

    /**
     * 更新菜单
     */
    public void updateMenu(SysMenu menu) {
        sysMenuRepository.update(menu);
    }

    /**
     * 删除菜单（逻辑删除）
     */
    @Transactional
    public void deleteMenu(Long id) {
        // 删除角色-菜单关联
        roleMenuRepository.deleteByMenuId(id);
        // 删除菜单
        sysMenuRepository.deleteById(id);
    }

    /**
     * 切换菜单显示/隐藏状态
     */
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
                .filter(role -> role != null)
                .flatMap(role -> roleMenuRepository.findMenuIdsByRoleId(role.getId()).stream())
                .distinct()
                .toList();
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeDTO> buildTree(List<SysMenu> menus) {
        // 转换为 DTO
        List<MenuTreeDTO> dtos = menus.stream().map(this::toMenuTreeDTO).toList();

        // 按 parentId 分组
        Map<Long, List<MenuTreeDTO>> parentMap = dtos.stream()
                .collect(Collectors.groupingBy(dto -> dto.getParentId() != null ? dto.getParentId() : 0L));

        // 设置子节点
        dtos.forEach(dto -> {
            List<MenuTreeDTO> children = parentMap.get(dto.getId());
            dto.setChildren(children != null ? children : new ArrayList<>());
        });

        // 返回顶级菜单（parentId 为 0 或 null）
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
