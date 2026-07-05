package com.mouhin.brief.wisdom.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于 WebSocket 的聊天同步服务实现
 * <p>
 * 通过 WebSocket 双向通信实现多端同步，仅在 {@code app.sync.transport=websocket} 时激活。
 * 连接管理由 {@link com.mouhin.brief.wisdom.websocket.WebSocketSyncHandler} 负责，
 * 本类通过注册表维护 userId 与 WebSocket 会话的映射关系。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.sync.transport", havingValue = "websocket")
public class WebSocketChatSyncService implements ChatSyncService {

    /**
     * 用户ID → WebSocket 会话列表（支持同一用户多端连接）
     */
    private final Map<String, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册一个 WebSocket 会话（由 WebSocketSyncHandler 在连接建立时调用）
     *
     * @param userId  用户ID
     * @param session WebSocket 会话
     */
    public void registerSession(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
        log.info("[WebSocket] 用户 {} 建立连接，sessionId: {}，当前连接数: {}", userId, session.getId(), getSessionCount(userId));
    }

    /**
     * 注销一个 WebSocket 会话（由 WebSocketSyncHandler 在连接关闭时调用）
     *
     * @param userId  用户ID
     * @param session WebSocket 会话
     */
    public void unregisterSession(String userId, WebSocketSession session) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("[WebSocket] 用户 {} 断开连接，sessionId: {}，剩余连接数: {}",
                userId, session.getId(), getSessionCount(userId));
    }

    @Override
    public void notifyUser(String userId, String eventType, String sessionId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String data = buildEventData(eventType, sessionId);
        TextMessage message = new TextMessage(data);

        int successCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                    successCount++;
                } catch (IOException e) {
                    log.debug("[WebSocket] 推送失败，sessionId: {}, error: {}", session.getId(), e.getMessage());
                }
            }
        }

        if (successCount > 0) {
            log.debug("[WebSocket] 向用户 {} 推送事件 {} 成功，共 {} 个连接", userId, eventType, successCount);
        }
    }

    @Override
    public void broadcastToAll(String eventType, String sessionId) {
        if (userSessions.isEmpty()) {
            return;
        }

        String data = buildEventData(eventType, sessionId);
        TextMessage message = new TextMessage(data);

        for (Map.Entry<String, List<WebSocketSession>> entry : userSessions.entrySet()) {
            List<WebSocketSession> sessions = entry.getValue();
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    } catch (IOException e) {
                        log.debug("[WebSocket] 广播失败，sessionId: {}, error: {}", session.getId(), e.getMessage());
                    }
                }
            }
        }
        log.debug("[WebSocket] 广播事件 {} 完成", eventType);
    }

    /**
     * 获取指定用户当前的在线连接数
     */
    public int getSessionCount(String userId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 构建事件 JSON 数据（安全转义，防止 JSON 注入）
     */
    private String buildEventData(String eventType, String sessionId) {
        StringBuilder data = new StringBuilder();
        data.append("{\"type\":\"").append(escapeJson(eventType)).append("\"");
        if (sessionId != null) {
            data.append(",\"sessionId\":\"").append(escapeJson(sessionId)).append("\"");
        }
        data.append("}");
        return data.toString();
    }

    /**
     * 简单 JSON 字符串转义（与 SseChatSyncService 保持一致）
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
