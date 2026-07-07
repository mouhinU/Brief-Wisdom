package com.mouhin.brief.wisdom.ai.service.impl;

import com.mouhin.brief.wisdom.ai.service.AiManageService;
import com.mouhin.brief.wisdom.common.manage.CostStatisticsDTO;
import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.constants.CachePrefix;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.ChatMessageRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatSessionRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI助手管理服务实现 - 按用户级别查询会话历史
 * <p>
 * 已优化 N+1 查询问题：
 * <ul>
 *   <li>listUsers() 使用批量聚合查询统计会话数</li>
 *   <li>listSessionsByUserId() 使用批量查询获取最后消息时间</li>
 *   <li>getCostStatistics() 使用批量查询获取用户信息</li>
 * </ul>
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiManageServiceImpl implements AiManageService {

    private final ChatUserRepository chatUserRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 获取所有用户（含级别信息）
     * <p>
     * 优化：使用单次批量查询统计所有用户的会话数量，替代逐个 countByUserId。
     */
    @Override
    public List<UserDTO> listUsers() {
        List<ChatUser> users = chatUserRepository.findAllOrderByCreateTimeDesc();
        if (users.isEmpty()) {
            return List.of();
        }
        // 批量统计所有用户的会话数量（1 次查询替代 N 次）
        List<String> userIds = users.stream().map(ChatUser::getUserId).collect(Collectors.toList());
        Map<String, Long> sessionCountMap = chatSessionRepository.countSessionsGroupedByUserIds(userIds);

        return users.stream().map(user -> toUserDTO(user, sessionCountMap)).collect(Collectors.toList());
    }

    /**
     * 按用户级别查询用户列表
     */
    @Override
    public List<UserDTO> listUsersByLevel(String userLevel) {
        List<ChatUser> users = chatUserRepository.findByUserLevelOrderByCreateTimeDesc(userLevel);
        if (users.isEmpty()) {
            return List.of();
        }
        List<String> userIds = users.stream().map(ChatUser::getUserId).collect(Collectors.toList());
        Map<String, Long> sessionCountMap = chatSessionRepository.countSessionsGroupedByUserIds(userIds);

        return users.stream().map(user -> toUserDTO(user, sessionCountMap)).collect(Collectors.toList());
    }

    /**
     * 获取所有可用的用户级别
     */
    @Override
    public List<String> listUserLevels() {
        return List.of("admin", "vip", "normal");
    }

    /**
     * 查询指定用户的会话列表
     * <p>
     * 优化：使用单次批量查询获取所有会话的最后消息时间，替代逐个 findLastMessageTime。
     */
    @Override
    @Cacheable(value = CachePrefix.AI_SESSION_CACHE, key = "'user:' + #userId")
    public List<SessionDTO> listSessionsByUserId(String userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);
        return convertSessions(sessions);
    }

    /**
     * 按用户级别查询会话列表（查询该级别下所有用户的会话）
     */
    @Override
    @Cacheable(value = CachePrefix.AI_SESSION_CACHE, key = "'level:' + #userLevel")
    public List<SessionDTO> listSessionsByUserLevel(String userLevel) {
        List<ChatUser> users = chatUserRepository.findByUserLevel(userLevel);
        if (users.isEmpty()) {
            return List.of();
        }
        List<String> userIds = users.stream().map(ChatUser::getUserId).collect(Collectors.toList());
        List<ChatSession> sessions = chatSessionRepository.findByUserIdsOrderByUpdateTimeDesc(userIds);
        return convertSessions(sessions);
    }

    /**
     * 获取会话的消息历史
     */
    @Override
    public List<MessageDTO> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

    /**
     * 获取费用统计数据
     * <p>
     * 优化：使用单次批量查询获取所有用户信息，替代逐个 findByUserId。
     */
    @Override
    public CostStatisticsDTO getCostStatistics(int days) {
        log.info("获取费用统计, days={}", days);

        CostStatisticsDTO dto = new CostStatisticsDTO();

        // 总体统计
        Map<String, Object> overall = chatMessageRepository.getOverallStats();
        dto.setTotalCost(toDouble(overall.get("totalCost")));
        dto.setTotalTokens(toLong(overall.get("totalTokens")));
        dto.setTotalMessages(toLong(overall.get("totalMessages")));

        // 按模型统计
        List<Map<String, Object>> modelRows = chatMessageRepository.getCostByModel();
        dto.getByModel().addAll(modelRows.stream().map(row -> {
            CostStatisticsDTO.ModelCostItem item = new CostStatisticsDTO.ModelCostItem();
            item.setModel((String) row.get("model"));
            item.setMessageCount(toLong(row.get("messageCount")));
            item.setTotalCost(toDouble(row.get("totalCost")));
            item.setTotalTokens(toLong(row.get("totalTokens")));
            return item;
        }).toList());

        // 按用户统计 —— 优化：批量查询用户信息（1 次查询替代 N 次）
        List<Map<String, Object>> userRows = chatMessageRepository.getCostByUser();
        List<String> costUserIds = userRows.stream()
                .map(row -> (String) row.get("userId"))
                .collect(Collectors.toList());
        Map<String, ChatUser> userMap = batchLoadUserMap(costUserIds);

        dto.getByUser().addAll(userRows.stream().map(row -> {
            CostStatisticsDTO.UserCostItem item = new CostStatisticsDTO.UserCostItem();
            item.setUserId((String) row.get("userId"));
            item.setMessageCount(toLong(row.get("messageCount")));
            item.setTotalCost(toDouble(row.get("totalCost")));
            item.setTotalTokens(toLong(row.get("totalTokens")));
            ChatUser user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            return item;
        }).toList());

        // 按日期统计
        List<Map<String, Object>> dateRows = chatMessageRepository.getCostByDate(days);
        dto.getByDate().addAll(dateRows.stream().map(row -> {
            CostStatisticsDTO.DateCostItem item = new CostStatisticsDTO.DateCostItem();
            item.setDate(String.valueOf(row.get("date")));
            item.setMessageCount(toLong(row.get("messageCount")));
            item.setTotalCost(toDouble(row.get("totalCost")));
            item.setTotalTokens(toLong(row.get("totalTokens")));
            return item;
        }).toList());

        // 按模型+日期统计
        List<Map<String, Object>> dateModelRows = chatMessageRepository.getCostByDateAndModel(days);
        dto.getByDateAndModel().addAll(dateModelRows.stream().map(row -> {
            CostStatisticsDTO.DateModelCostItem item = new CostStatisticsDTO.DateModelCostItem();
            item.setDate(String.valueOf(row.get("date")));
            item.setModel((String) row.get("model"));
            item.setMessageCount(toLong(row.get("messageCount")));
            item.setTotalCost(toDouble(row.get("totalCost")));
            return item;
        }).toList());

        return dto;
    }

    // ===== 批量转换方法 =====

    /**
     * 批量转换会话列表（一次性查询所有最后消息时间）
     */
    private List<SessionDTO> convertSessions(List<ChatSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        // 批量获取所有会话的最后消息时间（1 次查询替代 N 次）
        List<String> sessionIds = sessions.stream().map(ChatSession::getSessionId).collect(Collectors.toList());
        Map<String, LocalDateTime> lastTimeMap = batchLoadLastMessageTimes(sessionIds);

        return sessions.stream().map(session -> {
            SessionDTO dto = new SessionDTO();
            dto.setSessionId(session.getSessionId());
            dto.setUserId(session.getUserId());
            dto.setTitle(session.getTitle());
            dto.setDescription(session.getDescription());
            dto.setMessageCount(session.getMessageCount());
            dto.setCreateTime(session.getCreateTime());
            LocalDateTime lastMsgTime = lastTimeMap.get(session.getSessionId());
            dto.setUpdateTime(lastMsgTime != null ? lastMsgTime : session.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 批量加载用户信息映射（单次 IN 查询）
     */
    private Map<String, ChatUser> batchLoadUserMap(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<ChatUser> users = chatUserRepository.findByUserIdIn(userIds);
        Map<String, ChatUser> map = new HashMap<>(users.size());
        for (ChatUser u : users) {
            map.put(u.getUserId(), u);
        }
        return map;
    }

    /**
     * 批量加载会话最后消息时间映射（单次聚合查询）
     */
    private Map<String, LocalDateTime> batchLoadLastMessageTimes(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = chatMessageRepository.findLastMessageTimesBySessionIds(sessionIds);
        Map<String, LocalDateTime> map = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            String sid = (String) row.get("session_id");
            Object lastTime = row.get("last_time");
            if (sid != null && lastTime instanceof LocalDateTime) {
                map.put(sid, (LocalDateTime) lastTime);
            }
        }
        return map;
    }

    // ===== 单条转换方法 =====

    private UserDTO toUserDTO(ChatUser user, Map<String, Long> sessionCountMap) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setUserLevel(user.getUserLevel());
        dto.setCreateTime(user.getCreateTime());
        Long sessionCount = sessionCountMap.getOrDefault(user.getUserId(), 0L);
        dto.setSessionCount(sessionCount.intValue());
        return dto;
    }

    private MessageDTO toMessageDTO(ChatMessage msg) {
        MessageDTO dto = new MessageDTO();
        dto.setId(msg.getId());
        dto.setSessionId(msg.getSessionId());
        dto.setUserId(msg.getUserId());
        dto.setRole(msg.getRole());
        dto.setContent(msg.getContent());
        dto.setModel(msg.getModel());
        dto.setTokens(msg.getTokens());
        dto.setCost(msg.getCost());
        dto.setTimestamp(msg.getTimestamp());
        dto.setMessageType(msg.getMessageType());
        return dto;
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
