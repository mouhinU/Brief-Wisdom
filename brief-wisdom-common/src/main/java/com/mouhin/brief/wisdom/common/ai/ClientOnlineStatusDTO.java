package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 客户端在线状态 DTO
 * <p>
 * 实时展示当前有多少 AI 智能助手客户端在线连接，
 * 包含传输方式、总用户数、总连接数以及每个用户的连接详情。
 *
 * @author Brief-Wisdom
 * @date 2026-07-07
 */
@Data
public class ClientOnlineStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 传输方式：sse 或 websocket
     */
    private String transport;

    /**
     * 在线用户总数（去重）
     */
    private int totalOnlineUsers;

    /**
     * 总连接数（同一用户多端连接会累计）
     */
    private int totalConnections;

    /**
     * 服务端当前时间戳（毫秒）
     */
    private long serverTime;

    /**
     * 每个用户的连接详情列表
     */
    private List<UserConnectionInfo> userConnections;

    /**
     * 单个用户的连接信息
     */
    @Data
    public static class UserConnectionInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 该用户的连接数
         */
        private int connectionCount;
    }
}
