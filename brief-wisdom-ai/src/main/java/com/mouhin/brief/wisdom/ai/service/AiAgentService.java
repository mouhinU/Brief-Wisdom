package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.prompt.SystemPrompts;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.common.ai.SyncStatusDTO;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.AiModelRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatMessageRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatSessionRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
public class AiAgentService {

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatUserRepository userRepository;
    private final AiModelRepository aiModelRepository;
    private final ChatSyncService chatSyncService;
    private final ContentFilterService contentFilterService;
    private final RateLimitService rateLimitService;

    // 默认用户ID（用于未登录场景）
    private static final String DEFAULT_USER_ID = "default-user";

    // 单条消息最大长度
    private static final int MAX_MESSAGE_LENGTH = 10000;

    public AiAgentService(ChatClient chatClient,
                          ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository,
                          ChatUserRepository userRepository,
                          AiModelRepository aiModelRepository,
                          ChatSyncService chatSyncService,
                          ContentFilterService contentFilterService,
                          RateLimitService rateLimitService) {
        this.chatClient = chatClient;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiModelRepository = aiModelRepository;
        this.chatSyncService = chatSyncService;
        this.contentFilterService = contentFilterService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * 创建新会话（无参版本，使用默认用户ID）
     *
     * @return 会话ID
     */
    @Transactional
    public String createSession() {
        return createSession(DEFAULT_USER_ID);
    }

    /**
     * 为指定用户创建新会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    @Transactional
    public String createSession(String userId) {
        // 确保用户存在（防止外键约束失败）
        ensureUserExists(userId);

        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle("新会话");
        session.setMessageCount(0);

        sessionRepository.save(session);
        log.info("为用户 {} 创建新会话: {}", userId, session.getSessionId());

        // SSE 通知其他设备：新会话已创建
        chatSyncService.notifyUser(userId, "session_created", session.getSessionId());

        return session.getSessionId();
    }

    /**
     * 确保用户存在于数据库中（防止外键约束失败）
     * 如果用户不存在，则创建新用户。
     * <p>
     * 对于已逻辑删除的 guest 用户，不自动恢复，而是硬删除旧记录（清除残留数据）
     * 后创建全新的用户，保证干净的初始状态。
     */
    public void ensureUserExists(String userId) {
        // 1. 先用 @TableLogic 感知的查询找未删除的用户
        ChatUser existingUser = userRepository.findByUserId(userId);
        if (existingUser != null) {
            return; // 用户存在且未删除，直接返回
        }

        // 2. 查找是否存在已逻辑删除的记录（绕过 @TableLogic）
        ChatUser deletedUser = userRepository.findByUserIdIncludeDeleted(userId);
        if (deletedUser != null) {
            // 已删除的 guest 用户不自动恢复，硬删除旧记录（级联清除残留的 session/message 等）
            userRepository.hardDeleteByUserId(userId);
            log.info("硬删除已删除的 guest 用户旧记录: {}", userId);
            // 继续往下走，创建全新用户
        }

        // 3. 创建新用户
        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setUsername(userId);  // userId 本身已含 guest- 前缀
        user.setNickname(userId.startsWith("guest-") ? "访客" : userId);
        try {
            userRepository.save(user);
            log.info("动态创建用户: {}", userId);
        } catch (Exception e) {
            // 唯一键冲突时忽略（并发场景）
            log.warn("创建用户失败（可能已存在）: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 删除会话（逻辑删除）
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
        log.info("删除会话: {}", sessionId);

        // SSE 通知其他设备：会话已删除
        // 删除时无法确定 userId，广播给所有已连接用户
        chatSyncService.broadcastToAll("session_deleted", sessionId);
    }

    /**
     * 获取所有会话列表
     *
     * @return 会话元数据列表
     */
    public List<SessionMetaDTO> listSessions() {
        return listSessions(DEFAULT_USER_ID);
    }

    /**
     * 获取指定用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话元数据列表
     */
    public List<SessionMetaDTO> listSessions(String userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);
        return sessions.stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());

            // 使用最后一条消息的时间作为更新时间
            LocalDateTime lastMessageTime = messageRepository.findLastMessageTime(session.getSessionId());
            meta.setUpdateTime(lastMessageTime != null ? lastMessageTime : session.getUpdateTime());

            return meta;
        }).toList();
    }

    /**
     * 分页获取会话列表
     *
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public PageResult<SessionMetaDTO> listSessionsPaged(int page, int size) {
        return listSessionsPaged(DEFAULT_USER_ID, page, size);
    }

    /**
     * 分页获取指定用户的会话列表
     *
     * @param userId 用户ID
     * @param page   当前页码（从1开始）
     * @param size   每页大小
     * @return 分页结果
     */
    public PageResult<SessionMetaDTO> listSessionsPaged(String userId, int page, int size) {
        Page<ChatSession> pageResult = sessionRepository.findByUserIdOrderByUpdateTimeDesc(userId, page, size);

        // 转换为 SessionMeta
        List<SessionMetaDTO> sessionMetas = pageResult.getRecords().stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());

            LocalDateTime lastMessageTime = messageRepository.findLastMessageTime(session.getSessionId());
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
     * 获取会话历史消息（全量，用于向后兼容）
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ChatMessageDTO> getSessionHistory(String sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream().map(this::toChatMessageDTO).toList();
    }

    /**
     * 分页获取会话历史消息（倒序分页，第1页为最新消息）
     * <p>
     * 返回的 records 按时间正序排列（方便前端直接渲染），
     * 但分页逻辑按倒序切片：page=1 取最新的 size 条。
     *
     * @param sessionId 会话ID
     * @param page      页码，从1开始
     * @param size      每页条数
     * @return 分页结果
     */
    public PageResult<ChatMessageDTO> getSessionHistoryPaged(String sessionId, int page, int size) {
        Page<ChatMessage> pageResult = messageRepository.findBySessionIdOrderByTimestampDesc(sessionId, page, size);

        // 转换为 DTO
        List<ChatMessageDTO> dtos = pageResult.getRecords().stream()
                .map(this::toChatMessageDTO)
                .toList();

        // 反转为正序（方便前端直接按顺序渲染）
        List<ChatMessageDTO> reversedDtos = new ArrayList<>(dtos);
        Collections.reverse(reversedDtos);

        // 封装分页结果
        PageResult<ChatMessageDTO> result = new PageResult<>();
        result.setRecords(reversedDtos);
        result.setTotal(pageResult.getTotal());
        result.setPage(pageResult.getCurrent());
        result.setSize(pageResult.getSize());
        result.setPages(pageResult.getPages());
        result.setHasMore(pageResult.getCurrent() < pageResult.getPages());

        return result;
    }

    /**
     * ChatMessage 实体转 ChatMessageDTO
     */
    private ChatMessageDTO toChatMessageDTO(ChatMessage msg) {
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
    }

    /**
     * 根据模型名称和 token 用量计算费用（单位：元）
     */
    private Double calculateCost(String modelName, Integer promptTokens, Integer completionTokens) {
        try {
            AiModel aiModel = aiModelRepository.findByModelName(modelName);
            if (aiModel != null && aiModel.getInputPricePerMillion() != null && aiModel.getOutputPricePerMillion() != null) {
                double inputCost = promptTokens / 1_000_000.0 * aiModel.getInputPricePerMillion();
                double outputCost = completionTokens / 1_000_000.0 * aiModel.getOutputPricePerMillion();
                return Math.round((inputCost + outputCost) * 10000.0) / 10000.0;  // 保留4位小数
            }
        } catch (Exception e) {
            log.warn("计算费用失败，model: {}, error: {}", modelName, e.getMessage());
        }
        return 0.0;
    }

    /**
     * 获取当前激活的模型名称
     */
    public String getActiveModelName() {
        try {
            AiModel model = aiModelRepository.findActiveModel();
            return model != null ? model.getModelName() : "qwen-plus";
        } catch (Exception e) {
            log.warn("获取激活模型失败，使用默认模型: ", e);
            return "qwen-plus";
        }
    }

    /**
     * 构建带模型选项的 OpenAiChatOptions
     */
    private OpenAiChatOptions buildModelOptions(String modelName) {
        return OpenAiChatOptions.builder()
                .model(modelName)
                .build();
    }

    /**
     * 简单聊天对话（无上下文）
     *
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
        validateInput(message);
        // 输入预过滤
        checkInputSafety(message);

        log.info("收到用户消息: {}, 模型: {}", message, modelName);
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();

        String response = chatClient.prompt()
                .system(SystemPrompts.BASE_SYSTEM_PROMPT)
                .options(buildModelOptions(model))
                .user(message)
                .call()
                .content();

        // 输出过滤
        response = contentFilterService.filterOutput(response);
        log.info("AI 回复(模型: {}): {}", model, response);
        return response;
    }

    /**
     * 带上下文的聊天对话
     *
     * @param sessionId 会话ID
     * @param message   用户消息
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

        // 输入校验
        validateInput(message);
        // 限流检查
        checkRateLimit(userId);
        // 输入预过滤
        checkInputSafety(message);

        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();

        // 验证会话是否存在且属于该用户
        ChatSession session = sessionRepository.findBySessionId(sessionId);
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
        messageRepository.save(userMsg);

        // 获取最近10条消息作为上下文
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, 10);
        Collections.reverse(recentMessages);  // 反转为正序

        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // 调用 AI，获取完整响应（包含 token 用量）
        ChatResponse chatResponse = chatClient.prompt()
                .system(SystemPrompts.BASE_SYSTEM_PROMPT)
                .options(buildModelOptions(model))
                .user(context.toString())
                .call()
                .chatResponse();

        // 提取回复内容
        String response = chatResponse.getResult().getOutput().getText();

        // 输出过滤
        response = contentFilterService.filterOutput(response);

        // 提取 token 用量
        Integer promptTokens = 0;
        Integer completionTokens = 0;
        Integer totalTokens = 0;
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage != null) {
            promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
            totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
        }
        log.info("Token 用量 - 输入: {}, 输出: {}, 总计: {}", promptTokens, completionTokens, totalTokens);

        // 计算费用
        Double cost = calculateCost(model, promptTokens, completionTokens);

        // 保存 AI 回复（含 token 和费用信息）
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(response);
        aiMsg.setModel(model);
        aiMsg.setTokens(totalTokens);
        aiMsg.setCost(cost);
        aiMsg.setMessageType("text");
        messageRepository.save(aiMsg);

        // 更新会话统计信息
        long messageCount = messageRepository.countBySessionId(sessionId);
        session.setMessageCount((int) messageCount);

        // 如果是第一条消息，用用户消息作为标题
        if (messageCount == 2) {
            session.setTitle(message.length() > 30 ? message.substring(0, 30) + "..." : message);
        }

        // 手动设置更新时间为当前时间（即最后一条消息的时间）
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.update(session);

        log.info("AI 回复(模型: {}, tokens: {}, cost: {}): {}", model, totalTokens, cost, response);

        // SSE 通知其他设备：新消息已添加
        chatSyncService.notifyUser(userId, "message_added", sessionId);

        return response;
    }

    /**
     * 带系统提示的对话
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
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
     *
     * @param question 问题
     * @return 答案
     */
    public String askQuestion(String question) {
        validateInput(question);
        checkInputSafety(question);
        String systemPrompt = SystemPrompts.BASE_SYSTEM_PROMPT + "\n\n请简洁明了地回答问题。";
        String response = chatWithSystemPrompt(systemPrompt, question);
        return contentFilterService.filterOutput(response);
    }

    /**
     * 输入校验（基础格式校验）
     *
     * @param message 用户输入
     */
    private void validateInput(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息长度不能超过" + MAX_MESSAGE_LENGTH + "个字符");
        }
    }

    /**
     * 输入安全预过滤（关键词拦截，命中即拒绝，不消耗模型 token）
     *
     * @param message 用户输入
     */
    private void checkInputSafety(String message) {
        if (contentFilterService.isInputBlocked(message)) {
            throw new ContentSecurityException(contentFilterService.getBlockedMessage());
        }
    }

    /**
     * 限流检查
     *
     * @param userId 用户ID
     */
    private void checkRateLimit(String userId) {
        if (rateLimitService.isRateLimited(userId)) {
            throw new RateLimitException(rateLimitService.getRateLimitMessage());
        }
    }

    /**
     * 内容安全异常（输入违规时抛出）
     */
    public static class ContentSecurityException extends RuntimeException {
        public ContentSecurityException(String message) {
            super(message);
        }
    }

    /**
     * 限流异常（请求超频时抛出）
     */
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    /**
     * 获取当前用户的同步状态（轻量级，用于多端同步检测）
     *
     * @param userId 用户ID
     * @return 同步状态 DTO
     */
    public SyncStatusDTO getSyncStatus(String userId) {
        SyncStatusDTO syncStatus = new SyncStatusDTO();

        // 1. 会话总数
        long sessionCount = sessionRepository.countByUserId(userId);
        syncStatus.setSessionCount((int) sessionCount);

        // 2. 每个会话的消息数量
        Map<String, Integer> messageCounts = new HashMap<>();
        List<Map<String, Object>> countRows = messageRepository.findMessageCountsByUserId(userId);
        for (Map<String, Object> row : countRows) {
            String sid = String.valueOf(row.get("session_id"));
            int cnt = ((Number) row.get("cnt")).intValue();
            messageCounts.put(sid, cnt);
        }
        syncStatus.setSessionMessageCounts(messageCounts);

        // 3. 每个会话的最后消息时间（毫秒时间戳）
        Map<String, Long> lastMessageTimes = new HashMap<>();
        List<Map<String, Object>> timeRows = messageRepository.findLastMessageTimesByUserId(userId);
        for (Map<String, Object> row : timeRows) {
            String sid = String.valueOf(row.get("session_id"));
            Object lastTime = row.get("last_time");
            if (lastTime instanceof LocalDateTime) {
                lastMessageTimes.put(sid, ((LocalDateTime) lastTime).toInstant(ZoneOffset.UTC).toEpochMilli());
            }
        }
        syncStatus.setSessionLastMessageTimes(lastMessageTimes);

        // 4. 计算指纹（基于以上数据的哈希值）
        String raw = sessionCount + ":" + messageCounts.toString() + ":" + lastMessageTimes.toString();
        syncStatus.setFingerprint(Integer.toHexString(raw.hashCode()));

        return syncStatus;
    }
}
