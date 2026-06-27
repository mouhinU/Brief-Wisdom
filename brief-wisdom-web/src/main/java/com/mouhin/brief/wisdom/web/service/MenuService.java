package com.mouhin.brief.wisdom.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.persistence.mapper.SysMenuMapper;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper sysMenuMapper;

    /**
     * 获取所有可见菜单，按 sort_order 排序
     */
    public List<MenuDTO> listVisibleMenus() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getIsVisible, 1)
                        .orderByAsc(SysMenu::getSortOrder)
        ).stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有菜单（含隐藏）
     */
    public List<MenuDTO> listAllMenus() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        ).stream().map(this::toMenuDTO).toList();
    }

    /**
     * 根据 ID 查询菜单
     */
    public SysMenu getMenuById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    /**
     * 新增菜单
     */
    public void createMenu(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    /**
     * 更新菜单
     */
    public void updateMenu(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    /**
     * 删除菜单（逻辑删除）
     */
    public void deleteMenu(Long id) {
        sysMenuMapper.deleteById(id);
    }

    /**
     * 切换菜单显示/隐藏状态
     */
    public void toggleVisible(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu != null) {
            menu.setIsVisible(menu.getIsVisible() == 1 ? 0 : 1);
            sysMenuMapper.updateById(menu);
        }
    }

    private MenuDTO toMenuDTO(SysMenu m) {
        MenuDTO dto = new MenuDTO();
        dto.setId(m.getId());
        dto.setName(m.getName());
        dto.setUrl(m.getUrl());
        dto.setIcon(m.getIcon());
        dto.setTarget(m.getTarget());
        dto.setSortOrder(m.getSortOrder());
        dto.setIsVisible(m.getIsVisible());
        return dto;
    }
}
