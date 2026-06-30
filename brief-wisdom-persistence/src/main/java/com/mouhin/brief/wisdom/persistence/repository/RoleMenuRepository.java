package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.RoleMenuMapper;
import com.mouhin.brief.wisdom.persistence.model.RoleMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-菜单关联数据访问层
 */
/**
 * RoleMenuRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class RoleMenuRepository {

    private final RoleMenuMapper roleMenuMapper;

    public List<RoleMenu> findByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
    }

    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return findByRoleId(roleId).stream().map(RoleMenu::getMenuId).toList();
    }

    public void save(Long roleId, Long menuId) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        roleMenuMapper.insert(roleMenu);
    }

    public void deleteByRoleId(Long roleId) {
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
    }

    public void deleteByMenuId(Long menuId) {
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, menuId)
        );
    }
}
