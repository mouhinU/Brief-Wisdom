package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.common.menu.MenuTreeDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface MenuService {

    /**
     * 获取所有可见菜单（扁平列表），按 sort_order 排序
     */
    List<MenuDTO> listVisibleMenus();

    /**
     * 获取所有可见菜单（树形结构，含隐藏子项用于页面 Tab 渲染）
     */
    List<MenuTreeDTO> listVisibleMenuTree();

    /**
     * 根据用户角色获取可见菜单（树形结构）
     *
     * @param roleKeys 用户角色 Key 列表
     * @return 菜单树
     */
    List<MenuTreeDTO> getMenuTreeByRoles(List<String> roleKeys);

    /**
     * 获取所有菜单（含隐藏，管理页面用）
     */
    List<MenuDTO> listAllMenus();

    /**
     * 获取所有菜单（树形结构，管理页面用）
     */
    List<MenuTreeDTO> listAllMenuTree();

    /**
     * 根据 ID 查询菜单
     */
    SysMenu getMenuById(Long id);

    /**
     * 新增菜单
     */
    void createMenu(SysMenu menu);

    /**
     * 更新菜单
     */
    void updateMenu(SysMenu menu);

    /**
     * 删除菜单（逻辑删除）
     */
    void deleteMenu(Long id);

    /**
     * 切换菜单显示/隐藏状态
     */
    void toggleVisible(Long id);
}
