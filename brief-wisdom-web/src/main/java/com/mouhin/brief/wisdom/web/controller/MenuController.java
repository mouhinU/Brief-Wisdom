package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.web.service.MenuService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ApiResponse listMenus() {
        try {
            List<SysMenu> menus = menuService.listVisibleMenus();
            List<MenuVO> data = menus.stream().map(m -> {
                MenuVO vo = new MenuVO();
                vo.setId(m.getId());
                vo.setName(m.getName());
                vo.setUrl(m.getUrl());
                vo.setIcon(m.getIcon());
                vo.setTarget(m.getTarget());
                vo.setSortOrder(m.getSortOrder());
                return vo;
            }).collect(Collectors.toList());
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取菜单列表失败: ", e);
            return ApiResponse.error("获取菜单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取全部菜单（含隐藏，系统设置用）
     */
    @GetMapping("/all")
    public ApiResponse listAllMenus() {
        try {
            List<SysMenu> menus = menuService.listAllMenus();
            return ApiResponse.success(menus);
        } catch (Exception e) {
            log.error("获取全部菜单失败: ", e);
            return ApiResponse.error("获取全部菜单失败: " + e.getMessage());
        }
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public ApiResponse createMenu(@RequestBody SysMenu menu) {
        try {
            menuService.createMenu(menu);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("新增菜单失败: ", e);
            return ApiResponse.error("新增菜单失败: " + e.getMessage());
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping
    public ApiResponse updateMenu(@RequestBody SysMenu menu) {
        try {
            menuService.updateMenu(menu);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新菜单失败: ", e);
            return ApiResponse.error("更新菜单失败: " + e.getMessage());
        }
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ApiResponse deleteMenu(@PathVariable Long id) {
        try {
            menuService.deleteMenu(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除菜单失败: ", e);
            return ApiResponse.error("删除菜单失败: " + e.getMessage());
        }
    }

    @Data
    public static class MenuVO {
        private Long id;
        private String name;
        private String url;
        private String icon;
        private String target;
        private Integer sortOrder;
    }

    @Data
    public static class ApiResponse {
        private boolean success;
        private Object data;
        private String error;

        public static ApiResponse success(Object data) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(true);
            response.setData(data);
            return response;
        }

        public static ApiResponse error(String error) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(false);
            response.setError(error);
            return response;
        }
    }
}
