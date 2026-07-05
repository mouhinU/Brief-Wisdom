package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.UserRoleMapper;
import com.mouhin.brief.wisdom.persistence.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-角色关联数据访问层
 */
/**
 * UserRoleRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class UserRoleRepository {

    private final UserRoleMapper userRoleMapper;

    /**
     * 查询指定用户的所有用户-角色关联
     *
     * @param userId 用户 ID
     * @return 用户-角色关联列表
     */
    public List<UserRole> findByUserId(String userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
    }

    /**
     * 保存用户-角色关联
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    public void save(String userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    /**
     * 删除指定用户的所有角色关联
     *
     * @param userId 用户 ID
     */
    public void deleteByUserId(String userId) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
    }

    /**
     * 删除指定用户的指定角色关联
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    public void deleteByUserIdAndRoleId(String userId, Long roleId) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, roleId)
        );
    }

    /**
     * 统计拥有某个角色的用户数量
     */
    public long countByRoleId(Long roleId) {
        return userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)
        );
    }
}
