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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
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
    // 标题截断长度
    private static final int TITLE_MAX_LENGTH = 30;
    // 审计日志内容截断长度
    private static final int AUDIT_CONTENT_MAX_LENGTH = 50;
    // 占位符
    private static final String NA_PLACEHOLDER = "N/A";
    // AI 调用重试次数（针对瞬时故障，如响应格式异常、网络抖动）
    private static final int AI_CALL_MAX_RETRIES = 2;
    // 重试基础等待时间（毫秒），实际等待 = base * attempt
    private static final long AI_CALL_RETRY_DELAY_MS = 500L;
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
    private final AiAuditService auditService;
    private final ToolCallbackProvider toolCallbackProvider;

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
        log.info("[会话] 删除会话, sessionId: {}", sessionId);
        sessionRepository.deleteBySessionId(sessionId);
        log.info("[会话] 会话删除完成: {}", sessionId);

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
        log.info("[会话] 重命名会话, sessionId: {}, newTitle: {}", sessionId, newTitle);
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new AIException("会话不存在: " + sessionId);
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new AIException("标题不能为空");
        }
        String oldTitle = session.getTitle();
        session.setTitle(newTitle.trim());
        sessionRepository.update(session);
        log.info("[会话] 重命名完成: '{}' -> '{}'", oldTitle, newTitle.trim());
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
        log.info("[会话] 查询用户会话列表, userId: {}", userId);
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdateTimeDesc(userId);

        // 批量获取所有会话的最后消息时间（避免 N+1 查询）
        Map<String, LocalDateTime> lastMessageTimeMap = buildLastMessageTimeMap(userId);

        return sessions.stream().map(session -> {
            // 使用批量查询的结果
            LocalDateTime lastTime = lastMessageTimeMap.get(session.getSessionId());
            return new SessionMetaDTO(
                    session.getSessionId(),
                    session.getUserId(),
                    session.getTitle(),
                    session.getDescription(),
                    session.getPageContext(),
                    session.getMessageCount(),
                    session.getCreateTime(),
                    lastTime != null ? lastTime : session.getUpdateTime()
            );
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
        log.info("[会话] 分页查询用户会话列表, userId: {}, page: {}, size: {}", userId, page, size);
        Page<ChatSession> pageResult = sessionRepository.findByUserIdOrderByUpdateTimeDesc(userId, page, size);

        // 批量获取所有会话的最后消息时间（避免 N+1 查询）
        Map<String, LocalDateTime> lastMessageTimeMap = buildLastMessageTimeMap(userId);

        // 转换为 SessionMeta
        List<SessionMetaDTO> sessionMetas = pageResult.getRecords().stream().map(session -> {
            // 使用批量查询的结果
            LocalDateTime lastTime = lastMessageTimeMap.get(session.getSessionId());
            return new SessionMetaDTO(
                    session.getSessionId(),
                    session.getUserId(),
                    session.getTitle(),
                    session.getDescription(),
                    session.getPageContext(),
                    session.getMessageCount(),
                    session.getCreateTime(),
                    lastTime != null ? lastTime : session.getUpdateTime()
            );
        }).toList();

        // 封装分页结果
        PageResult<SessionMetaDTO> result = new PageResult<>();
        result.setRecords(sessionMetas);
        result.setTotal(pageResult.getTotal());
        result.setPage(pageResult.getCurrent());
        result.setSize(pageResult.getSize());
        result.setPages(pageResult.getPages());
        result.setHasMore(pageResult.getCurrent() < pageResult.getPages());

        log.info("[会话] 分页查询完成, userId: {}, 总数: {}, 当前页: {}, 每页: {}",
                userId, result.getTotal(), result.getPage(), result.getSize());
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
            if (lastTime instanceof LocalDateTime ldt) {
                map.put(sid, ldt);
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
        log.info("[会话] 查询会话历史(全量), sessionId: {}", sessionId);
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        List<ChatMessageDTO> dtos = messages.stream().map(this::toChatMessageDTO).toList();
        log.info("[会话] 查询会话历史完成, sessionId: {}, 消息数: {}", sessionId, dtos.size());
        return dtos;
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
        log.info("[会话] 分页查询会话历史, sessionId: {}, page: {}, size: {}", sessionId, page, size);
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

        log.info("[会话] 分页查询历史完成, sessionId: {}, 总数: {}, 当前页: {}",
                sessionId, result.getTotal(), result.getPage());
        return result;
    }

    /**
     * ChatMessage 实体转 ChatMessageDTO
     */
    private ChatMessageDTO toChatMessageDTO(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getSessionId(),
                msg.getUserId(),
                msg.getRole(),
                msg.getContent(),
                msg.getModel(),
                msg.getTokens(),
                msg.getCost(),
                msg.getTimestamp(),
                msg.getMessageType()
        );
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
     * 根据内容长度估算流式响应的 Token 数量
     * <p>
     * 中英文混合文本按约 3 字符 ≈ 1 Token 估算。
     *
     * @param content 流式响应累积的文本内容
     * @return 估算的 Token 数量
     */
    public int estimateStreamingTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return Math.max(1, content.length() / 3);
    }

    /**
     * 估算流式响应的费用（仅计算输出 Token 费用）
     * <p>
     * 流式模式下无法精确获取输入 Token 数量，仅按输出 Token 估算费用。
     *
     * @param modelName        模型名称
     * @param completionTokens 估算的输出 Token 数量
     * @return 估算的费用（元）
     */
    public double estimateStreamingCost(String modelName, int completionTokens) {
        try {
            AiModel aiModel = aiModelRepository.findByModelName(modelName);
            if (aiModel != null && aiModel.getOutputPricePerMillion() != null) {
                double outputCost = completionTokens / 1_000_000.0 * aiModel.getOutputPricePerMillion();
                return Math.round(outputCost * 10000.0) / 10000.0;
            }
        } catch (Exception e) {
            log.warn("估算流式费用失败，model: {}, error: {}", modelName, e.getMessage());
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
     * 根据提供商构建对应的 ChatOptions（默认包含工具）
     *
     * @param provider  提供商标识
     * @param modelName 模型名称
     * @return 提供商匹配的 ChatOptions
     */
    private ChatOptions buildChatOptions(String provider, String modelName) {
        return buildChatOptions(provider, modelName, true);
    }

    /**
     * 根据提供商构建对应的 ChatOptions
     *
     * @param provider     提供商标识
     * @param modelName    模型名称
     * @param includeTools 是否包含工具回调（流式模式暂不支持工具调用，传 false）
     * @return 提供商匹配的 ChatOptions
     */
    private ChatOptions buildChatOptions(String provider, String modelName, boolean includeTools) {
        if ("anthropic".equals(provider)) {
            AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder().model(modelName);
            if (includeTools) {
                builder.toolCallbacks(toolCallbackProvider.getToolCallbacks());
            }
            return builder.build();
        }
        // OpenAI 兼容协议（dashscope / openai / deepseek）
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(modelName);
        if (includeTools) {
            builder.toolCallbacks(toolCallbackProvider.getToolCallbacks());
        }
        return builder.build();
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
        ChatResponse chatResponse = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= AI_CALL_MAX_RETRIES; attempt++) {
            try {
                chatResponse = chatModel.call(prompt);
                break;
            } catch (Exception e) {
                lastException = e;
                if (isNonRetryableError(e)) {
                    log.error("[AI调用] 不可重试错误: {}", e.getMessage());
                    throw new AIException(translateApiError(e));
                }
                if (attempt < AI_CALL_MAX_RETRIES) {
                    long delay = AI_CALL_RETRY_DELAY_MS * (attempt + 1);
                    log.warn("[AI调用] 第 {} 次调用失败，{}ms 后重试: {}", attempt + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AIException("AI 服务调用被中断");
                    }
                }
            }
        }

        if (chatResponse == null) {
            log.error("[AI调用] 重试 {} 次后仍失败: {}", AI_CALL_MAX_RETRIES,
                    lastException != null ? lastException.getMessage() : "unknown");
            throw new AIException(translateApiError(lastException));
        }

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

        // 如果是第一条消息，立即用用户消息作为标题
        long currentCount = messageRepository.countBySessionId(sessionId);
        if (currentCount == 1) {
            session.setTitle(message.length() > TITLE_MAX_LENGTH ? message.substring(0, TITLE_MAX_LENGTH) + "..." : message);
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.update(session);
            log.info("设置会话标题为用户首条消息: {}", session.getTitle());
        }

        // 获取当前会话最近消息作为上下文
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, RECENT_MESSAGES_COUNT);
        Collections.reverse(recentMessages);  // 反转为正序

        // 构建当前会话上下文
        StringBuilder context = new StringBuilder();
        context.append("## 当前会话的最近消息\n");
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // 计算有效页面上下文（仅用于定制系统提示词）
        // 注意：上下文只包含当前会话自身的历史消息，不注入其他会话的记录，确保会话间相互隔离
        String effectivePageContext = (pageContext != null && !pageContext.isBlank()) ? pageContext : session.getPageContext();

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

        // 注入用户记忆上下文（仅当前会话的记忆，确保会话间隔离）
        try {
            String memoryContext = chatMemoryService.buildMemoryContext(userId, sessionId);
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
        ChatResponse chatResponse = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= AI_CALL_MAX_RETRIES; attempt++) {
            try {
                chatResponse = chatModel.call(prompt);
                break;
            } catch (Exception e) {
                lastException = e;
                if (isNonRetryableError(e)) {
                    log.error("[AI调用] 不可重试错误: {}", e.getMessage());
                    throw new AIException(translateApiError(e));
                }
                if (attempt < AI_CALL_MAX_RETRIES) {
                    long delay = AI_CALL_RETRY_DELAY_MS * (attempt + 1);
                    log.warn("[AI调用] 第 {} 次调用失败，{}ms 后重试: {}", attempt + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AIException("AI 服务调用被中断");
                    }
                }
            }
        }

        if (chatResponse == null) {
            log.error("[AI调用] 重试 {} 次后仍失败: {}", AI_CALL_MAX_RETRIES,
                    lastException != null ? lastException.getMessage() : "unknown");
            throw new AIException(translateApiError(lastException));
        }

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

        // 如果是第一条消息，立即用用户消息作为标题
        long currentCount = messageRepository.countBySessionId(sessionId);
        if (currentCount == 1) {
            session.setTitle(message.length() > TITLE_MAX_LENGTH ? message.substring(0, TITLE_MAX_LENGTH) + "..." : message);
            session.setUpdateTime(LocalDateTime.now());
            sessionRepository.update(session);
            log.info("设置会话标题为用户首条消息（流式）: {}", session.getTitle());
        }

        // 构建上下文（与非流式相同逻辑）
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, RECENT_MESSAGES_COUNT);
        Collections.reverse(recentMessages);

        StringBuilder context = new StringBuilder();
        context.append("## 当前会话的最近消息\n");
        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // 计算有效页面上下文（仅用于定制系统提示词）
        // 注意：上下文只包含当前会话自身的历史消息，不注入其他会话的记录，确保会话间相互隔离
        String effectivePageContext = (pageContext != null && !pageContext.isBlank()) ? pageContext : session.getPageContext();

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

        // 注入用户记忆上下文（仅当前会话的记忆，确保会话间隔离）
        try {
            String memoryContext = chatMemoryService.buildMemoryContext(userId, sessionId);
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
            log.warn("记忆提取失败 {}", e.getMessage());
        }

        String provider = resolveProvider(model);
        String thinkingMode = resolveThinkingMode(model);
        ChatModel chatModel = chatModelRegistry.getChatModel(provider, model, thinkingMode);
        if (chatModel == null) {
            log.error("[AI] 无法获取 ChatModel: provider={}, model={}, thinkingMode={}", provider, model, thinkingMode);
            throw new AIException("AI 模型不可用，请稍后重试");
        }

        // 有工具时使用 call() 路径（支持工具调用循环），无工具时使用 stream() 路径（流式输出）
        boolean hasTools = toolCallbackProvider != null && toolCallbackProvider.getToolCallbacks().length > 0;

        if (hasTools) {
            // === 工具可用路径：手动实现工具调用循环（兼容 DashScope 等 OpenAI 兼容协议） ===
            log.info("[AI] 工具已注册，使用手动工具调用循环");
            ChatOptions chatOptions = buildChatOptions(provider, model, true);
            ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();

            // 构建可变的消息列表（用于多轮工具调用）
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(context.toString()));

            long startTime = System.currentTimeMillis();
            String response = "";
            int maxToolRounds = 5;
            int round = 0;

            try {
                while (round < maxToolRounds) {
                    round++;
                    Prompt prompt = new Prompt(messages, chatOptions);
                    ChatResponse chatResponse = chatModel.call(prompt);

                    if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
                        return Flux.error(new AIException("AI 模型返回为空，请稍后重试"));
                    }

                    AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

                    if (!assistantMessage.hasToolCalls()) {
                        // 模型没有调用工具，返回最终文本回复
                        response = assistantMessage.getText();
                        if (response == null) {
                            response = "";
                        }
                        log.info("[AI] 工具调用循环结束，共 {} 轮", round);
                        break;
                    }

                    // 模型调用了工具，执行工具并将结果加入消息列表
                    log.info("[AI] 第 {} 轮工具调用，工具数: {}", round, assistantMessage.getToolCalls().size());
                    messages.add(assistantMessage);

                    List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
                    for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                        String toolName = toolCall.name();
                        String toolArgs = toolCall.arguments();
                        log.info("[AI] 调用工具: {}, 参数: {}", toolName, toolArgs);

                        // 查找对应的 ToolCallback
                        ToolCallback matchedCallback = null;
                        for (ToolCallback callback : toolCallbacks) {
                            if (callback.getToolDefinition().name().equals(toolName)) {
                                matchedCallback = callback;
                                break;
                            }
                        }

                        String toolResult;
                        if (matchedCallback != null) {
                            try {
                                toolResult = matchedCallback.call(toolArgs);
                                log.info("[AI] 工具 {} 执行成功，结果长度: {}", toolName, toolResult.length());
                            } catch (Exception e) {
                                toolResult = "工具执行失败: " + e.getMessage();
                                log.error("[AI] 工具 {} 执行失败: {}", toolName, e.getMessage());
                            }
                        } else {
                            toolResult = "未找到工具: " + toolName;
                            log.warn("[AI] 未找到工具: {}", toolName);
                        }
                        toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
                    }
                    // 将工具执行结果作为 ToolResponseMessage 加入消息列表
                    messages.add(ToolResponseMessage.builder().responses(toolResponses).build());
                }

                if (round >= maxToolRounds) {
                    log.warn("[AI] 工具调用达到最大轮次 {}，强制结束", maxToolRounds);
                }
            } catch (Exception e) {
                log.error("[AI] 模型调用失败: {}", e.getMessage(), e);
                return Flux.error(new AIException(translateApiError(e)));
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[AI] 模型: {}, 总耗时: {}ms, 工具调用轮次: {}", model, elapsed, round);

            // 输出过滤
            response = contentFilterService.filterOutput(response, sessionId, userId, null);
            log.info("[AI] 回复(含工具调用, 模型: {}): {}", model, response);

            chatSyncService.notifyUser(userId, "message_added", sessionId);
            return Flux.just(response);
        }

        // === 无工具路径：使用 stream() 流式输出 ===
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(context.toString())),
                buildChatOptions(provider, model, false)
        );

        // 捕获流式响应中的 Usage 元数据和完整文本，用于服务端保存消息
        java.util.concurrent.atomic.AtomicReference<Usage> lastUsage = new java.util.concurrent.atomic.AtomicReference<>();
        StringBuilder responseAccumulator = new StringBuilder();
        final String resolvedModel = model;

        return chatModel.stream(prompt)
                .doOnNext(chatResponse -> {
                    // 捕获最后一个 chunk 的 Usage 元数据（提供商通常在最后一个 chunk 返回）
                    if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        if (usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
                            lastUsage.set(usage);
                        }
                    }
                })
                .map(chatResponse -> {
                    var generation = chatResponse.getResult();
                    if (generation == null) {
                        return "";
                    }
                    var output = generation.getOutput();
                    if (output == null) {
                        return "";
                    }
                    String text = output.getText();
                    return text != null ? text : "";
                })
                .filter(text -> !text.isEmpty())
                .doOnNext(responseAccumulator::append)
                .doOnComplete(() -> {
                    String fullResponse = responseAccumulator.toString();
                    log.info("[流式] 完成, 响应长度: {}", fullResponse.length());

                    // 服务端保存 AI 消息（含真实/估算 Token 数据），避免前端保存时丢失 Token 信息
                    if (!fullResponse.isBlank()) {
                        try {
                            String filteredResponse = contentFilterService.filterOutput(fullResponse, sessionId, userId, null);
                            int tokens;
                            double cost;
                            Usage usage = lastUsage.get();
                            if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
                                tokens = usage.getTotalTokens();
                                int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                                int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                                cost = calculateCost(resolvedModel, promptTokens, completionTokens);
                                log.info("[流式] 使用真实 Token 数据 - tokens: {}, cost: {}", tokens, cost);
                            } else {
                                tokens = estimateStreamingTokens(fullResponse);
                                cost = estimateStreamingCost(resolvedModel, tokens);
                                log.info("[流式] 使用估算 Token 数据 - tokens: {}, cost: {}", tokens, cost);
                            }

                            ChatMessage aiMsg = new ChatMessage();
                            aiMsg.setSessionId(sessionId);
                            aiMsg.setUserId(userId);
                            aiMsg.setRole(ROLE_ASSISTANT);
                            aiMsg.setContent(filteredResponse);
                            aiMsg.setModel(resolvedModel);
                            aiMsg.setTokens(tokens);
                            aiMsg.setCost(cost);
                            aiMsg.setMessageType(MESSAGE_TYPE_TEXT);
                            messageRepository.save(aiMsg);

                            // 更新会话消息计数
                            long messageCount = messageRepository.countBySessionId(sessionId);
                            ChatSession currentSession = sessionRepository.findBySessionId(sessionId);
                            if (currentSession != null) {
                                currentSession.setMessageCount((int) messageCount);
                                sessionRepository.update(currentSession);
                            }
                        } catch (Exception e) {
                            log.warn("[流式] 服务端保存 AI 消息失败，将由前端兜底保存: {}", e.getMessage());
                        }
                    }

                    chatSyncService.notifyUser(userId, "message_added", sessionId);
                })
                .doOnError(error -> {
                    log.error("[流式] 错误: {}", error.getMessage());
                });
    }

    /**
     * 判断异常是否为不可重试错误（认证失败、余额不足、权限拒绝等）
     * <p>
     * 此类错误重试无意义，应立即失败并返回明确提示。
     *
     * @param error 异常
     * @return true 表示不可重试
     */
    private boolean isNonRetryableError(Throwable error) {
        Throwable cause = error;
        while (cause instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) {
            cause = ce.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("401") || msg.contains("Unauthorized")
                || msg.contains("402") || msg.contains("Insufficient Balance") || msg.contains("insufficient")
                || msg.contains("403") || msg.contains("Forbidden");
    }

    /**
     * 将 AI API 异常翻译为用户友好的中文提示
     */
    private String translateApiError(Throwable error) {
        Throwable cause = error;
        while (cause instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) {
            cause = ce.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null) {
            return "AI 服务调用异常，请稍后重试";
        }
        if (msg.contains("402") || msg.contains("Insufficient Balance") || msg.contains("insufficient")) {
            return "AI 服务账户余额不足，请联系管理员充值";
        }
        if (msg.contains("401") || msg.contains("Unauthorized")) {
            return "AI 服务认证失败，请检查 API Key 配置";
        }
        if (msg.contains("403") || msg.contains("Forbidden")) {
            return "AI 服务访问被拒绝，请检查权限配置";
        }
        if (msg.contains("429") || msg.contains("Rate limit")) {
            return "AI 服务请求频率超限，请稍后再试";
        }
        if (msg.contains("500") || msg.contains("502") || msg.contains("503")) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        if (msg.contains("timeout") || msg.contains("Timeout") || msg.contains("SocketTimeout")) {
            return "AI 服务响应超时，请稍后重试";
        }
        if (msg.contains("choices") || msg.contains("InvalidData")) {
            return "AI 模型返回数据异常，请稍后重试";
        }
        return "AI 服务异常，请稍后重试";
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
        if (chatModel == null) {
            log.error("[AI调用] chatWithSystemPrompt 无法获取默认 ChatModel");
            throw new AIException("AI 模型不可用，请稍后重试");
        }
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage))
        );
        long startTime = System.currentTimeMillis();
        ChatResponse chatResponse = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= AI_CALL_MAX_RETRIES; attempt++) {
            try {
                chatResponse = chatModel.call(prompt);
                break;
            } catch (Exception e) {
                lastException = e;
                // 不可重试的错误立即抛出（认证失败、余额不足、权限拒绝）
                if (isNonRetryableError(e)) {
                    log.error("[AI调用] chatWithSystemPrompt 不可重试错误: {}", e.getMessage());
                    throw new AIException(translateApiError(e));
                }
                if (attempt < AI_CALL_MAX_RETRIES) {
                    long delay = AI_CALL_RETRY_DELAY_MS * (attempt + 1);
                    log.warn("[AI调用] chatWithSystemPrompt 第 {} 次调用失败，{}ms 后重试: {}",
                            attempt + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AIException("AI 服务调用被中断");
                    }
                }
            }
        }

        if (chatResponse == null) {
            log.error("[AI调用] chatWithSystemPrompt 重试 {} 次后仍失败: {}",
                    AI_CALL_MAX_RETRIES, lastException != null ? lastException.getMessage() : "unknown");
            throw new AIException(translateApiError(lastException));
        }

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
        // 1. 检测敏感关键词
        String blockedKeyword = contentFilterService.checkInputBlocked(message);
        if (blockedKeyword != null) {
            // 被拦截，按正确顺序保存
            saveBlockedMessages(sessionId, userId, message, blockedKeyword);
            throw new ContentSecurityException(contentFilterService.getBlockedMessage());
        }

        // 2. 检测 Prompt 注入攻击
        String injectionType = contentFilterService.detectPromptInjection(message);
        if (injectionType != null) {
            log.warn("[Prompt注入防护] 检测到攻击 - type: {}, sessionId: {}, userId: {}", injectionType, sessionId, userId);
            // 保存拦截记录
            savePromptInjectionBlocked(sessionId, userId, message, injectionType);
            throw new ContentSecurityException("抱歉，检测到潜在的安全风险请求，无法处理。请遵守使用规范。");
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
     * @param sessionId      会话ID
     * @param userId         用户ID
     * @param message        用户消息内容
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
     * 保存 Prompt 注入攻击拦截记录
     * <p>
     * 与 saveBlockedMessages 类似，但专门用于 Prompt 注入攻击。
     *
     * @param sessionId  会话ID
     * @param userId     用户ID
     * @param message    用户消息内容
     * @param attackType 攻击类型
     */
    @Transactional
    public void savePromptInjectionBlocked(String sessionId, String userId, String message, String attackType) {
        log.info("[Prompt注入防护] 开始保存拦截记录 - sessionId: {}, userId: {}, attackType: {}", sessionId, userId, attackType);

        try {
            // 第一步：保存用户消息
            LocalDateTime userMsgTime = LocalDateTime.now();
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setUserId(userId);
            userMsg.setRole(ROLE_USER);
            userMsg.setContent(message);
            userMsg.setMessageType(MESSAGE_TYPE_TEXT);
            userMsg.setTimestamp(userMsgTime);
            userMsg.setTokens(0);
            userMsg.setCost(0.0);
            messageRepository.save(userMsg);

            // 第二步：保存系统拦截回复
            LocalDateTime systemMsgTime = userMsgTime.plusNanos(1_000_000);
            ChatMessage systemMsg = new ChatMessage();
            systemMsg.setSessionId(sessionId);
            systemMsg.setUserId(userId);
            systemMsg.setRole(ROLE_ASSISTANT);
            systemMsg.setContent("抱歉，检测到潜在的安全风险请求，无法处理。请遵守使用规范。");
            systemMsg.setModel("prompt-injection-filter"); // 标记为 Prompt 注入过滤
            systemMsg.setMessageType(MESSAGE_TYPE_TEXT);
            systemMsg.setTimestamp(systemMsgTime);
            systemMsg.setTokens(0);
            systemMsg.setCost(0.0);
            messageRepository.save(systemMsg);

            // 第三步：更新会话统计
            long messageCount = messageRepository.countBySessionId(sessionId);
            ChatSession session = sessionRepository.findBySessionId(sessionId);
            if (session != null) {
                session.setMessageCount((int) messageCount);
                if (messageCount == 2) {
                    session.setTitle(message.length() > TITLE_MAX_LENGTH ? message.substring(0, TITLE_MAX_LENGTH) + "..." : message);
                }
                session.setUpdateTime(LocalDateTime.now());
                sessionRepository.update(session);
            }

            // 第四步：记录审计日志
            auditService.logInputBlocked(
                    sessionId,
                    userId,
                    "PROMPT_INJECTION:" + attackType,
                    maskContentForAudit(message)
            );
            log.info("[Prompt注入防护] 审计日志已记录 - sessionId: {}, attackType: {}", sessionId, attackType);

        } catch (Exception e) {
            log.error("[Prompt注入防护] 保存拦截记录失败: {}", e.getMessage(), e);
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
        // 1. 会话总数
        long sessionCount = sessionRepository.countByUserId(userId);

        // 2. 每个会话的消息数量
        Map<String, Integer> messageCounts = new HashMap<>(16);
        List<Map<String, Object>> countRows = messageRepository.findMessageCountsByUserId(userId);
        for (Map<String, Object> row : countRows) {
            String sid = String.valueOf(row.get("session_id"));
            Object cntObj = row.get("cnt");
            int cnt = cntObj instanceof Number num ? num.intValue() : 0;
            messageCounts.put(sid, cnt);
        }

        // 3. 每个会话的最后消息时间（毫秒时间戳）
        Map<String, Long> lastMessageTimes = new HashMap<>(16);
        List<Map<String, Object>> timeRows = messageRepository.findLastMessageTimesByUserId(userId);
        for (Map<String, Object> row : timeRows) {
            String sid = String.valueOf(row.get("session_id"));
            Object lastTime = row.get("last_time");
            if (lastTime instanceof LocalDateTime ldt) {
                lastMessageTimes.put(sid, ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
        }

        // 4. 计算指纹（基于以上数据的哈希值）
        String raw = sessionCount + ":" + messageCounts.toString() + ":" + lastMessageTimes.toString();
        String fingerprint = Integer.toHexString(raw.hashCode());

        return new SyncStatusDTO((int) sessionCount, messageCounts, lastMessageTimes, fingerprint);
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
    public void saveStreamedMessage(String sessionId, String userId, String content, String model,
                                    Integer tokens, Double cost) {
        log.info("保存流式消息 - sessionId: {}, userId: {}, model: {}, content length: {}, tokens: {}, cost: {}",
                sessionId, userId, model, content != null ? content.length() : 0, tokens, cost);

        // 验证会话
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new AIException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            throw new AIException("无权访问此会话");
        }

        // 幂等检查：流式完成后服务端已自动保存 AI 消息，避免前端重复保存
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(sessionId, 1);
        if (!recentMessages.isEmpty()) {
            ChatMessage lastMsg = recentMessages.get(0);
            if (ROLE_ASSISTANT.equals(lastMsg.getRole()) && content != null && content.equals(lastMsg.getContent())) {
                log.info("流式消息已由服务端保存，跳过重复保存 - sessionId: {}", sessionId);
                // 如果服务端保存时使用的是估算值，而前端提供了更准确的 token 数据，则更新
                if (tokens != null && tokens > 0 && (lastMsg.getTokens() == null || lastMsg.getTokens() == 0)) {
                    lastMsg.setTokens(tokens);
                    lastMsg.setCost(cost != null ? cost : 0.0);
                    messageRepository.update(lastMsg);
                    log.info("已更新流式消息的 Token 数据 - sessionId: {}, tokens: {}", sessionId, tokens);
                }
                return;
            }
        }

        // 保存 AI 回复
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole(ROLE_ASSISTANT);
        aiMsg.setContent(content);
        aiMsg.setModel(model);
        aiMsg.setMessageType(MESSAGE_TYPE_TEXT);
        aiMsg.setTokens(tokens != null ? tokens : 0);
        aiMsg.setCost(cost != null ? cost : 0.0);
        messageRepository.save(aiMsg);

        // 更新会话统计信息
        long messageCount = messageRepository.countBySessionId(sessionId);
        session.setMessageCount((int) messageCount);

        // 如果是第一条对话（用户消息+AI回复=2条），用用户的首条消息作为标题
        if (messageCount == 2) {
            ChatMessage firstUserMsg = messageRepository.findFirstUserMessage(sessionId);
            if (firstUserMsg != null) {
                String title = firstUserMsg.getContent();
                session.setTitle(title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) + "..." : title);
            }
        }

        // 手动设置更新时间为当前时间
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.update(session);

        log.info("流式消息保存成功 - sessionId: {}, messageCount: {}", sessionId, messageCount);

        // SSE 通知其他设备：新消息已添加
        chatSyncService.notifyUser(userId, "message_added", sessionId);
    }

    /**
     * 导出会话消息为 Markdown 格式
     * <p>
     * 验证会话归属权后，加载所有消息并按时间顺序格式化为 Markdown 文档。
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID（用于权限校验）
     * @return Markdown 格式的会话内容
     */
    public String exportSessionAsMarkdown(String sessionId, String userId) {
        // 验证会话存在性和归属权
        ChatSession session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new AIException("会话不存在: " + sessionId);
        }
        if (!session.getUserId().equals(userId)) {
            throw new AIException("无权访问此会话");
        }

        // 加载所有消息（按时间正序）
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        if (messages.isEmpty()) {
            return "# " + escapeMarkdown(session.getTitle()) + "\n\n*此会话暂无消息记录。*\n";
        }

        // 构建 Markdown 文档
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(escapeMarkdown(session.getTitle())).append("\n\n");
        markdown.append("- **会话ID**: ").append(sessionId).append("\n");
        markdown.append("- **创建时间**: ").append(formatDateTime(session.getCreateTime())).append("\n");
        markdown.append("- **消息数量**: ").append(messages.size()).append("\n\n");
        markdown.append("---\n\n");

        for (ChatMessage message : messages) {
            String roleLabel = ROLE_USER.equals(message.getRole()) ? "👤 用户" : "🤖 AI 助手";
            String timestamp = formatDateTime(message.getTimestamp());

            markdown.append("### ").append(roleLabel).append("\n\n");
            markdown.append("*").append(timestamp).append("*\n\n");
            markdown.append(message.getContent()).append("\n\n");

            // 如果有模型信息（AI 回复），附加显示
            if (ROLE_ASSISTANT.equals(message.getRole()) && message.getModel() != null) {
                markdown.append("*模型: ").append(message.getModel());
                if (message.getTokens() != null && message.getTokens() > 0) {
                    markdown.append(" | Tokens: ").append(message.getTokens());
                }
                markdown.append("*\n\n");
            }

            markdown.append("---\n\n");
        }

        return markdown.toString();
    }

    /**
     * 格式化日期时间为可读字符串
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 转义 Markdown 特殊字符（标题中的）
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "未命名会话";
        }
        // 转义标题中可能破坏格式的字符
        return text.replace("#", "\\#")
                .replace("*", "\\*")
                .replace("_", "\\_");
    }

    /**
     * 提交对话消息的用户反馈（质量评估）
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     * @param score     评分（1-5）
     * @param comment   反馈备注（可选）
     */
    public void submitFeedback(Long messageId, String userId, Integer score, String comment) {
        if (messageId == null || score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("参数不合法：messageId 和 score(1-5) 为必填项");
        }

        ChatMessage message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在: " + messageId);
        }

        // 验证消息归属
        if (!userId.equals(message.getUserId())) {
            throw new IllegalArgumentException("无权反馈他人的消息");
        }

        message.setFeedbackScore(score);
        message.setFeedbackComment(comment);
        message.setFeedbackTime(LocalDateTime.now());
        messageRepository.update(message);

        log.info("用户反馈已记录: messageId={}, userId={}, score={}", messageId, userId, score);
    }
}
