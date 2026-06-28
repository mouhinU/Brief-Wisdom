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
@Repository
@RequiredArgsConstructor
public class ChatUserRepository {

    private final ChatUserMapper chatUserMapper;

    public ChatUser findByUserId(String userId) {
        return chatUserMapper.selectOne(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUserId, userId)
        );
    }

    public ChatUser findByUsername(String username) {
        return chatUserMapper.selectByUsername(username);
    }

    /**
     * 查找用户（包含已逻辑删除的记录，绕过 @TableLogic）
     */
    public ChatUser findByUserIdIncludeDeleted(String userId) {
        return chatUserMapper.selectByUserIdIncludeDeleted(userId);
    }

    public ChatUser findById(Long id) {
        return chatUserMapper.selectById(id);
    }

    public List<ChatUser> findAllOrderByCreateTimeDesc() {
        return chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .orderByDesc(ChatUser::getCreateTime)
        );
    }

    public List<ChatUser> findByUserLevelOrderByCreateTimeDesc(String userLevel) {
        LambdaQueryWrapper<ChatUser> query = new LambdaQueryWrapper<ChatUser>()
                .orderByDesc(ChatUser::getCreateTime);
        if (userLevel != null && !userLevel.isEmpty()) {
            query.eq(ChatUser::getUserLevel, userLevel);
        }
        return chatUserMapper.selectList(query);
    }

    public List<ChatUser> findByUserLevel(String userLevel) {
        return chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUserLevel, userLevel)
        );
    }

    public Page<ChatUser> findPage(int page, int size, LambdaQueryWrapper<ChatUser> query) {
        Page<ChatUser> pageParam = new Page<>(page, size);
        return chatUserMapper.selectPage(pageParam, query);
    }

    public void save(ChatUser user) {
        chatUserMapper.insert(user);
    }

    public void update(ChatUser user) {
        chatUserMapper.updateById(user);
    }

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
