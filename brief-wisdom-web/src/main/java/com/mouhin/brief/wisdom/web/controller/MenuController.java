package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.common.menu.MenuTreeDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.web.service.MenuService;
import com.mouhin.brief.wisdom.web.service.RoleService;
import com.mouhin.brief.wisdom.web.service.UserContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 菜单 REST 接口
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MenuController {

    private final MenuService menuService;
    private final RoleService roleService;
    private final UserContextHelper userContextHelper;

    /**
     * 获取可见菜单列表（扁平结构）
     */
    @GetMapping("/list")
    public java.util.List<MenuDTO> listMenus() {
        return menuService.listVisibleMenus();
    }

    /**
     * 获取可见菜单（树形结构）
     */
    @GetMapping("/tree")
    public java.util.List<MenuTreeDTO> listMenuTree() {
        return menuService.listVisibleMenuTree();
    }

    /**
     * 获取当前用户可见菜单（树形结构，基于角色权限）
     */
    @GetMapping("/my-menus")
    public java.util.List<MenuTreeDTO> getMyMenus() {
        String userId = userContextHelper.getCurrentUserId();
        if (userId == null) {
            // 未登录返回空
            return java.util.List.of();
        }
        java.util.List<String> roleKeys = roleService.getUserRoleKeys(userId);
        return menuService.getMenuTreeByRoles(roleKeys);
    }

    /**
     * 获取全部菜单（含隐藏，系统设置用，扁平结构）
     */
    @GetMapping("/all")
    public java.util.List<MenuDTO> listAllMenus() {
        return menuService.listAllMenus();
    }

    /**
     * 获取全部菜单（树形结构，系统设置用）
     */
    @GetMapping("/all/tree")
    public java.util.List<MenuTreeDTO> listAllMenuTree() {
        return menuService.listAllMenuTree();
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public Boolean createMenu(@RequestBody SysMenu menu) {
        menuService.createMenu(menu);
        return true;
    }

    /**
     * 更新菜单
     */
    @PutMapping
    public Boolean updateMenu(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return true;
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public Boolean deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return true;
    }

    /**
     * 切换菜单显示/隐藏
     */
    @PutMapping("/{id}/toggle")
    public Boolean toggleVisible(@PathVariable Long id) {
        menuService.toggleVisible(id);
        return true;
    }
}
