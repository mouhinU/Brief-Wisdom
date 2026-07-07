package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.common.menu.MenuTreeDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.system.service.MenuService;
import com.mouhin.brief.wisdom.system.service.RoleService;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 菜单 REST 接口
 */
/**
 * MenuController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "菜单管理", description = "系统菜单管理相关接口")
public class MenuController {

    private final MenuService menuService;
    private final RoleService roleService;
    private final UserContextHelper userContextHelper;

    /**
     * 获取可见菜单列表（扁平结构）
     */
    @Operation(summary = "获取可见菜单列表", description = "获取扁平结构的可见菜单")
    @GetMapping("/list")
    public java.util.List<MenuDTO> listMenus() {
        return menuService.listVisibleMenus();
    }

    /**
     * 获取可见菜单（树形结构）
     * <p>
     * 已登录用户：基于角色权限过滤菜单
     * 未登录用户：返回所有可见菜单（公开菜单）
     */
    @Operation(summary = "获取菜单树", description = "已登录用户基于角色权限过滤，未登录用户返回所有可见菜单")
    @GetMapping("/tree")
    public java.util.List<MenuTreeDTO> listMenuTree(jakarta.servlet.http.HttpServletRequest request) {
        // 检查用户是否已登录
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            com.mouhin.brief.wisdom.persistence.model.ChatUser user =
                (com.mouhin.brief.wisdom.persistence.model.ChatUser) session.getAttribute(UserContextHelper.SESSION_USER_KEY);
            if (user != null) {
                // 已登录：基于角色过滤菜单
                java.util.List<String> roleKeys = roleService.getUserRoleKeys(user.getUserId());
                return menuService.getMenuTreeByRoles(roleKeys);
            }
        }
        // 未登录：返回所有可见菜单
        return menuService.listVisibleMenuTree();
    }

    /**
     * 获取当前用户可见菜单（树形结构，基于角色权限）
     */
    @Operation(summary = "获取当前用户菜单", description = "获取当前登录用户可见的菜单树，基于角色权限")
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
    @Operation(summary = "获取全部菜单", description = "获取所有菜单（含隐藏），系统设置用，扁平结构")
    @GetMapping("/all")
    @RequiresPermission("menu:manage")
    public java.util.List<MenuDTO> listAllMenus() {
        return menuService.listAllMenus();
    }

    /**
     * 获取全部菜单（树形结构，系统设置用）
     */
    @Operation(summary = "获取全部菜单树", description = "获取所有菜单树形结构，系统设置用")
    @GetMapping("/all/tree")
    @RequiresPermission("menu:manage")
    public java.util.List<MenuTreeDTO> listAllMenuTree() {
        return menuService.listAllMenuTree();
    }

    /**
     * 新增菜单
     */
    @Operation(summary = "新增菜单")
    @PostMapping
    @RequiresPermission("menu:manage")
    public Boolean createMenu(@RequestBody SysMenu menu) {
        menuService.createMenu(menu);
        return true;
    }

    /**
     * 更新菜单
     */
    @Operation(summary = "更新菜单")
    @PutMapping
    @RequiresPermission("menu:manage")
    public Boolean updateMenu(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return true;
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @RequiresPermission("menu:manage")
    public Boolean deleteMenu(
            @Parameter(description = "菜单ID", required = true) @PathVariable Long id) {
        menuService.deleteMenu(id);
        return true;
    }

    /**
     * 切换菜单显示/隐藏
     */
    @Operation(summary = "切换菜单显示/隐藏")
    @PutMapping("/{id}/toggle")
    @RequiresPermission("menu:manage")
    public Boolean toggleVisible(
            @Parameter(description = "菜单ID", required = true) @PathVariable Long id) {
        menuService.toggleVisible(id);
        return true;
    }
}
