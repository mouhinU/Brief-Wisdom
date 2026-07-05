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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI助手管理服务实现 - 按用户级别查询会话历史
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
     */
    @Override
    public List<UserDTO> listUsers() {
        List<ChatUser> users = chatUserRepository.findAllOrderByCreateTimeDesc();
        return users.stream().map(this::toUserDTO).collect(Collectors.toList());
    }

    /**
     * 按用户级别查询用户列表
     */
    @Override
    public List<UserDTO> listUsersByLevel(String userLevel) {
        List<ChatUser> users = chatUserRepository.findByUserLevelOrderByCreateTimeDesc(userLevel);
        return users.stream().map(this::toUserDTO).collect(Collectors.toList());
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
     */
    @Override
    @Cacheable(value = CachePrefix.AI_SESSION_CACHE, key = "'user:' + #userId")
    public List<SessionDTO> listSessionsByUserId(String userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);
        return sessions.stream().map(this::toSessionDTO).collect(Collectors.toList());
    }

    /**
     * 按用户级别查询会话列表（查询该级别下所有用户的会话）
     */
    @Override
    @Cacheable(value = CachePrefix.AI_SESSION_CACHE, key = "'level:' + #userLevel")
    public List<SessionDTO> listSessionsByUserLevel(String userLevel) {
        // 先查出该级别的所有用户
        List<ChatUser> users = chatUserRepository.findByUserLevel(userLevel);
        if (users.isEmpty()) {
            return List.of();
        }
        List<String> userIds = users.stream().map(ChatUser::getUserId).collect(Collectors.toList());

        // 查询这些用户的所有会话
        List<ChatSession> sessions = chatSessionRepository.findByUserIdsOrderByUpdateTimeDesc(userIds);
        return sessions.stream().map(this::toSessionDTO).collect(Collectors.toList());
    }

    /**
     * 获取会话的消息历史
     */
    @Override
    public List<MessageDTO> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

    // ===== 转换方法 =====

    private UserDTO toUserDTO(ChatUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setUserLevel(user.getUserLevel());
        dto.setCreateTime(user.getCreateTime());
        // 统计会话数（防御性处理，count 可能返回 null）
        long sessionCount = chatSessionRepository.countByUserId(user.getUserId());
        dto.setSessionCount((int) sessionCount);
        return dto;
    }

    private SessionDTO toSessionDTO(ChatSession session) {
        SessionDTO dto = new SessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setUserId(session.getUserId());
        dto.setTitle(session.getTitle());
        dto.setDescription(session.getDescription());
        dto.setMessageCount(session.getMessageCount());
        dto.setCreateTime(session.getCreateTime());
        LocalDateTime lastMsgTime = chatMessageRepository.findLastMessageTime(session.getSessionId());
        dto.setUpdateTime(lastMsgTime != null ? lastMsgTime : session.getUpdateTime());
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

    /**
     * 获取费用统计数据
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

        // 按用户统计
        List<Map<String, Object>> userRows = chatMessageRepository.getCostByUser();
        dto.getByUser().addAll(userRows.stream().map(row -> {
            CostStatisticsDTO.UserCostItem item = new CostStatisticsDTO.UserCostItem();
            item.setUserId((String) row.get("userId"));
            item.setMessageCount(toLong(row.get("messageCount")));
            item.setTotalCost(toDouble(row.get("totalCost")));
            item.setTotalTokens(toLong(row.get("totalTokens")));
            // 尝试查找用户名
            try {
                ChatUser user = chatUserRepository.findByUserId(item.getUserId());
                if (user != null) {
                    item.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                }
            } catch (Exception e) {
                log.info("查找用户名失败: {}", item.getUserId());
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
