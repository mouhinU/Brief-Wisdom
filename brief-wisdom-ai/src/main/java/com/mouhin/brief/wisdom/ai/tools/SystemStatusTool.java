package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import com.mouhin.brief.wisdom.persistence.repository.ChatMessageRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatSessionRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 系统状态查询工具
 * <p>
 * 允许管理员通过 AI 助手查询系统运行状态，包括用户数、会话数、Redis 状态等。
 * 仅限管理员角色使用。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemStatusTool {

    private final ChatUserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final StringRedisTemplate redisTemplate;
    private final ToolContextProvider toolContextProvider;

    /**
     * 查询系统运行状态
     *
     * @param type 查询类型
     * @return 系统状态信息
     */
    @Tool(description = "查询系统运行状态，包括用户数、会话数、消息数、Redis 状态等。仅限管理员使用。当管理员问'系统状态如何'、'有多少用户'、'Redis 正常吗'时调用。")
    public String getSystemStatus(
            @ToolParam(description = "查询类型: overview(概览)/users(用户统计)/sessions(会话统计)/redis(Redis状态)") String type) {

        log.info("[Tool] getSystemStatus 被调用: type={}", type);

        // 权限校验
        if (!toolContextProvider.isAdmin()) {
            return "权限不足：仅管理员可查询系统状态。";
        }

        String effectiveType = (type != null) ? type.toLowerCase() : "overview";

        try {
            return switch (effectiveType) {
                case "users" -> getUserStats();
                case "sessions" -> getSessionStats();
                case "redis" -> getRedisStats();
                default -> getOverview();
            };
        } catch (Exception e) {
            log.error("[Tool] getSystemStatus 执行失败: {}", e.getMessage(), e);
            return "查询系统状态失败: " + e.getMessage();
        }
    }

    /**
     * 系统概览
     */
    private String getOverview() {
        long userCount = userRepository.countAll();
        long sessionCount = sessionRepository.countAll();
        long messageCount = messageRepository.countAll();

        StringBuilder sb = new StringBuilder();
        sb.append("## 系统状态概览\n\n");
        sb.append("- 注册用户总数: ").append(userCount).append("\n");
        sb.append("- 会话总数: ").append(sessionCount).append("\n");
        sb.append("- 消息总数: ").append(messageCount).append("\n");
        sb.append("- Redis 状态: ").append(getRedisHealth()).append("\n");
        return sb.toString();
    }

    /**
     * 用户统计
     */
    private String getUserStats() {
        long totalUsers = userRepository.countAll();
        return "## 用户统计\n\n- 注册用户总数: " + totalUsers + "\n";
    }

    /**
     * 会话统计
     */
    private String getSessionStats() {
        long totalSessions = sessionRepository.countAll();
        return "## 会话统计\n\n- 会话总数: " + totalSessions + "\n";
    }

    /**
     * Redis 状态
     */
    private String getRedisStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Redis 状态\n\n");
        sb.append("- 连接状态: ").append(getRedisHealth()).append("\n");

        try {
            String dbSize = redisTemplate.getConnectionFactory().getConnection().info("keyspace") != null
                    ? "正常" : "异常";
            sb.append("- 数据库: ").append(dbSize).append("\n");
        } catch (Exception e) {
            sb.append("- 数据库查询: 异常 (").append(e.getMessage()).append(")\n");
        }

        return sb.toString();
    }

    /**
     * Redis 健康检查
     */
    private String getRedisHealth() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(pong) ? "✅ 正常" : "❌ 异常";
        } catch (Exception e) {
            return "❌ 连接失败: " + e.getMessage();
        }
    }

}
