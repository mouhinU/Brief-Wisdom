package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.ChatSessionMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    /**
     * 查询指定用户的所有会话（按更新时间降序）
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    /**
     * 分页查询指定用户的会话（按更新时间降序）
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页大小
     * @return 分页结果
     */
    public Page<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId, int page, int size) {
        Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 批量查询多个用户的所有会话（按更新时间降序）
     *
     * @param userIds 用户 ID 列表
     * @return 会话列表
     */
    public List<ChatSession> findByUserIdsOrderByUpdateTimeDesc(List<String> userIds) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .in(ChatSession::getUserId, userIds)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    /**
     * 统计指定用户的会话数量
     *
     * @param userId 用户 ID
     * @return 会话数量
     */
    public long countByUserId(String userId) {
        return chatSessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
        );
    }

    /**
     * 统计所有会话数量
     *
     * @return 会话总数
     */
    public long countAll() {
        return chatSessionMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /**
     * 批量统计多个用户的会话数量（单次查询 + 内存分组计数）
     *
     * @param userIds 用户 ID 列表
     * @return 每个用户 ID 对应的会话数量映射
     */
    public Map<String, Long> countSessionsGroupedByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .select(ChatSession::getUserId)
                        .in(ChatSession::getUserId, userIds)
        );
        Map<String, Long> result = new java.util.HashMap<>(userIds.size());
        for (ChatSession s : sessions) {
            result.merge(s.getUserId(), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 根据会话 ID 查询会话
     *
     * @param sessionId 会话 ID
     * @return 匹配的会话，不存在返回 null
     */
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

    /**
     * 保存新会话
     *
     * @param session 会话实体
     */
    public void save(ChatSession session) {
        chatSessionMapper.insert(session);
    }

    /**
     * 更新会话
     *
     * @param session 会话实体
     */
    public void update(ChatSession session) {
        chatSessionMapper.updateById(session);
    }

    /**
     * 根据会话 ID 删除会话
     *
     * @param sessionId 会话 ID
     */
    public void deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getSessionId, sessionId);
        chatSessionMapper.delete(qw);
    }
}
