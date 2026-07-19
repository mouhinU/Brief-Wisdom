package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.persistence.mapper.ChatMessageMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息数据访问层
 */
/**
 * ChatMessageRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

    private final ChatMessageMapper chatMessageMapper;

    /**
     * 查询指定会话的消息列表（按时间戳升序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getTimestamp)
        );
    }

    /**
     * 分页查询指定会话的消息列表（按时间戳升序）
     *
     * @param sessionId 会话 ID
     * @param page      页码（从 1 开始）
     * @param size      每页大小
     * @return 分页结果
     */
    public Page<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId, int page, int size) {
        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getTimestamp);
        return chatMessageMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 分页查询指定会话的消息列表（按时间戳降序）
     *
     * @param sessionId 会话 ID
     * @param page      页码（从 1 开始）
     * @param size      每页大小
     * @return 分页结果
     */
    public Page<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId, int page, int size) {
        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getTimestamp);
        return chatMessageMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 统计指定用户的消息数量
     *
     * @param userId 用户 ID
     * @return 消息数量
     */
    public long countByUserId(String userId) {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, userId)
        );
    }

    /**
     * 统计指定会话的消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    public long countBySessionId(String sessionId) {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );
    }

    /**
     * 统计所有消息数量
     *
     * @return 消息总数
     */
    public long countAll() {
        return chatMessageMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /**
     * 查询指定会话的最近 N 条消息
     *
     * @param sessionId 会话 ID
     * @param limit     最大返回条数
     * @return 消息列表（按时间戳降序）
     */
    public List<ChatMessage> findRecentMessages(String sessionId, int limit) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getTimestamp)
                        .last("LIMIT " + limit)
        );
    }

    /**
     * 查询指定会话的第一条用户消息（用于自动生成会话标题）
     *
     * @param sessionId 会话 ID
     * @return 第一条用户消息，不存在返回 null
     */
    public ChatMessage findFirstUserMessage(String sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getRole, "user")
                        .orderByAsc(ChatMessage::getTimestamp)
                        .last("LIMIT 1")
        );
        return messages.isEmpty() ? null : messages.get(0);
    }

    /**
     * 查询指定会话的最后一条消息时间
     *
     * @param sessionId 会话 ID
     * @return 最后一条消息的时间，不存在返回 null
     */
    public LocalDateTime findLastMessageTime(String sessionId) {
        return chatMessageMapper.selectLastMessageTime(sessionId);
    }

    /**
     * 批量获取指定用户所有会话的消息数量
     *
     * @param userId 用户 ID
     * @return 会话 ID 与消息数量的映射列表
     */
    public List<Map<String, Object>> findMessageCountsByUserId(String userId) {
        return chatMessageMapper.selectMessageCountsByUserId(userId);
    }

    /**
     * 批量获取指定用户所有会话的最后消息时间
     *
     * @param userId 用户 ID
     * @return 会话 ID 与最后消息时间的映射列表
     */
    public List<Map<String, Object>> findLastMessageTimesByUserId(String userId) {
        return chatMessageMapper.selectLastMessageTimesByUserId(userId);
    }

    /**
     * 批量获取多个会话的最后消息时间（单次聚合查询）
     *
     * @param sessionIds 会话 ID 列表
     * @return 每行包含 session_id 和 last_time 的映射列表
     */
    public List<Map<String, Object>> findLastMessageTimesBySessionIds(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return chatMessageMapper.selectLastMessageTimesBySessionIds(sessionIds);
    }

    /**
     * 保存消息
     *
     * @param message 消息实体
     */
    public void save(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    /**
     * 根据 ID 查询消息
     *
     * @param id 消息 ID
     * @return 消息实体，不存在返回 null
     */
    public ChatMessage findById(Long id) {
        return chatMessageMapper.selectById(id);
    }

    /**
     * 更新消息
     *
     * @param message 消息实体（必须包含 ID）
     */
    public void update(ChatMessage message) {
        chatMessageMapper.updateById(message);
    }

    // ========== 费用统计 ==========

    /**
     * 获取全局费用统计概览（总费用、总 Token 数、总消息数）
     *
     * @return 统计数据映射
     */
    public Map<String, Object> getOverallStats() {
        return chatMessageMapper.selectOverallStats();
    }

    /**
     * 按模型分组统计费用
     *
     * @return 各模型费用统计列表
     */
    public List<Map<String, Object>> getCostByModel() {
        return chatMessageMapper.selectCostByModel();
    }

    /**
     * 按用户分组统计费用
     *
     * @return 各用户费用统计列表
     */
    public List<Map<String, Object>> getCostByUser() {
        return chatMessageMapper.selectCostByUser();
    }

    /**
     * 按日期分组统计费用（最近 N 天）
     *
     * @param days 统计天数
     * @return 各日期费用统计列表
     */
    public List<Map<String, Object>> getCostByDate(int days) {
        return chatMessageMapper.selectCostByDate(days);
    }

    /**
     * 按模型和日期分组统计费用（最近 N 天）
     *
     * @param days 统计天数
     * @return 各日期各模型费用统计列表
     */
    public List<Map<String, Object>> getCostByDateAndModel(int days) {
        return chatMessageMapper.selectCostByDateAndModel(days);
    }
}
