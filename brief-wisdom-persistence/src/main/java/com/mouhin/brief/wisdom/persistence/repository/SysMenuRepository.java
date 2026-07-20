package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.SysMenuMapper;
import com.mouhin.brief.wisdom.persistence.model.SysMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统菜单数据访问层
 */

/**
 * SysMenuRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class SysMenuRepository {

    private final SysMenuMapper sysMenuMapper;

    /**
     * 查询所有可见菜单（按排序字段升序）
     *
     * @return 可见菜单列表
     */
    public List<SysMenu> findVisibleOrderBySortOrderAsc() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getIsVisible, 1)
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 查询无需权限控制的可见菜单（permission 为空 且 require_login=0）
     * 这些菜单对所有用户（包括未登录）可见
     */
    public List<SysMenu> findPublicVisibleMenus() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getIsVisible, 1)
                        .eq(SysMenu::getRequireLogin, 0)
                        .and(w -> w.isNull(SysMenu::getPermission).or().eq(SysMenu::getPermission, ""))
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 查询所有菜单（按排序字段升序，含隐藏）
     *
     * @return 全部菜单列表
     */
    public List<SysMenu> findAllOrderBySortOrderAsc() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 根据 ID 列表批量查询菜单
     *
     * @param ids 菜单 ID 列表
     * @return 菜单列表（按排序字段升序）
     */
    public List<SysMenu> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, ids)
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 查询指定父菜单下的隐藏子项（is_visible=0）
     * 用于页面 Tab 渲染（Tab 项不需要在导航栏显示，但需要返回给前端）
     */
    public List<SysMenu> findHiddenChildrenByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getParentId, parentIds)
                        .eq(SysMenu::getIsVisible, 0)
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 查询指定父菜单下的可见子菜单
     *
     * @param parentId 父菜单 ID
     * @return 子菜单列表
     */
    public List<SysMenu> findByParentId(Long parentId) {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getParentId, parentId)
                        .eq(SysMenu::getIsVisible, 1)
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    /**
     * 根据 ID 查询菜单
     *
     * @param id 菜单 ID
     * @return 匹配的菜单，不存在返回 null
     */
    public SysMenu findById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    /**
     * 保存新菜单
     *
     * @param menu 菜单实体
     */
    public void save(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    /**
     * 更新菜单
     *
     * @param menu 菜单实体
     */
    public void update(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    /**
     * 根据 ID 删除菜单（逻辑删除）
     *
     * @param id 菜单 ID
     */
    public void deleteById(Long id) {
        sysMenuMapper.deleteById(id);
    }
}
