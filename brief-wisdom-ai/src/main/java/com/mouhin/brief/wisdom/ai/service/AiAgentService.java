package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.mapper.ChatMessageMapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatSessionMapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatUserMapper;
import com.mouhin.brief.wisdom.persistence.mapper.AiModelMapper;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AiAgentService {

    private final ChatClient chatClient;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatUserMapper userMapper;
    private final AiModelMapper aiModelMapper;
    
    // 默认用户ID（用于未登录场景）
    private static final String DEFAULT_USER_ID = "default-user";

    public AiAgentService(ChatClient chatClient, 
                         ChatSessionMapper sessionMapper,
                         ChatMessageMapper messageMapper,
                         ChatUserMapper userMapper,
                         AiModelMapper aiModelMapper) {
        this.chatClient = chatClient;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.aiModelMapper = aiModelMapper;
        
        // 初始化默认用户
        initDefaultUser();
    }
    
    /**
     * 初始化默认用户
     */
    private void initDefaultUser() {
        LambdaQueryWrapper<ChatUser> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatUser::getUserId, DEFAULT_USER_ID);
        ChatUser existingUser = userMapper.selectOne(qw);
        if (existingUser == null) {
            ChatUser defaultUser = new ChatUser();
            defaultUser.setUserId(DEFAULT_USER_ID);
            defaultUser.setUsername("guest");
            defaultUser.setNickname("访客");
            userMapper.insert(defaultUser);
            log.info("创建默认用户: {}", DEFAULT_USER_ID);
        }
    }

    /**
     * 创建新会话
     * @return 会话ID
     */
    @Transactional
    public String createSession() {
        return createSession(DEFAULT_USER_ID);
    }
    
    /**
     * 为指定用户创建新会话
     * @param userId 用户ID
     * @return 会话ID
     */
    @Transactional
    public String createSession(String userId) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle("新会话");
        session.setMessageCount(0);
        
        sessionMapper.insert(session);
        log.info("为用户 {} 创建新会话: {}", userId, session.getSessionId());
        return session.getSessionId();
    }

    /**
     * 删除会话（逻辑删除）
     * @param sessionId 会话ID
     */
    @Transactional
    public void deleteSession(String sessionId) {
        // 通过 sessionId 业务键进行逻辑删除
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getSessionId, sessionId);
        sessionMapper.delete(qw);
        log.info("删除会话: {}", sessionId);
    }

    /**
     * 获取所有会话列表
     * @return 会话元数据列表
     */
    public List<SessionMetaDTO> listSessions() {
        return listSessions(DEFAULT_USER_ID);
    }
    
    /**
     * 获取指定用户的会话列表
     * @param userId 用户ID
     * @return 会话元数据列表
     */
    public List<SessionMetaDTO> listSessions(String userId) {
        List<ChatSession> sessions = sessionMapper.selectByUserIdOrderByUpdateTimeDesc(userId);
        return sessions.stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());
            
            // 使用最后一条消息的时间作为更新时间
            LocalDateTime lastMessageTime = messageMapper.selectLastMessageTime(session.getSessionId());
            meta.setUpdateTime(lastMessageTime != null ? lastMessageTime : session.getUpdateTime());
            
            return meta;
        }).toList();
    }

    /**
     * 分页获取会话列表
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public PageResult<SessionMetaDTO> listSessionsPaged(int page, int size) {
        return listSessionsPaged(DEFAULT_USER_ID, page, size);
    }

    /**
     * 分页获取指定用户的会话列表
     * @param userId 用户ID
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public PageResult<SessionMetaDTO> listSessionsPaged(String userId, int page, int size) {
        // 使用 MyBatis-Plus 分页查询
        Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);

        Page<ChatSession> pageResult = sessionMapper.selectPage(pageParam, queryWrapper);

        // 转换为 SessionMeta
        List<SessionMetaDTO> sessionMetas = pageResult.getRecords().stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());

            LocalDateTime lastMessageTime = messageMapper.selectLastMessageTime(session.getSessionId());
            meta.setUpdateTime(lastMessageTime != null ? lastMessageTime : session.getUpdateTime());

            return meta;
        }).toList();

        // 封装分页结果
        PageResult<SessionMetaDTO> result = new PageResult<>();
        result.setRecords(sessionMetas);
        result.setTotal(pageResult.getTotal());
        result.setPage(pageResult.getCurrent());
        result.setSize(pageResult.getSize());
        result.setPages(pageResult.getPages());
        result.setHasMore(pageResult.getCurrent() < pageResult.getPages());

        return result;
    }

    /**
     * 获取会话历史消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ChatMessageDTO> getSessionHistory(String sessionId) {
        List<ChatMessage> messages = messageMapper.selectBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream().map(msg -> {
            ChatMessageDTO dto = new ChatMessageDTO();
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
        }).toList();
    }

    /**
     * 获取当前激活的模型名称
     */
    public String getActiveModelName() {
        try {
            AiModel model = aiModelMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiModel>()
                            .eq(AiModel::getIsActive, 1)
                            .eq(AiModel::getIsEnabled, 1)
            );
            return model != null ? model.getModelName() : "qwen-plus";
        } catch (Exception e) {
            log.warn("获取激活模型失败，使用默认模型: ", e);
            return "qwen-plus";
        }
    }

    /**
     * 构建带模型选项的 DashScopeChatOptions
     */
    private DashScopeChatOptions buildModelOptions(String modelName) {
        return DashScopeChatOptions.builder()
                .withModel(modelName)
                .build();
    }

    /**
     * 简单聊天对话（无上下文）
     * @param message 用户消息
     * @return AI 回复
     */
    public String chat(String message) {
        return chat(message, null);
    }

    /**
     * 简单聊天对话（指定模型）
     */
    public String chat(String message, String modelName) {
        log.info("收到用户消息: {}, 模型: {}", message, modelName);
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();
        
        String response = chatClient.prompt()
                .options(buildModelOptions(model))
                .user(message)
                .call()
                .content();
        
        log.info("AI 回复(模型: {}): {}", model, response);
        return response;
    }

    /**
     * 带上下文的聊天对话
     * @param sessionId 会话ID
     * @param message 用户消息
     * @return AI 回复
     */
    @Transactional
    public String chatWithSession(String sessionId, String message) {
        return chatWithSession(sessionId, DEFAULT_USER_ID, message, null);
    }
    
    /**
     * 带上下文的聊天对话（指定用户和模型）
     */
    @Transactional
    public String chatWithSession(String sessionId, String userId, String message, String modelName) {
        log.info("========== 开始处理聊天请求 ==========");
        log.info("sessionId: {}, userId: {}, model: {}", sessionId, userId, modelName);
        
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();
        
        // 验证会话是否存在且属于该用户
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = sessionMapper.selectOne(qw);
        log.info("查询到的 session: {}", session);
        
        if (session == null) {
            log.error("会话不存在: {}", sessionId);
            throw new RuntimeException("会话不存在: " + sessionId);
        }
        
        if (!session.getUserId().equals(userId)) {
            log.error("无权访问此会话，session.userId: {}, userId: {}", session.getUserId(), userId);
            throw new RuntimeException("无权访问此会话");
        }
        
        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setMessageType("text");
        messageMapper.insert(userMsg);
        
        // 获取最近10条消息作为上下文
        List<ChatMessage> recentMessages = messageMapper.selectRecentMessages(sessionId, 10);
        Collections.reverse(recentMessages);  // 反转为正序
        
        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        
        // 调用 AI
        String response = chatClient.prompt()
                .options(buildModelOptions(model))
                .user(context.toString())
                .call()
                .content();
        
        // 保存 AI 回复
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(response);
        aiMsg.setModel(model);  // 记录使用的模型
        aiMsg.setMessageType("text");
        messageMapper.insert(aiMsg);
        
        // 更新会话统计信息
        long messageCount = messageMapper.countBySessionId(sessionId);
        session.setMessageCount((int) messageCount);
        
        // 如果是第一条消息，用用户消息作为标题
        if (messageCount == 2) {
            session.setTitle(message.length() > 30 ? message.substring(0, 30) + "..." : message);
        }
        
        // 手动设置更新时间为当前时间（即最后一条消息的时间）
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        
        log.info("AI 回复: {}", response);
        return response;
    }

    /**
     * 带系统提示的对话
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI 回复
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        log.info("系统提示: {}", systemPrompt);
        log.info("用户消息: {}", userMessage);
        
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
        
        log.info("AI 回复: {}", response);
        return response;
    }

    /**
     * 智能问答
     * @param question 问题
     * @return 答案
     */
    public String askQuestion(String question) {
        String systemPrompt = "你是一个专业的AI助手,请简洁明了地回答问题。";
        return chatWithSystemPrompt(systemPrompt, question);
    }
}
