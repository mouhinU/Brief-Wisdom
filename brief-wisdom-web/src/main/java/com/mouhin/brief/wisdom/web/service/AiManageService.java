package com.mouhin.brief.wisdom.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.mapper.ChatMessageMapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatSessionMapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatUserMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI助手管理服务 - 按用户级别查询会话历史
 */
@Service
@RequiredArgsConstructor
public class AiManageService {

    private final ChatUserMapper chatUserMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 获取所有用户（含级别信息）
     */
    public List<UserDTO> listUsers() {
        List<ChatUser> users = chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .orderByDesc(ChatUser::getCreateTime)
        );
        return users.stream().map(this::toUserDTO).collect(Collectors.toList());
    }

    /**
     * 按用户级别查询用户列表
     */
    public List<UserDTO> listUsersByLevel(String userLevel) {
        LambdaQueryWrapper<ChatUser> query = new LambdaQueryWrapper<ChatUser>()
                .orderByDesc(ChatUser::getCreateTime);
        if (userLevel != null && !userLevel.isEmpty()) {
            query.eq(ChatUser::getUserLevel, userLevel);
        }
        List<ChatUser> users = chatUserMapper.selectList(query);
        return users.stream().map(this::toUserDTO).collect(Collectors.toList());
    }

    /**
     * 获取所有可用的用户级别
     */
    public List<String> listUserLevels() {
        return List.of("admin", "vip", "normal");
    }

    /**
     * 查询指定用户的会话列表
     */
    public List<SessionDTO> listSessionsByUserId(String userId) {
        List<ChatSession> sessions = chatSessionMapper.selectByUserIdOrderByUpdateTimeDesc(userId);
        return sessions.stream().map(this::toSessionDTO).collect(Collectors.toList());
    }

    /**
     * 按用户级别查询会话列表（查询该级别下所有用户的会话）
     */
    public List<SessionDTO> listSessionsByUserLevel(String userLevel) {
        // 先查出该级别的所有用户
        List<ChatUser> users = chatUserMapper.selectList(
                new LambdaQueryWrapper<ChatUser>()
                        .eq(ChatUser::getUserLevel, userLevel)
        );
        if (users.isEmpty()) {
            return List.of();
        }
        List<String> userIds = users.stream().map(ChatUser::getUserId).collect(Collectors.toList());

        // 查询这些用户的所有会话
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .in(ChatSession::getUserId, userIds)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
        return sessions.stream().map(this::toSessionDTO).collect(Collectors.toList());
    }

    /**
     * 获取会话的消息历史
     */
    public List<MessageDTO> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectBySessionIdOrderByTimestampAsc(sessionId);
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
        // 统计会话数
        dto.setSessionCount(Math.toIntExact(chatSessionMapper.countByUserId(user.getUserId())));
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
        LocalDateTime lastMsgTime = chatMessageMapper.selectLastMessageTime(session.getSessionId());
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
}
