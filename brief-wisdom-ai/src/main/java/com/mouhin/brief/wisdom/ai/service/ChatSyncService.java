package com.mouhin.brief.wisdom.ai.service;

/**
 * AI 聊天多端同步服务接口
 * <p>
 * 定义同步事件推送的统一抽象，支持 SSE 和 WebSocket 两种传输方式。
 * 具体实现由配置项 {@code app.sync.transport} 决定。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
public interface ChatSyncService {

    /**
     * 向指定用户的所有在线设备推送同步事件
     *
     * @param userId    用户ID
     * @param eventType 事件类型（session_created / session_deleted / message_added）
     * @param sessionId 相关会话ID（可为 null）
     */
    void notifyUser(String userId, String eventType, String sessionId);

    /**
     * 向所有已连接的用户广播同步事件（用于无法确定目标用户的场景，如删除会话）
     *
     * @param eventType 事件类型
     * @param sessionId 相关会话ID（可为 null）
     */
    void broadcastToAll(String eventType, String sessionId);
}
