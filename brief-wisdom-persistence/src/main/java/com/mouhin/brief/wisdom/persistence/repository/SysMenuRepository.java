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
@Repository
@RequiredArgsConstructor
public class SysMenuRepository {

    private final SysMenuMapper sysMenuMapper;

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

    public List<SysMenu> findAllOrderBySortOrderAsc() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

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

    public List<SysMenu> findByParentId(Long parentId) {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getParentId, parentId)
                        .eq(SysMenu::getIsVisible, 1)
                        .orderByAsc(SysMenu::getSortOrder)
        );
    }

    public SysMenu findById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    public void save(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    public void update(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    public void deleteById(Long id) {
        sysMenuMapper.deleteById(id);
    }
}
