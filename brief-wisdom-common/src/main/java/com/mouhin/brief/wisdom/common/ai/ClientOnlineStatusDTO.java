package com.mouhin.brief.wisdom.common.ai;

import java.io.Serializable;
import java.util.List;

/**
 * 客户端在线状态传输对象
 * <p>
 * 实时展示当前有多少 AI 智能助手客户端在线连接，
 * 包含传输方式、总用户数、总连接数以及每个用户的连接详情。
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record ClientOnlineStatusDTO(
        String transport,
        int totalOnlineUsers,
        int totalConnections,
        long serverTime,
        List<UserConnectionInfo> userConnections
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public String getTransport() { return transport; }

    public int getTotalOnlineUsers() { return totalOnlineUsers; }

    public int getTotalConnections() { return totalConnections; }

    public long getServerTime() { return serverTime; }

    public List<UserConnectionInfo> getUserConnections() { return userConnections; }

    /**
     * 单个用户的连接信息
     *
     * @author Brief-Wisdom
     * @date 2026-07-22
     */
    public record UserConnectionInfo(
            String userId,
            String nickname,
            int connectionCount
    ) implements Serializable {

        private static final long serialVersionUID = 1L;

        public String getUserId() { return userId; }

        public String getNickname() { return nickname; }

        public int getConnectionCount() { return connectionCount; }
    }
}
