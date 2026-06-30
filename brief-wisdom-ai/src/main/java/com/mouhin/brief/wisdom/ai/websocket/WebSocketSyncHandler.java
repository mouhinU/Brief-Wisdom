package com.mouhin.brief.wisdom.ai.websocket;

import com.mouhin.brief.wisdom.ai.service.WebSocketChatSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 同步处理器
 * <p>
 * 处理 WebSocket 连接的建立、消息接收和关闭事件。
 * 连接建立时通过 {@link WebSocketSyncHandshakeInterceptor} 注入的 userId 属性识别用户身份。
 * <p>
 * 消息协议：
 * <ul>
 *   <li>客户端 → 服务端：{"type":"ping"} （心跳保活）</li>
 *   <li>服务端 → 客户端：{"type":"connected"} （连接确认）</li>
 *   <li>服务端 → 客户端：{"type":"sync","eventType":"...","sessionId":"..."} （同步事件）</li>
 * </ul>
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sync.transport", havingValue = "websocket")
public class WebSocketSyncHandler extends TextWebSocketHandler {

    /**
     * WebSocket 会话属性：用户ID
     */
    public static final String ATTR_USER_ID = "userId";

    private final WebSocketChatSyncService syncService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null || userId.isBlank()) {
            log.warn("[WebSocket] 连接缺少 userId，sessionId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 注册会话
        syncService.registerSession(userId, session);

        // 发送连接确认消息
        String connectedMsg = "{\"type\":\"connected\",\"message\":\"WebSocket 连接已建立\"}";
        session.sendMessage(new TextMessage(connectedMsg));

        log.info("[WebSocket] 连接建立完成 - userId: {}, sessionId: {}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("[WebSocket] 收到消息 - sessionId: {}, payload: {}", session.getId(), payload);

        // 简单处理心跳 ping
        if (payload.contains("\"ping\"")) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId != null) {
            syncService.unregisterSession(userId, session);
        }
        log.info("[WebSocket] 连接关闭 - userId: {}, sessionId: {}, status: {}",
                userId, session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        log.warn("[WebSocket] 传输错误 - userId: {}, sessionId: {}, error: {}",
                userId, session.getId(), exception.getMessage());

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
