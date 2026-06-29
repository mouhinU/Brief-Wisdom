package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.req.ChatRequest;
import com.mouhin.brief.wisdom.ai.req.ChatWithPromptRequest;
import com.mouhin.brief.wisdom.ai.req.QuestionRequest;
import com.mouhin.brief.wisdom.ai.req.SessionCreateRequest;
import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.ai.service.ChatSyncService;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.common.ai.SyncStatusDTO;
import com.mouhin.brief.wisdom.config.PaginationProperties;
import com.mouhin.brief.wisdom.web.service.UserContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final PaginationProperties paginationProperties;
    private final UserContextHelper userContextHelper;
    private final ChatSyncService chatSyncService;

    /**
     * 简单聊天接口（无上下文）
     */
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        String response = aiAgentService.chat(request.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * 带上下文的聊天接口
     */
    @PostMapping("/chat/session/{sessionId}")
    public ResponseEntity<String> chatWithSession(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        log.info("收到聊天请求 - sessionId: {}, userId: {}, message: {}, model: {}", sessionId, userId, request.getMessage(), request.getModel());
        String response = aiAgentService.chatWithSession(sessionId, userId, request.getMessage(), request.getModel());
        return ResponseEntity.ok(response);
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
        var result = aiAgentService.listSessionsPaged(userId, page, resolvedSize);
        return result;
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
        var result = aiAgentService.getSessionHistoryPaged(sessionId, page, resolvedSize);
        return result;
    }

    /**
     * 获取分页配置信息
     * <p>
     * 前端可调用此接口获取各业务的默认分页大小，避免硬编码
     */
    @GetMapping("/config/pagination")
    public Map<String, Object> getPaginationConfig() {
        Map<String, Object> config = Map.of(
                "sessionList", Map.of(
                        "defaultSize", paginationProperties.getSessionList().getDefaultSize(),
                        "maxSize", paginationProperties.getSessionList().getMaxSize()
                ),
                "messageHistory", Map.of(
                        "defaultSize", paginationProperties.getMessageHistory().getDefaultSize(),
                        "maxSize", paginationProperties.getMessageHistory().getMaxSize()
                )
        );
        return config;
    }

    /**
     * 带系统提示的聊天接口
     */
    @PostMapping("/chat-with-prompt")
    public String chatWithPrompt(@RequestBody ChatWithPromptRequest request) {
        String response = aiAgentService.chatWithSystemPrompt(
                request.getSystemPrompt(),
                request.getUserMessage()
        );
        return response;
    }

    /**
     * 智能问答接口
     */
    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody QuestionRequest request) {
        String response = aiAgentService.askQuestion(request.getQuestion());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的同步状态（用于多端同步检测）
     * <p>
     * 返回轻量级同步指纹，前端定时轮询此接口，对比 fingerprint 判断是否需要刷新数据。
     */
    @GetMapping("/sync/status")
    public SyncStatusDTO getSyncStatus() {
        String userId = userContextHelper.getCurrentUserId();
        SyncStatusDTO syncStatus = aiAgentService.getSyncStatus(userId);
        return syncStatus;
    }

    /**
     * SSE 实时同步事件流
     * <p>
     * 前端通过 EventSource 连接此端点，当数据发生变更（创建/删除会话、发送消息）时，
     * 服务端实时推送 sync 事件，前端收到后按需拉取最新数据。
     * <p>
     * 支持多端同时连接，同一用户的所有设备都会收到通知。
     */
    @GetMapping(value = "/sync/events", produces = "text/event-stream")
    public SseEmitter syncEvents() {
        String userId = userContextHelper.getCurrentUserId();

        log.info("[SSE] 用户 {} 请求建立 SSE 连接", userId);
        return chatSyncService.createConnection(userId);
    }

    /**
     * 断开 SSE 连接
     * <p>
     * 前端关闭聊天窗口时调用，主动清理服务端的 SSE 连接资源。
     */
    @DeleteMapping("/sync/events")
    public Boolean disconnectSync() {
        String userId = userContextHelper.getCurrentUserId();
        chatSyncService.disconnectUser(userId);
        log.info("[SSE] 用户 {} 主动断开 SSE 连接", userId);
        return true;
    }
}
