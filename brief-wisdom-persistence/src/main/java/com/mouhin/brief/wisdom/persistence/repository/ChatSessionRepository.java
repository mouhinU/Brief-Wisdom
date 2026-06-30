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
/**
 * ChatSessionRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepository {

    private final ChatSessionMapper chatSessionMapper;

    public List<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
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
        return chatSessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
        );
    }

    public ChatSession findBySessionId(String sessionId) {
        return chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
        );
    }

    /**
     * 查找用户在指定页面上下文下的最近会话（按更新时间倒序）
     *
     * @param userId      用户ID
     * @param pageContext 页面上下文（如 /about.html）
     * @param limit       最多返回的会话数量
     * @return 会话列表（按更新时间倒序）
     */
    public List<ChatSession> findRecentByUserIdAndPageContext(String userId, String pageContext, int limit) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getPageContext, pageContext)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT " + limit);
        return chatSessionMapper.selectList(queryWrapper);
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
