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

    /**
     * 查询指定角色的所有角色-菜单关联
     *
     * @param roleId 角色 ID
     * @return 角色-菜单关联列表
     */
    public List<RoleMenu> findByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
    }

    /**
     * 查询指定角色拥有的菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return findByRoleId(roleId).stream().map(RoleMenu::getMenuId).toList();
    }

    /**
     * 保存角色-菜单关联
     *
     * @param roleId 角色 ID
     * @param menuId 菜单 ID
     */
    public void save(Long roleId, Long menuId) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        roleMenuMapper.insert(roleMenu);
    }

    /**
     * 删除指定角色的所有菜单关联
     *
     * @param roleId 角色 ID
     */
    public void deleteByRoleId(Long roleId) {
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        );
    }

    /**
     * 删除指定菜单的所有角色关联
     *
     * @param menuId 菜单 ID
     */
    public void deleteByMenuId(Long menuId) {
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, menuId)
        );
    }
}
