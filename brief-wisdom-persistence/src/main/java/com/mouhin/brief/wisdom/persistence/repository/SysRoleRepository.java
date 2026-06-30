package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.SysRoleMapper;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

    public List<SysRole> findAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public List<SysRole> findAllEnabled() {
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId)
        );
    }

    public SysRole findById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    public SysRole findByRoleKey(String roleKey) {
        return sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey)
        );
    }

    public void save(SysRole role) {
        sysRoleMapper.insert(role);
    }

    public void update(SysRole role) {
        sysRoleMapper.updateById(role);
    }

    public void deleteById(Long id) {
        sysRoleMapper.deleteById(id);
    }
}
