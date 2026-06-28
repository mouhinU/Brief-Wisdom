package com.mouhin.brief.wisdom.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 聊天多端同步服务（SSE 实时推送）
 * <p>
 * 管理每个用户的 SSE 连接，当数据发生变更时向该用户的所有在线设备推送事件通知。
 * 前端通过 EventSource 接收事件后，按需拉取最新数据。
 */
@Slf4j
@Service
public class ChatSyncService {

    /**
     * 每个用户对应的 SseEmitter 列表（支持同一用户多端连接）
     */
    private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * SSE 超时时间：30 分钟（长连接）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 为指定用户创建一个新的 SSE 连接
     *
     * @param userId 用户ID
     * @return SseEmitter 实例
     */
    public SseEmitter createConnection(String userId) {
        // 清理该用户的所有旧连接（前端关闭 EventSource 后服务端不会立即感知）
//        clearUserEmitters(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 注册回调：连接完成/超时/出错时自动移除
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // 添加到用户连接列表
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("[SSE] 用户 {} 建立连接，当前连接数: {}", userId, getEmitterCount(userId));

        // 发送初始连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE 连接已建立\"}"));
        } catch (IOException e) {
            log.warn("[SSE] 发送连接确认失败: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * 向指定用户的所有在线设备推送同步事件
     *
     * @param userId    用户ID
     * @param eventType 事件类型（session_created / session_deleted / message_added）
     * @param sessionId 相关会话ID（可为 null）
     */
    public void notifyUser(String userId, String eventType, String sessionId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        // 构建事件数据
        StringBuilder data = new StringBuilder();
        data.append("{\"type\":\"").append(eventType).append("\"");
        if (sessionId != null) {
            data.append(",\"sessionId\":\"").append(sessionId).append("\"");
        }
        data.append("}");

        int successCount = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("sync")
                        .data(data.toString()));
                successCount++;
            } catch (IOException e) {
                log.debug("[SSE] 推送失败，移除连接: {}", e.getMessage());
                removeEmitter(userId, emitter);
            }
        }

        if (successCount > 0) {
            log.debug("[SSE] 向用户 {} 推送事件 {} 成功，共 {} 个连接", userId, eventType, successCount);
        }
    }

    /**
     * 向所有已连接的用户广播同步事件（用于无法确定目标用户的场景，如删除会话）
     *
     * @param eventType 事件类型
     * @param sessionId 相关会话ID（可为 null）
     */
    public void broadcastToAll(String eventType, String sessionId) {
        if (userEmitters.isEmpty()) {
            return;
        }

        StringBuilder data = new StringBuilder();
        data.append("{\"type\":\"").append(eventType).append("\"");
        if (sessionId != null) {
            data.append(",\"sessionId\":\"").append(sessionId).append("\"");
        }
        data.append("}");

        for (Map.Entry<String, List<SseEmitter>> entry : userEmitters.entrySet()) {
            String userId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("sync")
                            .data(data.toString()));
                } catch (IOException e) {
                    log.debug("[SSE] 广播失败，移除连接: {}", e.getMessage());
                    removeEmitter(userId, emitter);
                }
            }
        }
        log.debug("[SSE] 广播事件 {} 完成", eventType);
    }

    /**
     * 获取指定用户当前的在线连接数
     */
    public int getEmitterCount(String userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * 断开指定用户的所有 SSE 连接（前端关闭聊天窗口时调用）
     */
    public void disconnectUser(String userId) {
        List<SseEmitter> oldEmitters = userEmitters.remove(userId);
        if (oldEmitters != null) {
            for (SseEmitter emitter : oldEmitters) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 忽略：可能已经超时或出错
                }
            }
            log.info("[SSE] 用户 {} 断开连接，清理 {} 个连接", userId, oldEmitters.size());
        }
    }

    /**
     * 移除指定用户的某个 SSE 连接
     */
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        // 主动完成 emitter（防止重复 complete 异常）
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 忽略：可能已经超时或出错
        }
    }
}
