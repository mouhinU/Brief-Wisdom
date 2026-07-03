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

    public List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getTimestamp)
        );
    }

    public Page<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId, int page, int size) {
        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getTimestamp);
        return chatMessageMapper.selectPage(pageParam, queryWrapper);
    }

    public long countByUserId(String userId) {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, userId)
        );
    }

    public long countBySessionId(String sessionId) {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );
    }

    public List<ChatMessage> findRecentMessages(String sessionId, int limit) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getTimestamp)
                        .last("LIMIT " + limit)
        );
    }

    public LocalDateTime findLastMessageTime(String sessionId) {
        return chatMessageMapper.selectLastMessageTime(sessionId);
    }

    public List<Map<String, Object>> findMessageCountsByUserId(String userId) {
        return chatMessageMapper.selectMessageCountsByUserId(userId);
    }

    public List<Map<String, Object>> findLastMessageTimesByUserId(String userId) {
        return chatMessageMapper.selectLastMessageTimesByUserId(userId);
    }

    public void save(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    // ========== 费用统计 ==========

    public Map<String, Object> getOverallStats() {
        return chatMessageMapper.selectOverallStats();
    }

    public List<Map<String, Object>> getCostByModel() {
        return chatMessageMapper.selectCostByModel();
    }

    public List<Map<String, Object>> getCostByUser() {
        return chatMessageMapper.selectCostByUser();
    }

    public List<Map<String, Object>> getCostByDate(int days) {
        return chatMessageMapper.selectCostByDate(days);
    }

    public List<Map<String, Object>> getCostByDateAndModel(int days) {
        return chatMessageMapper.selectCostByDateAndModel(days);
    }
}
