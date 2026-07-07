package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.websocket.WebSocketSyncHandler;
import com.mouhin.brief.wisdom.interceptor.WebSocketSyncHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * <p>
 * 仅在 {@code app.sync.transport=websocket} 时激活。
 * 注册 WebSocket 处理器和握手拦截器，端点路径为 {@code /ws/sync}。
 * <p>
 * WebSocket 允许的域名通过 app.cors.allowed-origins 统一配置。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sync.transport", havingValue = "websocket")
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketSyncHandler webSocketSyncHandler;
    private final WebSocketSyncHandshakeInterceptor handshakeInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:8090,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addHandler(webSocketSyncHandler, "/ws/sync")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(origins);
    }
}
