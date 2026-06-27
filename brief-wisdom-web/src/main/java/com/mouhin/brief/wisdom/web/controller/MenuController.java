package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.web.service.MenuService;
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

    /**
     * 获取可见菜单列表
     */
    @GetMapping("/list")
    public ApiResponse<java.util.List<MenuDTO>> listMenus() {
        try {
            return ApiResponse.success(menuService.listVisibleMenus());
        } catch (Exception e) {
            log.error("获取菜单列表失败: ", e);
            return ApiResponse.fail("获取菜单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取全部菜单（含隐藏，系统设置用）
     */
    @GetMapping("/all")
    public ApiResponse<java.util.List<MenuDTO>> listAllMenus() {
        try {
            return ApiResponse.success(menuService.listAllMenus());
        } catch (Exception e) {
            log.error("获取全部菜单失败: ", e);
            return ApiResponse.fail("获取全部菜单失败: " + e.getMessage());
        }
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public ApiResponse<Void> createMenu(@RequestBody SysMenu menu) {
        try {
            menuService.createMenu(menu);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("新增菜单失败: ", e);
            return ApiResponse.fail("新增菜单失败: " + e.getMessage());
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping
    public ApiResponse<Void> updateMenu(@RequestBody SysMenu menu) {
        try {
            menuService.updateMenu(menu);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新菜单失败: ", e);
            return ApiResponse.fail("更新菜单失败: " + e.getMessage());
        }
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        try {
            menuService.deleteMenu(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除菜单失败: ", e);
            return ApiResponse.fail("删除菜单失败: " + e.getMessage());
        }
    }

    /**
     * 切换菜单显示/隐藏
     */
    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggleVisible(@PathVariable Long id) {
        try {
            menuService.toggleVisible(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("切换菜单状态失败: ", e);
            return ApiResponse.fail("切换菜单状态失败: " + e.getMessage());
        }
    }
}
