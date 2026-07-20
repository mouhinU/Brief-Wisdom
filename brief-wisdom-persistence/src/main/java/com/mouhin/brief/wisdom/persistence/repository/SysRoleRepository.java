package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.SysRoleMapper;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 系统角色数据访问层
 */

/**
 * SysRoleRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class SysRoleRepository {

    private final SysRoleMapper sysRoleMapper;

    /**
     * 查询所有角色（按 ID 升序）
     *
     * @return 角色列表
     */
    public List<SysRole> findAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    /**
     * 查询所有启用的角色
     *
     * @return 启用的角色列表
     */
    public List<SysRole> findAllEnabled() {
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId)
        );
    }

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 匹配的角色，不存在返回 null
     */
    public SysRole findById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    /**
     * 根据角色 Key 查询角色
     *
     * @param roleKey 角色标识
     * @return 匹配的角色，不存在返回 null
     */
    public SysRole findByRoleKey(String roleKey) {
        return sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey)
        );
    }

    /**
     * 保存新角色
     *
     * @param role 角色实体
     */
    public void save(SysRole role) {
        sysRoleMapper.insert(role);
    }

    /**
     * 更新角色
     *
     * @param role 角色实体
     */
    public void update(SysRole role) {
        sysRoleMapper.updateById(role);
    }

    /**
     * 根据 ID 删除角色（逻辑删除）
     *
     * @param id 角色 ID
     */
    public void deleteById(Long id) {
        sysRoleMapper.deleteById(id);
    }

    /**
     * 根据 ID 列表批量查询角色
     */
    public List<SysRole> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectBatchIds(ids);
    }
}
