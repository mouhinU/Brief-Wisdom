package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.req.ChatRequest;
import com.mouhin.brief.wisdom.ai.req.ChatWithPromptRequest;
import com.mouhin.brief.wisdom.ai.req.QuestionRequest;
import com.mouhin.brief.wisdom.ai.req.SaveStreamedMessageRequest;
import com.mouhin.brief.wisdom.ai.req.SessionCreateRequest;
import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.ai.service.AiModelService;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.common.ai.SyncStatusDTO;
import com.mouhin.brief.wisdom.config.PaginationProperties;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
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
/**
 * AiAgentController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final PaginationProperties paginationProperties;
    private final UserContextHelper userContextHelper;
    private final AiModelService aiModelService;

    @Value("${app.sync.transport:sse}")
    private String syncTransport;

    @Value("${app.chat.streaming:true}")
    private boolean chatStreamingEnabled;

    /**
     * 简单聊天接口（无上下文）
     */
    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return aiAgentService.chat(request.getMessage());
    }

    /**
     * 带上下文的聊天接口
     */
    @PostMapping("/chat/session/{sessionId}")
    public String chatWithSession(@PathVariable String sessionId, @RequestBody ChatRequest request) {
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
    @GetMapping(value = "/chat/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamWithSession(@PathVariable String sessionId, ChatRequest request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String userId = userContextHelper.getCurrentUserId();

        log.info("收到流式聊天请求 - sessionId: {}, userId: {}, message: {}, model: {}",
                sessionId, userId, request.getMessage(), request.getModel());

        // 异步处理流式响应
        CompletableFuture.runAsync(() -> {
            try {
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
                            emitter.completeWithError(error);
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
            } catch (Exception e) {
                log.error("[流式] 启动失败: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 创建新会话
     * <p>
     * 支持传入 pageContext 记录会话来源页面
     */
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
    @DeleteMapping("/session/{sessionId}")
    public Boolean deleteSession(@PathVariable String sessionId) {
        aiAgentService.deleteSession(sessionId);
        return true;
    }

    /**
     * 重命名会话标题
     */
    @PutMapping("/session/{sessionId}/title")
    public Boolean renameSession(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String newTitle = body.get("title");
        aiAgentService.renameSession(sessionId, newTitle);
        return true;
    }

    /**
     * 获取当前登录用户的会话列表（支持分页）
     * <p>
     * 默认每页条数和最大值由 application.yml 中 app.pagination.session-list 配置
     *
     * @param page 当前页码，从 1 开始，默认 1
     * @param size 每页大小，不传则使用配置的默认值，超过配置的最大值会被截断
     */
    @GetMapping("/sessions")
    public PageResult<SessionMetaDTO> listSessions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        String userId = userContextHelper.getCurrentUserId();
        PaginationProperties.PageConfig config = paginationProperties.getSessionList();
        int resolvedSize = config.resolveSize(size);
        return aiAgentService.listSessionsPaged(userId, page, resolvedSize);
    }

    /**
     * 获取会话历史消息（支持分页，第1页为最新消息）
     * <p>
     * 默认每页条数由 application.yml 中 app.pagination.message-history 配置
     *
     * @param sessionId 会话ID
     * @param page      当前页码，从 1 开始，默认 1
     * @param size      每页大小，不传则使用配置的默认值，超过配置的最大值会被截断
     */
    @GetMapping("/session/{sessionId}/history")
    public PageResult<ChatMessageDTO> getSessionHistory(
            @PathVariable String sessionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        PaginationProperties.PageConfig config = paginationProperties.getMessageHistory();
        int resolvedSize = config.resolveSize(size);
        return aiAgentService.getSessionHistoryPaged(sessionId, page, resolvedSize);
    }

    /**
     * 获取分页配置信息
     * <p>
     * 前端可调用此接口获取各业务的默认分页大小，避免硬编码
     */
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
    @PostMapping("/chat-with-prompt")
    public String chatWithPrompt(@RequestBody ChatWithPromptRequest request) {
        return aiAgentService.chatWithSystemPrompt(
                request.getSystemPrompt(),
                request.getUserMessage()
        );
    }

    /**
     * 智能问答接口
     */
    @PostMapping("/ask")
    public String ask(@RequestBody QuestionRequest request) {
        return aiAgentService.askQuestion(request.getQuestion());
    }

    /**
     * 获取当前用户的同步状态（用于多端同步检测）
     * <p>
     * 返回轻量级同步指纹，前端定时轮询此接口，对比 fingerprint 判断是否需要刷新数据。
     */
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
    @GetMapping("/sync/transport")
    public Map<String, String> getSyncTransport() {
        return Map.of("transport", syncTransport);
    }
}
