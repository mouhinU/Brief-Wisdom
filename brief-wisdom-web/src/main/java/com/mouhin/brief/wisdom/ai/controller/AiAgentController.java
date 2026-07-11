package com.mouhin.brief.wisdom.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.ai.req.ChatRequest;
import com.mouhin.brief.wisdom.ai.req.ChatWithPromptRequest;
import com.mouhin.brief.wisdom.ai.req.MessageHistoryQueryRequest;
import com.mouhin.brief.wisdom.ai.req.QuestionRequest;
import com.mouhin.brief.wisdom.ai.req.SaveStreamedMessageRequest;
import com.mouhin.brief.wisdom.ai.req.SessionCreateRequest;
import com.mouhin.brief.wisdom.ai.req.SessionListQueryRequest;
import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.ai.service.AiModelService;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.common.ai.SyncStatusDTO;
import com.mouhin.brief.wisdom.config.PaginationProperties;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
/**
 * AiAgentController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI对话", description = "AI 智能对话、会话管理、模型列表相关接口")
public class AiAgentController {

    /**
     * SSE 连接超时时间：30 分钟
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final AiAgentService aiAgentService;
    private final PaginationProperties paginationProperties;
    private final UserContextHelper userContextHelper;
    private final AiModelService aiModelService;
    private final Executor briefWisdomExecutor;
    private final ObjectMapper objectMapper;

    @Value("${app.sync.transport:sse}")
    private String syncTransport;

    @Value("${app.chat.streaming:true}")
    private boolean chatStreamingEnabled;

    /**
     * 简单聊天接口（无上下文）
     */
    @Operation(summary = "简单聊天", description = "无上下文的单次 AI 对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return aiAgentService.chat(request.getMessage());
    }

    /**
     * 带上下文的聊天接口
     */
    @Operation(summary = "带会话上下文聊天", description = "在指定会话中进行 AI 对话，携带上下文")
    @PostMapping("/chat/session/{sessionId}")
    public String chatWithSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @RequestBody ChatRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        log.info("收到聊天请求 - sessionId: {}, userId: {}, message: {}, model: {}, pageContext: {}", sessionId, userId, request.getMessage(), request.getModel(), request.getPageContext());
        return aiAgentService.chatWithSession(sessionId, userId, request.getMessage(), request.getModel(), request.getPageContext());
    }

    /**
     * 流式聊天接口（SSE）
     * <p>
     * 根据配置 app.chat.streaming 决定是否启用流式输出。
     * 返回 SSE 格式数据，前端使用 EventSource 接收。
     *
     * @param sessionId 会话ID
     * @param request   聊天请求
     * @return SseEmitter
     */
    @Operation(summary = "流式聊天（SSE）", description = "流式输出 AI 回复，前端使用 EventSource 接收")
    @GetMapping(value = "/chat/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamWithSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            ChatRequest request) {
        String userId = userContextHelper.getCurrentUserId();

        log.info("收到流式聊天请求 - sessionId: {}, userId: {}, message: {}, model: {}",
                sessionId, userId, request.getMessage(), request.getModel());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 异步处理流式响应（使用自定义线程池）
        CompletableFuture.runAsync(() -> {
            try {
                // 在异步流内部进行输入安全检查，避免媒体类型冲突
                aiAgentService.checkInputSafety(request.getMessage(), sessionId, userId);
                
                aiAgentService.chatStreamWithSession(
                        sessionId,
                        userId,
                        request.getMessage(),
                        request.getModel(),
                        request.getPageContext()
                ).subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(chunk));
                                }
                            } catch (IOException e) {
                                log.error("[流式] 发送数据失败: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("[流式] 错误: {}", error.getMessage(), error);
                            sendSseErrorEvent(emitter, "STREAM_ERROR", translateApiError(error));
                        },
                        () -> {
                            log.info("[流式] 完成");
                            try {
                                // 发送完成事件
                                emitter.send(SseEmitter.event().name("complete").data("[DONE]"));
                            } catch (IOException e) {
                                log.error("[流式] 发送完成事件失败: {}", e.getMessage());
                            }
                            emitter.complete();
                        }
                );
            } catch (com.mouhin.brief.wisdom.exception.ContentSecurityException e) {
                log.warn("[内容安全] 流式聊天输入被拦截: {}", e.getMessage());
                sendSseErrorEvent(emitter, "CONTENT_BLOCKED", e.getMessage());
            } catch (Exception e) {
                log.error("[流式] 启动失败: {}", e.getMessage());
                sendSseErrorEvent(emitter, "STREAM_ERROR", "流式聊天启动失败");
            }
        }, briefWisdomExecutor);

        return emitter;
    }

    /**
     * 创建新会话
     * <p>
     * 支持传入 pageContext 记录会话来源页面
     */
    @Operation(summary = "创建新会话", description = "创建新的 AI 对话会话，支持传入页面上下文")
    @PostMapping("/session")
    public String createSession(@RequestBody(required = false) SessionCreateRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        String pageContext = (request != null) ? request.getPageContext() : null;
        String sessionId = aiAgentService.createSession(userId, pageContext);
        return sessionId;
    }

    /**
     * 删除会话
     */
    @Operation(summary = "删除会话", description = "删除指定的 AI 对话会话")
    @DeleteMapping("/session/{sessionId}")
    public Boolean deleteSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        aiAgentService.deleteSession(sessionId);
        return true;
    }

    /**
     * 重命名会话标题
     */
    @Operation(summary = "重命名会话标题")
    @PutMapping("/session/{sessionId}/title")
    public Boolean renameSession(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String newTitle = body.get("title");
        aiAgentService.renameSession(sessionId, newTitle);
        return true;
    }

    /**
     * 获取当前登录用户的会话列表（支持分页）
     * <p>
     * 默认每页条数和最大值由 application.yml 中 app.pagination.session-list 配置
     *
     * @param request 查询请求（包含页码、每页大小）
     */
    @Operation(summary = "获取会话列表", description = "获取当前登录用户的会话列表，支持分页")
    @PostMapping("/sessions")
    public PageResult<SessionMetaDTO> listSessions(@RequestBody SessionListQueryRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        PaginationProperties.PageConfig config = paginationProperties.getSessionList();
        int resolvedSize = config.resolveSize(request.getSize());
        return aiAgentService.listSessionsPaged(userId, request.getPage(), resolvedSize);
    }

    /**
     * 获取会话历史消息（支持分页，第1页为最新消息）
     * <p>
     * 默认每页条数由 application.yml 中 app.pagination.message-history 配置
     *
     * @param sessionId 会话ID
     * @param request   查询请求（包含页码、每页大小）
     */
    @Operation(summary = "获取会话历史消息", description = "获取指定会话的聊天历史，支持分页，第1页为最新消息")
    @PostMapping("/session/{sessionId}/history")
    public PageResult<ChatMessageDTO> getSessionHistory(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @RequestBody MessageHistoryQueryRequest request) {
        PaginationProperties.PageConfig config = paginationProperties.getMessageHistory();
        int resolvedSize = config.resolveSize(request.getSize());
        return aiAgentService.getSessionHistoryPaged(sessionId, request.getPage(), resolvedSize);
    }

    /**
     * 获取分页配置信息
     * <p>
     * 前端可调用此接口获取各业务的默认分页大小，避免硬编码
     */
    @Operation(summary = "获取分页配置", description = "获取各业务的默认分页大小配置")
    @GetMapping("/config/pagination")
    public Map<String, Object> getPaginationConfig() {
        return Map.of(
                "sessionList", Map.of(
                        "defaultSize", paginationProperties.getSessionList().getDefaultSize(),
                        "maxSize", paginationProperties.getSessionList().getMaxSize()
                ),
                "messageHistory", Map.of(
                        "defaultSize", paginationProperties.getMessageHistory().getDefaultSize(),
                        "maxSize", paginationProperties.getMessageHistory().getMaxSize()
                )
        );
    }

    /**
     * 获取聊天模式配置
     * <p>
     * 返回是否启用流式输出（打字机效果）
     */
    @Operation(summary = "获取聊天模式配置", description = "返回是否启用流式输出")
    @GetMapping("/config/chat")
    public Map<String, Object> getChatConfig() {
        return Map.of("streaming", chatStreamingEnabled);
    }

    /**
     * 保存流式输出的 AI 消息到数据库
     * <p>
     * 前端在接收到完整的流式响应后调用此接口
     *
     * @param request 保存请求
     * @return 是否成功
     */
    @Operation(summary = "保存流式消息", description = "前端接收完整流式响应后调用此接口持久化消息")
    @PostMapping("/message/save")
    public Boolean saveStreamedMessage(@RequestBody SaveStreamedMessageRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        log.info("收到保存流式消息请求 - sessionId: {}, userId: {}, model: {}",
                request.getSessionId(), userId, request.getModel());
        
        aiAgentService.saveStreamedMessage(
                request.getSessionId(),
                userId,
                request.getContent(),
                request.getModel()
        );
        
        return true;
    }

    /**
     * 带系统提示的聊天接口
     */
    @Operation(summary = "带系统提示聊天", description = "携带自定义系统提示词的 AI 对话")
    @PostMapping("/chat-with-prompt")
    public String chatWithPrompt(@RequestBody ChatWithPromptRequest request) {
        // 输入校验
        if (request.getSystemPrompt() == null || request.getSystemPrompt().isBlank()) {
            throw new AIException("系统提示词不能为空");
        }
        if (request.getUserMessage() == null || request.getUserMessage().isBlank()) {
            throw new AIException("用户消息不能为空");
        }
        // 安全检查：防止恶意 systemPrompt 注入
        aiAgentService.checkInputSafety(request.getUserMessage(), "N/A", "N/A");
        return aiAgentService.chatWithSystemPrompt(
                request.getSystemPrompt(),
                request.getUserMessage()
        );
    }

    /**
     * 智能问答接口
     */
    @Operation(summary = "智能问答", description = "基于知识库的智能问答接口")
    @PostMapping("/ask")
    public String ask(@RequestBody QuestionRequest request) {
        return aiAgentService.askQuestion(request.getQuestion());
    }

    /**
     * 获取当前用户的同步状态（用于多端同步检测）
     * <p>
     * 返回轻量级同步指纹，前端定时轮询此接口，对比 fingerprint 判断是否需要刷新数据。
     */
    @Operation(summary = "获取同步状态", description = "获取当前用户的多端同步状态指纹")
    @GetMapping("/sync/status")
    public SyncStatusDTO getSyncStatus() {
        String userId = userContextHelper.getCurrentUserId();
        return aiAgentService.getSyncStatus(userId);
    }

    /**
     * 获取启用的 AI 模型列表（公开接口，无需权限）
     * <p>
     * 前端聊天页面选择器使用此接口
     */
    @Operation(summary = "获取启用模型列表", description = "获取所有已启用的 AI 模型列表，前端聊天选择器使用")
    @GetMapping("/models/enabled")
    public List<AiModelDTO> listEnabledModels() {
        return aiModelService.listEnabledModels();
    }

    /**
     * 获取当前同步传输方式
     * <p>
     * 前端根据此接口返回值决定使用 SSE（EventSource）还是 WebSocket 进行实时同步连接。
     * 返回值为 "sse" 或 "websocket"。
     */
    @Operation(summary = "获取同步传输方式", description = "返回当前同步传输方式（sse 或 websocket）")
    @GetMapping("/sync/transport")
    public Map<String, String> getSyncTransport() {
        return Map.of("transport", syncTransport);
    }

    /**
     * 将 AI API 异常翻译为用户友好的中文提示
     */
    private String translateApiError(Throwable error) {
        // 解包 CompletionException
        Throwable cause = error;
        while (cause.getCause() != null && cause instanceof java.util.concurrent.CompletionException) {
            cause = cause.getCause();
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
        return "AI 服务异常：" + msg;
    }

    /**
     * 通过 SSE 发送 JSON 格式错误事件，避免字符串拼接破坏 JSON 结构
     */
    private void sendSseErrorEvent(SseEmitter emitter, String type, String message) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "message", message != null ? message : "未知错误"
            ));
            emitter.send(SseEmitter.event().name("error").data(payload));
        } catch (IOException ioException) {
            log.error("[流式] 发送错误消息失败: {}", ioException.getMessage());
        }
        emitter.complete();
    }
}
