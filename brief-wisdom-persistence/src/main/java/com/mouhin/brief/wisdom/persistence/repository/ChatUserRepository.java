package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.ChatUserMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天用户数据访问层
 */
/**
 * ChatUserRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class ChatUserRepository {

    private final ChatUserMapper chatUserMapper;

    /**
     * 根据用户 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 匹配的用户，不存在返回 null
     */
    public ChatUser findByUserId(String userId) {
        return chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUserId, userId)
        );
    }

    /**
     * 批量根据用户 ID 查询用户（单次 IN 查询）
     *
     * @param userIds 用户 ID 列表
     * @return 匹配的用户列表
     */
    public List<ChatUser> findByUserIdIn(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .in(ChatUser::getUserId, userIds)
        );
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 匹配的用户，不存在返回 null
     */
    public ChatUser findByUsername(String username) {
        return chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUsername, username)
        );
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 匹配的用户，不存在返回 null
     */
    public ChatUser findByPhone(String phone) {
        return chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getPhone, phone)
        );
    }

    /**
     * 查找用户（包含已逻辑删除的记录，绕过 @TableLogic）
     */
    public ChatUser findByUserIdIncludeDeleted(String userId) {
        return chatUserMapper.selectByUserIdIncludeDeleted(userId);
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 主键 ID
     * @return 匹配的用户，不存在返回 null
     */
    public ChatUser findById(Long id) {
        return chatUserMapper.selectById(id);
    }

    /**
     * 查询所有用户（按创建时间降序）
     *
     * @return 用户列表
     */
    public List<ChatUser> findAllOrderByCreateTimeDesc() {
        return chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .orderByDesc(ChatUser::getCreateTime)
        );
    }

    /**
     * 按用户级别查询用户（按创建时间降序，支持空级别查全部）
     *
     * @param userLevel 用户级别，为 null 或空时查询所有用户
     * @return 用户列表
     */
    public List<ChatUser> findByUserLevelOrderByCreateTimeDesc(String userLevel) {
        LambdaQueryWrapper<ChatUser> query = new LambdaQueryWrapper<ChatUser>()
                .orderByDesc(ChatUser::getCreateTime);
        if (userLevel != null && !userLevel.isEmpty()) {
            query.eq(ChatUser::getUserLevel, userLevel);
        }
        return chatUserMapper.selectList(query);
    }

    /**
     * 按用户级别查询用户
     *
     * @param userLevel 用户级别
     * @return 用户列表
     */
    public List<ChatUser> findByUserLevel(String userLevel) {
        return chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUserLevel, userLevel)
        );
    }

    /**
     * 分页查询用户
     *
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @param query 查询条件
     * @return 分页结果
     */
    public Page<ChatUser> findPage(int page, int size, LambdaQueryWrapper<ChatUser> query) {
        Page<ChatUser> pageParam = new Page<>(page, size);
        return chatUserMapper.selectPage(pageParam, query);
    }

    /**
     * 保存新用户
     *
     * @param user 用户实体
     */
    public void save(ChatUser user) {
        chatUserMapper.insert(user);
    }

    /**
     * 更新用户
     *
     * @param user 用户实体
     */
    public void update(ChatUser user) {
        chatUserMapper.updateById(user);
    }

    /**
     * 根据 ID 删除用户（逻辑删除）
     *
     * @param id 主键 ID
     */
    public void deleteById(Long id) {
        chatUserMapper.deleteById(id);
    }

    /**
     * 硬删除用户（绕过 @TableLogic，物理删除记录及其级联数据）
     */
    public void hardDeleteByUserId(String userId) {
        chatUserMapper.hardDeleteByUserId(userId);
    }
}
