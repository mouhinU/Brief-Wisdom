package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.service.SseChatSyncService;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 实时同步事件控制器
 * <p>
 * 仅在 {@code app.sync.transport=sse} 时激活，提供 SSE 连接的建立和断开端点。
 * WebSocket 模式下此控制器不会被加载。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/ai/sync")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.sync.transport", havingValue = "sse", matchIfMissing = true)
@Tag(name = "AI同步", description = "SSE 实时同步事件流接口")
public class SseSyncController {

    private final SseChatSyncService sseChatSyncService;
    private final UserContextHelper userContextHelper;

    /**
     * SSE 实时同步事件流
     * <p>
     * 前端通过 EventSource 连接此端点，当数据发生变更（创建/删除会话、发送消息）时，
     * 服务端实时推送 sync 事件，前端收到后按需拉取最新数据。
     * <p>
     * 支持多端同时连接，同一用户的所有设备都会收到通知。
     */
    // 注意：不使用 produces = "text/event-stream"，
    // 因为 produces 约束会在异常时传递给 GlobalExceptionHandler，
    // 导致 handler 无法返回 JSON（HttpMediaTypeNotAcceptableException）。
    // SseEmitter 自身会自动设置 Content-Type: text/event-stream 响应头。
    @GetMapping(value = "/events")
    @Operation(summary = "建立 SSE 同步事件流", description = "前端通过 EventSource 连接，接收会话与消息变更通知")
    public SseEmitter syncEvents() {
        String userId = userContextHelper.getCurrentUserId();
        log.info("[SSE] 用户 {} 请求建立 SSE 连接", userId);
        return sseChatSyncService.createConnection(userId);
    }

    /**
     * 断开 SSE 连接
     * <p>
     * 前端关闭聊天窗口时调用，主动清理服务端的 SSE 连接资源。
     */
    @DeleteMapping("/events")
    @Operation(summary = "断开 SSE 同步连接", description = "前端关闭聊天窗口时主动清理服务端连接资源")
    public Boolean disconnectSync() {
        String userId = userContextHelper.getCurrentUserId();
        sseChatSyncService.disconnectUser(userId);
        log.info("[SSE] 用户 {} 主动断开 SSE 连接", userId);
        return true;
    }
}
