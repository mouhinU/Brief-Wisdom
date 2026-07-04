package com.mouhin.brief.wisdom.interceptor;

import com.mouhin.brief.wisdom.ai.websocket.WebSocketSyncHandler;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * <p>
 * 在 WebSocket 握手阶段从 HTTP 请求中提取用户身份（userId），
 * 并将其存入 WebSocket 会话属性，供后续消息处理使用。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sync.transport", havingValue = "websocket")
public class WebSocketSyncHandshakeInterceptor implements HandshakeInterceptor {

    private final UserContextHelper userContextHelper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String userId = userContextHelper.getCurrentUserId(servletRequest.getServletRequest());
            attributes.put(WebSocketSyncHandler.ATTR_USER_ID, userId);
            log.info("[WebSocket] 握手成功 - userId: {}, remoteAddr: {}",
                    userId, request.getRemoteAddress());
            return true;
        }
        log.warn("[WebSocket] 非 HTTP 请求，拒绝握手");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外处理
    }
}
