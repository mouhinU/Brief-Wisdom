package com.mouhin.brief.wisdom.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.ai.prompt.SystemPrompts;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.common.ai.SyncStatusDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.exception.ContentSecurityException;
import com.mouhin.brief.wisdom.exception.RateLimitException;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.AiModelRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatMessageRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatSessionRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
/**
 * AiAgentService
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAgentService {

    // 默认用户ID（用于未登录场景）
    private static final String DEFAULT_USER_ID = "default-user";
    // 单条消息最大长度
    private static final int MAX_MESSAGE_LENGTH = 10000;
    // 默认模型名称
    private static final String DEFAULT_MODEL_NAME = "qwen-plus";
    // 默认提供商标识
    private static final String DEFAULT_PROVIDER = "dashscope";
    // 默认思考模式
    private static final String DEFAULT_THINKING_MODE = "normal";
    // 消息角色常量
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    // 消息类型常量
    private static final String MESSAGE_TYPE_TEXT = "text";
    // 上下文相关参数
    private static final int RECENT_MESSAGES_COUNT = 10;
    private static final int RELATED_SESSIONS_COUNT = 3;
    private static final int RELATED_MESSAGES_COUNT = 5;
    // 标题截断长度
    private static final int TITLE_MAX_LENGTH = 30;
    // 审计日志内容截断长度
    private static final int AUDIT_CONTENT_MAX_LENGTH = 50;
    // 占位符
    private static final String NA_PLACEHOLDER = "N/A";
    private final ChatModelRegistry chatModelRegistry;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatUserRepository userRepository;
    private final AiModelRepository aiModelRepository;
    private final ChatSyncService chatSyncService;
    private final ContentFilterService contentFilterService;
    private final RateLimitService rateLimitService;
    private final KnowledgeRagService knowledgeRagService;
    private final ChatMemoryService chatMemoryService;

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
        return createSession(userId, null);
    }

    /**
     * 为指定用户创建新会话（带页面上下文）
     *
     * @param userId      用户ID
     * @param pageContext 页面上下文（如 /about.html）
     * @return 会话ID
     */
    @Transactional
    public String createSession(String userId, String pageContext) {
        // 确保用户存在（防止外键约束失败）
        ensureUserExists(userId);

        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle("新会话");
        session.setPageContext(pageContext);
        session.setMessageCount(0);

        sessionRepository.save(session);
        log.info("为用户 {} 创建新会话: {}, pageContext: {}", userId, session.getSessionId(), pageContext);

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
     * 重命名会话标题
     *
     * @param sessionId 会话ID
     * @param newTitle  新标题
     */
    public void renameSession(String sessionId, String newTitle) {
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new AIException("会话不存在: " + sessionId);
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new AIException("标题不能为空");
        }
        session.setTitle(newTitle.trim());
        sessionRepository.update(session);
        log.info("重命名会话: {} -> {}", sessionId, newTitle);
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

        // 批量获取所有会话的最后消息时间（避免 N+1 查询）
        Map<String, LocalDateTime> lastMessageTimeMap = buildLastMessageTimeMap(userId);

        return sessions.stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setPageContext(session.getPageContext());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());

            // 使用批量查询的结果
            LocalDateTime lastTime = lastMessageTimeMap.get(session.getSessionId());
            meta.setUpdateTime(lastTime != null ? lastTime : session.getUpdateTime());

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

        // 批量获取所有会话的最后消息时间（避免 N+1 查询）
        Map<String, LocalDateTime> lastMessageTimeMap = buildLastMessageTimeMap(userId);

        // 转换为 SessionMeta
        List<SessionMetaDTO> sessionMetas = pageResult.getRecords().stream().map(session -> {
            SessionMetaDTO meta = new SessionMetaDTO();
            meta.setSessionId(session.getSessionId());
            meta.setUserId(session.getUserId());
            meta.setTitle(session.getTitle());
            meta.setDescription(session.getDescription());
            meta.setPageContext(session.getPageContext());
            meta.setMessageCount(session.getMessageCount());
            meta.setCreateTime(session.getCreateTime());

            // 使用批量查询的结果
            LocalDateTime lastTime = lastMessageTimeMap.get(session.getSessionId());
            meta.setUpdateTime(lastTime != null ? lastTime : session.getUpdateTime());

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
     * 批量构建用户所有会话的最后消息时间 Map
     * <p>
     * 一次 SQL 查询获取所有会话的最后消息时间，避免 N+1 查询问题。
     *
     * @param userId 用户ID
     * @return sessionId -> lastMessageTime 的映射
     */
    private Map<String, LocalDateTime> buildLastMessageTimeMap(String userId) {
        // 预估初始容量，避免频繁扩容
        Map<String, LocalDateTime> map = new HashMap<>(16);
        List<Map<String, Object>> timeRows = messageRepository.findLastMessageTimesByUserId(userId);
        for (Map<String, Object> row : timeRows) {
            String sid = String.valueOf(row.get("session_id"));
            Object lastTime = row.get("last_time");
            if (lastTime instanceof LocalDateTime) {
                map.put(sid, (LocalDateTime) lastTime);
            }
        }
        return map;
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
     * 分页获取会话历史消息（正序返回）
     * <p>
     * 返回的 records 按时间正序排列（从早到晚），方便前端直接渲染。
     *
     * @param sessionId 会话ID
     * @param page      页码，从1开始
     * @param size      每页条数
     * @return 分页结果
     */
    public PageResult<ChatMessageDTO> getSessionHistoryPaged(String sessionId, int page, int size) {
        // 使用数据库级别分页，避免全量加载到内存
        Page<ChatMessage> pageResult = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId, page, size);

        // 转换为 DTO（保持正序）
        List<ChatMessageDTO> dtos = pageResult.getRecords().stream()
                .map(this::toChatMessageDTO)
                .toList();

        // 封装分页结果
        PageResult<ChatMessageDTO> result = new PageResult<>();
        result.setRecords(dtos);
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
            return model != null ? model.getModelName() : DEFAULT_MODEL_NAME;
        } catch (Exception e) {
            log.warn("获取激活模型失败，使用默认模型: ", e);
            return DEFAULT_MODEL_NAME;
        }
    }

    /**
     * 根据模型名称查询对应的提供商
     *
     * @param modelName 模型名称
     * @return 提供商标识（dashscope / openai / anthropic / deepseek 等）
     */
    private String resolveProvider(String modelName) {
        AiModel aiModel = aiModelRepository.findByModelName(modelName);
        return (aiModel != null && aiModel.getProvider() != null)
                ? aiModel.getProvider() : DEFAULT_PROVIDER;
    }

    /**
     * 根据模型名称查询对应的思考模式
     *
     * @param modelName 模型名称
     * @return 思考模式: normal-普通模式, thinking-思考模式
     */
    private String resolveThinkingMode(String modelName) {
        AiModel aiModel = aiModelRepository.findByModelName(modelName);
        return (aiModel != null && aiModel.getThinkingMode() != null)
                ? aiModel.getThinkingMode() : DEFAULT_THINKING_MODE;
    }

    /**
     * 根据提供商构建对应的 ChatOptions
     *
     * @param provider  提供商标识
     * @param modelName 模型名称
     * @return 提供商匹配的 ChatOptions
     */
    private ChatOptions buildChatOptions(String provider, String modelName) {
        if ("anthropic".equals(provider)) {
            return AnthropicChatOptions.builder()
                    .model(modelName)
                    .build();
        }
        // OpenAI 兼容协议（dashscope / openai / deepseek）
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
        // 输入预过滤（无 sessionId/userId，使用占位符）
        checkInputSafety(message, NA_PLACEHOLDER, NA_PLACEHOLDER);

        log.info("收到用户消息: {}, 模型: {}", message, modelName);
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();
        String provider = resolveProvider(model);
        String thinkingMode = resolveThinkingMode(model);

        // 使用带思考模式的 ChatModel
        ChatModel chatModel = chatModelRegistry.getChatModel(provider, model, thinkingMode);
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(SystemPrompts.BASE_SYSTEM_PROMPT), new UserMessage(message)),
                buildChatOptions(provider, model)
        );
        long startTime = System.currentTimeMillis();
        ChatResponse chatResponse = chatModel.call(prompt);
        long elapsed = System.currentTimeMillis() - startTime;

        // NPE 防护：检查响应链
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            throw new AIException("AI 模型返回为空，请稍后重试");
        }
        String response = chatResponse.getResult().getOutput().getText();
        if (response == null) {
            response = "";
        }

        // 记录 Token 消耗和耗时
        Usage usage = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getUsage() : null;
        if (usage != null) {
            log.info("[AI调用] 模型: {}, 提供商: {}, 耗时: {}ms, 输入Token: {}, 输出Token: {}, 总Token: {}",
                    model, provider, elapsed,
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        } else {
            log.info("[AI调用] 模型: {}, 提供商: {}, 耗时: {}ms", model, provider, elapsed);
        }

        // 输出过滤（无 sessionId/userId/messageId，使用占位符）
        response = contentFilterService.filterOutput(response, NA_PLACEHOLDER, NA_PLACEHOLDER, null);
        log.info("AI 回复(模型: {}, 提供商: {}, 思考模式: {}): {}", model, provider, thinkingMode, response);
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
        return chatWithSession(sessionId, DEFAULT_USER_ID, message, null, null);
    }

    /**
     * 带上下文的聊天对话（指定用户和模型）
     */
    @Transactional
    public String chatWithSession(String sessionId, String userId, String message, String modelName) {
        return chatWithSession(sessionId, userId, message, modelName, null);
    }

    /**
     * 带上下文的聊天对话（指定用户、模型和当前页面上下文）
     * <p>
     * 会根据当前页面上下文查找同页面的其他会话，获取最近消息作为额外上下文，
     * 使 AI 能够更好地理解用户在该页面的历史交互。
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param message     用户消息
     * @param modelName   模型名称（可选）
     * @param pageContext 当前页面上下文（可选，如 /about.html）
     * @return AI 回复
     */
    @Transactional
    public String chatWithSession(String sessionId, String userId, String message, String modelName, String pageContext) {
        log.info("========== 开始处理聊天请求 ==========");
        log.info("sessionId: {}, userId: {}, model: {}, pageContext: {}", sessionId, userId, modelName, pageContext);

        // 输入校验
        validateInput(message);
        // 输入预过滤
        checkInputSafety(message, sessionId, userId);

        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();

        // 验证会话是否存在且属于该用户
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        log.info("查询到的 session: {}", session);

        if (session == null) {
            log.error("会话不存在: {}", sessionId);
            throw new AIException("会话不存在: " + sessionId);
        }

        if (!session.getUserId().equals(userId)) {
            log.error("无权访问此会话，session.userId: {}, userId: {}", session.getUserId(), userId);
            throw new AIException("无权访问此会话");
        }

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole(ROLE_USER);
        userMsg.setContent(message);
        userMsg.setMessageType(MESSAGE_TYPE_TEXT);
        messageRepository.save(userMsg);

        // 获取当前会话最近消息作为上下文
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, RECENT_MESSAGES_COUNT);
        Collections.reverse(recentMessages);  // 反转为正序

        // 构建当前会话上下文
        StringBuilder context = new StringBuilder();
        context.append("## 当前会话的最近消息\n");
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // 根据当前页面上下文，获取同页面其他会话的最近消息作为额外上下文
        String effectivePageContext = (pageContext != null && !pageContext.isBlank()) ? pageContext : session.getPageContext();
        if (effectivePageContext != null && !effectivePageContext.isBlank()) {
            List<ChatSession> relatedSessions = sessionRepository.findRecentByUserIdAndPageContext(userId, effectivePageContext, RELATED_SESSIONS_COUNT);
            // 过滤掉当前会话
            relatedSessions = relatedSessions.stream()
                    .filter(s -> !s.getSessionId().equals(sessionId))
                    .toList();

            if (!relatedSessions.isEmpty()) {
                context.append("\n## 同页面（").append(getPageContextName(effectivePageContext)).append("）的其他最近会话\n");
                for (ChatSession relatedSession : relatedSessions) {
                    List<ChatMessage> relatedMessages = messageRepository.findRecentMessages(relatedSession.getSessionId(), RELATED_MESSAGES_COUNT);
                    Collections.reverse(relatedMessages);
                    if (!relatedMessages.isEmpty()) {
                        context.append("\n### 会话: ").append(relatedSession.getTitle()).append("\n");
                        for (ChatMessage msg : relatedMessages) {
                            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                        }
                    }
                }
            }
        }

        // 根据会话的页面上下文构建增强的系统提示词
        String systemPrompt = SystemPrompts.getSystemPromptWithContext(effectivePageContext);

        // 注入知识库 RAG 上下文（基于用户消息检索相关文档）
        try {
            var relevantDocs = knowledgeRagService.retrieveRelevantDocuments(message);
            String ragContext = knowledgeRagService.buildContextFromDocuments(relevantDocs);
            if (!ragContext.isBlank()) {
                systemPrompt += ragContext;
            }
        } catch (Exception e) {
            log.warn("知识库 RAG 检索失败，跳过上下文注入: {}", e.getMessage());
        }

        // 注入用户记忆上下文（跨会话记忆）
        try {
            String memoryContext = chatMemoryService.buildMemoryContext(userId);
            if (!memoryContext.isBlank()) {
                systemPrompt += memoryContext;
            }
        } catch (Exception e) {
            log.warn("用户记忆加载失败，跳过记忆注入: {}", e.getMessage());
        }

        // 从用户消息中自动提取记忆（异步，不影响主流程）
        try {
            chatMemoryService.extractMemoriesFromMessage(userId, message, sessionId);
        } catch (Exception e) {
            log.warn("记忆提取失败: {}", e.getMessage());
        }

        // 根据模型查询提供商和思考模式，路由到对应的 ChatModel
        String provider = resolveProvider(model);
        String thinkingMode = resolveThinkingMode(model);
        ChatModel chatModel = chatModelRegistry.getChatModel(provider, model, thinkingMode);
        if (chatModel == null) {
            log.error("无法获取 ChatModel: provider={}, model={}, thinkingMode={}", provider, model, thinkingMode);
            throw new AIException("AI 模型不可用，请稍后重试");
        }

        // 调用 AI，获取完整响应（包含 token 用量）
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(context.toString())),
                buildChatOptions(provider, model)
        );
        long startTime = System.currentTimeMillis();
        ChatResponse chatResponse = chatModel.call(prompt);
        long elapsed = System.currentTimeMillis() - startTime;

        // 提取回复内容
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            throw new AIException("AI 模型返回为空，请稍后重试");
        }
        String response = chatResponse.getResult().getOutput().getText();
        if (response == null) {
            response = "";
        }

        // 输出过滤（在保存消息之前，以便记录 messageId）
        response = contentFilterService.filterOutput(response, sessionId, userId, null);

        // 提取 token 用量
        int promptTokens = 0;
        int completionTokens = 0;
        Integer totalTokens = 0;
        Usage usage = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getUsage() : null;
        if (usage != null) {
            promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
            totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
        }
        log.info("Token 用量 - 输入: {}, 输出: {}, 总计: {}, 耗时: {}ms", promptTokens, completionTokens, totalTokens, elapsed);

        // 计算费用
        Double cost = calculateCost(model, promptTokens, completionTokens);

        // 保存 AI 回复（含 token 和费用信息）
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole(ROLE_ASSISTANT);
        aiMsg.setContent(response);
        aiMsg.setModel(model);
        aiMsg.setTokens(totalTokens);
        aiMsg.setCost(cost);
        aiMsg.setMessageType(MESSAGE_TYPE_TEXT);
        messageRepository.save(aiMsg);

        // 如果之前输出过滤时记录了审计日志，需要更新 messageId
        // （由于 filterOutput 在 save 之前调用，messageId 为 null，这里可以补充记录）

        // 更新会话统计信息
        long messageCount = messageRepository.countBySessionId(sessionId);
        session.setMessageCount((int) messageCount);

        // 如果是第一条消息，用用户消息作为标题
        if (messageCount == 2) {
            session.setTitle(message.length() > TITLE_MAX_LENGTH ? message.substring(0, TITLE_MAX_LENGTH) + "..." : message);
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
     * 流式聊天（返回 Flux<String>，每个元素是一段文本片段）
     * <p>
     * 注意：流式模式下不保存 token 和费用信息，仅在最终完成后由调用方统一保存
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param message     用户消息
     * @param modelName   模型名称（可选）
     * @param pageContext 当前页面上下文（可选）
     * @return 文本片段流
     */
    public Flux<String> chatStreamWithSession(String sessionId, String userId, String message, String modelName, String pageContext) {
        log.info("========== 开始流式聊天请求 ==========");
        log.info("sessionId: {}, userId: {}, model: {}, pageContext: {}", sessionId, userId, modelName, pageContext);

        // 输入校验
        validateInput(message);
        checkInputSafety(message, sessionId, userId);

        String model = (modelName != null && !modelName.isEmpty()) ? modelName : getActiveModelName();

        // 验证会话
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            return Flux.error(new AIException("会话不存在: " + sessionId));
        }
        if (!session.getUserId().equals(userId)) {
            return Flux.error(new AIException("无权访问此会话"));
        }

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole(ROLE_USER);
        userMsg.setContent(message);
        userMsg.setMessageType(MESSAGE_TYPE_TEXT);
        messageRepository.save(userMsg);

        // 构建上下文（与非流式相同逻辑）
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, RECENT_MESSAGES_COUNT);
        Collections.reverse(recentMessages);

        StringBuilder context = new StringBuilder();
        context.append("## 当前会话的最近消息\n");
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        String effectivePageContext = (pageContext != null && !pageContext.isBlank()) ? pageContext : session.getPageContext();
        if (effectivePageContext != null && !effectivePageContext.isBlank()) {
            List<ChatSession> relatedSessions = sessionRepository.findRecentByUserIdAndPageContext(userId, effectivePageContext, RELATED_SESSIONS_COUNT);
            relatedSessions = relatedSessions.stream()
                    .filter(s -> !s.getSessionId().equals(sessionId))
                    .toList();

            if (!relatedSessions.isEmpty()) {
                context.append("\n## 同页面（").append(getPageContextName(effectivePageContext)).append("）的其他最近会话\n");
                for (ChatSession relatedSession : relatedSessions) {
                    List<ChatMessage> relatedMessages = messageRepository.findRecentMessages(relatedSession.getSessionId(), RELATED_MESSAGES_COUNT);
                    Collections.reverse(relatedMessages);
                    if (!relatedMessages.isEmpty()) {
                        context.append("\n### 会话: ").append(relatedSession.getTitle()).append("\n");
                        for (ChatMessage msg : relatedMessages) {
                            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                        }
                    }
                }
            }
        }

        String systemPrompt = SystemPrompts.getSystemPromptWithContext(effectivePageContext);
        String provider = resolveProvider(model);
        String thinkingMode = resolveThinkingMode(model);
        ChatModel chatModel = chatModelRegistry.getChatModel(provider, model, thinkingMode);

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(context.toString())),
                buildChatOptions(provider, model)
        );

        // 使用 stream() 获取流式响应
        return chatModel.stream(prompt)
                .map(chatResponse -> {
                    // 防御性编程：流式响应中某些 chunk 可能没有 result
                    var generation = chatResponse.getResult();
                    if (generation == null) {
                        log.debug("[流式] 收到空 result 的 chunk，跳过");
                        return "";
                    }
                    var output = generation.getOutput();
                    if (output == null) {
                        log.debug("[流式] 收到空 output 的 chunk，跳过");
                        return "";
                    }
                    String text = output.getText();
                    return text != null ? text : "";
                })
                .filter(text -> !text.isEmpty()) // 过滤掉空字符串
                .doOnComplete(() -> {
                    log.info("[流式] 完成");
                    // 流式完成后，通知其他设备
                    chatSyncService.notifyUser(userId, "message_added", sessionId);
                })
                .doOnError(error -> {
                    log.error("[流式] 错误: {}", error.getMessage());
                });
    }

    /**
     * 获取页面上下文的中文名称
     */
    private String getPageContextName(String pageContext) {
        if (pageContext == null) return "未知页面";
        return switch (pageContext) {
            case "/" -> "首页";
            case "/about.html" -> "个人简历";
            case "/resume-manage.html" -> "简历维护";
            case "/system-settings.html" -> "系统设置";
            case "/ai-manage.html" -> "AI管理";
            default -> pageContext;
        };
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

        ChatModel chatModel = chatModelRegistry.getDefaultChatModel();
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage))
        );
        long startTime = System.currentTimeMillis();
        ChatResponse chatResponse = chatModel.call(prompt);
        long elapsed = System.currentTimeMillis() - startTime;

        // NPE 防护：检查响应链
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            throw new AIException("AI 模型返回为空，请稍后重试");
        }
        String response = chatResponse.getResult().getOutput().getText();
        if (response == null) {
            response = "";
        }
        // 记录 Token 消耗和耗时
        Usage usage = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getUsage() : null;
        if (usage != null) {
            log.info("[AI调用] chatWithSystemPrompt, 耗时: {}ms, 输入Token: {}, 输出Token: {}, 总Token: {}",
                    elapsed, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        } else {
            log.info("[AI调用] chatWithSystemPrompt, 耗时: {}ms", elapsed);
        }
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
        // 输入预过滤（无 sessionId/userId，使用占位符）
        checkInputSafety(question, NA_PLACEHOLDER, NA_PLACEHOLDER);
        String systemPrompt = SystemPrompts.BASE_SYSTEM_PROMPT + "\n\n请简洁明了地回答问题。";
        String response = chatWithSystemPrompt(systemPrompt, question);
        // 输出过滤（无 sessionId/userId/messageId，使用占位符）
        return contentFilterService.filterOutput(response, NA_PLACEHOLDER, NA_PLACEHOLDER, null);
    }

    /**
     * 输入校验（基础格式校验）
     *
     * @param message 用户输入
     */
    private void validateInput(String message) {
        if (message == null || message.isBlank()) {
            throw new AIException("消息内容不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new AIException("消息长度不能超过" + MAX_MESSAGE_LENGTH + "个字符");
        }
    }

    /**
     * 输入安全预过滤（关键词拦截，命中即拒绝，不消耗模型 token）
     * <p>
     * 拦截时会按顺序执行：
     * 1. 保存用户消息到聊天记录
     * 2. 保存系统拦截回复到聊天记录
     * 3. 记录审计日志
     *
     * @param message   用户输入
     * @param sessionId 会话ID（用于审计日志）
     * @param userId    用户ID（用于审计日志）
     */
    public void checkInputSafety(String message, String sessionId, String userId) {
        // 仅检测，不记录日志
        String blockedKeyword = contentFilterService.checkInputBlocked(message);
        if (blockedKeyword != null) {
            // 被拦截，按正确顺序保存
            saveBlockedMessages(sessionId, userId, message, blockedKeyword);
            throw new ContentSecurityException(contentFilterService.getBlockedMessage());
        }
    }

    /**
     * 保存被拦截的消息到聊天记录（按正确顺序）
     * <p>
     * 执行顺序：
     * 1. 保存用户消息
     * 2. 保存系统拦截回复
     * 3. 更新会话统计
     * 4. 记录审计日志
     *
     * @param sessionId     会话ID
     * @param userId        用户ID
     * @param message       用户消息内容
     * @param blockedKeyword 命中的敏感词
     */
    @Transactional
    public void saveBlockedMessages(String sessionId, String userId, String message, String blockedKeyword) {
        log.info("[内容安全] 开始保存被拦截的消息 - sessionId: {}, userId: {}, keyword: {}", sessionId, userId, blockedKeyword);

        try {
            // 第一步：保存用户消息（显式设置 timestamp）
            LocalDateTime userMsgTime = LocalDateTime.now();
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setUserId(userId);
            userMsg.setRole(ROLE_USER);
            userMsg.setContent(message);
            userMsg.setMessageType(MESSAGE_TYPE_TEXT);
            userMsg.setTimestamp(userMsgTime); // 显式设置时间戳
            userMsg.setTokens(0);
            userMsg.setCost(0.0);
            messageRepository.save(userMsg);
            log.debug("[内容安全] 用户消息已保存 - timestamp: {}", userMsgTime);

            // 第二步：保存系统拦截回复（时间戳稍晚于用户消息）
            LocalDateTime systemMsgTime = userMsgTime.plusNanos(1_000_000); // +1毫秒
            ChatMessage systemMsg = new ChatMessage();
            systemMsg.setSessionId(sessionId);
            systemMsg.setUserId(userId);
            systemMsg.setRole(ROLE_ASSISTANT);
            systemMsg.setContent(contentFilterService.getBlockedMessage());
            systemMsg.setModel("system-security-filter"); // 标记为系统安全过滤
            systemMsg.setMessageType(MESSAGE_TYPE_TEXT);
            systemMsg.setTimestamp(systemMsgTime); // 显式设置时间戳
            systemMsg.setTokens(0);
            systemMsg.setCost(0.0);
            messageRepository.save(systemMsg);
            log.debug("[内容安全] 系统回复已保存 - timestamp: {}", systemMsgTime);

            // 第三步：更新会话统计信息
            long messageCount = messageRepository.countBySessionId(sessionId);
            ChatSession session = sessionRepository.findBySessionId(sessionId);
            if (session != null) {
                session.setMessageCount((int) messageCount);
                // 如果是第一条消息，用用户消息作为标题
                if (messageCount == 2) {
                    session.setTitle(message.length() > TITLE_MAX_LENGTH ? message.substring(0, TITLE_MAX_LENGTH) + "..." : message);
                }
                session.setUpdateTime(LocalDateTime.now());
                sessionRepository.update(session);
                log.debug("[内容安全] 会话统计已更新");
            }

            // 第四步：记录审计日志（在聊天记录之后）
            contentFilterService.logInputBlocked(
                    sessionId, 
                    userId, 
                    blockedKeyword, 
                    maskContentForAudit(message)
            );
            log.info("[内容安全] 审计日志已记录 - sessionId: {}, messageCount: {}", sessionId, messageCount);

        } catch (Exception e) {
            // 即使保存失败，也不影响拦截逻辑
            log.error("[内容安全] 保存被拦截消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 对内容进行脱敏处理（用于审计日志）
     *
     * @param content 原始内容
     * @return 脱敏后的内容
     */
    private String maskContentForAudit(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        // 简单脱敏：只显示前N个字符
        if (content.length() > AUDIT_CONTENT_MAX_LENGTH) {
            return content.substring(0, AUDIT_CONTENT_MAX_LENGTH) + "...[已脱敏]";
        }
        return content;
    }

    /**
     * 限流检查（AI 聊天不调用此方法，其他功能可按需调用）
     *
     * @param userId 用户ID
     */
    private void checkRateLimit(String userId) {
        if (rateLimitService.isRateLimited(userId)) {
            throw new RateLimitException(rateLimitService.getRateLimitMessage());
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
        Map<String, Integer> messageCounts = new HashMap<>(16);
        List<Map<String, Object>> countRows = messageRepository.findMessageCountsByUserId(userId);
        for (Map<String, Object> row : countRows) {
            String sid = String.valueOf(row.get("session_id"));
            int cnt = ((Number) row.get("cnt")).intValue();
            messageCounts.put(sid, cnt);
        }
        syncStatus.setSessionMessageCounts(messageCounts);

        // 3. 每个会话的最后消息时间（毫秒时间戳）
        Map<String, Long> lastMessageTimes = new HashMap<>(16);
        List<Map<String, Object>> timeRows = messageRepository.findLastMessageTimesByUserId(userId);
        for (Map<String, Object> row : timeRows) {
            String sid = String.valueOf(row.get("session_id"));
            Object lastTime = row.get("last_time");
            if (lastTime instanceof LocalDateTime) {
                lastMessageTimes.put(sid, ((LocalDateTime) lastTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
        }
        syncStatus.setSessionLastMessageTimes(lastMessageTimes);

        // 4. 计算指纹（基于以上数据的哈希值）
        String raw = sessionCount + ":" + messageCounts.toString() + ":" + lastMessageTimes.toString();
        syncStatus.setFingerprint(Integer.toHexString(raw.hashCode()));

        return syncStatus;
    }

    /**
     * 保存流式输出的 AI 消息到数据库
     * <p>
     * 前端在接收到完整的流式响应后调用此接口，将 AI 回复保存到数据库。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param content   AI 回复内容
     * @param model     模型名称
     */
    @Transactional
    public void saveStreamedMessage(String sessionId, String userId, String content, String model) {
        log.info("保存流式消息 - sessionId: {}, userId: {}, model: {}, content length: {}",
                sessionId, userId, model, content != null ? content.length() : 0);

        // 验证会话
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new AIException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            throw new AIException("无权访问此会话");
        }

        // 保存 AI 回复
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole(ROLE_ASSISTANT);
        aiMsg.setContent(content);
        aiMsg.setModel(model);
        aiMsg.setMessageType(MESSAGE_TYPE_TEXT);
        // 流式模式下不记录 token 和费用
        aiMsg.setTokens(0);
        aiMsg.setCost(0.0);
        messageRepository.save(aiMsg);

        // 更新会话统计信息
        long messageCount = messageRepository.countBySessionId(sessionId);
        session.setMessageCount((int) messageCount);

        // 如果是第一条消息，用用户消息作为标题
        if (messageCount == 2) {
            // 获取用户的第一条消息作为标题
            List<ChatMessage> firstMessages = messageRepository.findRecentMessages(sessionId, 1);
            if (!firstMessages.isEmpty()) {
                String firstMessage = firstMessages.get(0).getContent();
                session.setTitle(firstMessage.length() > TITLE_MAX_LENGTH ? firstMessage.substring(0, TITLE_MAX_LENGTH) + "..." : firstMessage);
            }
        }

        // 手动设置更新时间为当前时间
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.update(session);

        log.info("流式消息保存成功 - sessionId: {}, messageCount: {}", sessionId, messageCount);

        // SSE 通知其他设备：新消息已添加
        chatSyncService.notifyUser(userId, "message_added", sessionId);
    }
}
