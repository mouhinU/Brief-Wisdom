package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.ChatSessionMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天会话数据访问层
 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepository {

    private final ChatSessionMapper chatSessionMapper;

    public List<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId) {
        return chatSessionMapper.selectByUserIdOrderByUpdateTimeDesc(userId);
    }

    public Page<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId, int page, int size) {
        Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectPage(pageParam, queryWrapper);
    }

    public List<ChatSession> findByUserIdsOrderByUpdateTimeDesc(List<String> userIds) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .in(ChatSession::getUserId, userIds)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    public long countByUserId(String userId) {
        return chatSessionMapper.countByUserId(userId);
    }

    public ChatSession findBySessionId(String sessionId) {
        return chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
        );
    }

    public void save(ChatSession session) {
        chatSessionMapper.insert(session);
    }

    public void update(ChatSession session) {
        chatSessionMapper.updateById(session);
    }

    public void deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getSessionId, sessionId);
        chatSessionMapper.delete(qw);
    }
}
