package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.common.menu.MenuDTO;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import com.mouhin.brief.wisdom.persistence.repository.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuRepository sysMenuRepository;

    /**
     * 获取所有可见菜单，按 sort_order 排序
     */
    public List<MenuDTO> listVisibleMenus() {
        return sysMenuRepository.findVisibleOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
    }

    /**
     * 获取所有菜单（含隐藏）
     */
    public List<MenuDTO> listAllMenus() {
        return sysMenuRepository.findAllOrderBySortOrderAsc().stream().map(this::toMenuDTO).toList();
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
    public void deleteMenu(Long id) {
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
