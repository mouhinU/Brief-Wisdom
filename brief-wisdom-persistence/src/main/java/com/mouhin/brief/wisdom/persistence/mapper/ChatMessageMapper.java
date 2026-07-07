package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    // 获取指定会话的消息数量（未删除的）
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0")
    long countBySessionId(String sessionId);

    // 获取会话的最后一条消息时间
    @Select("SELECT MAX(timestamp) FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0")
    java.time.LocalDateTime selectLastMessageTime(String sessionId);

    // 批量获取指定用户所有会话的消息数量（用于同步状态检测）
    @Select("SELECT session_id, COUNT(*) as cnt FROM chat_message WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY session_id")
    List<Map<String, Object>> selectMessageCountsByUserId(String userId);

    // 批量获取指定用户所有会话的最后消息时间（用于同步状态检测）
    @Select("SELECT session_id, MAX(timestamp) as last_time FROM chat_message WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY session_id")
    List<Map<String, Object>> selectLastMessageTimesByUserId(String userId);

    // 批量获取多个会话的最后消息时间
    @Select("<script>" +
            "SELECT session_id, MAX(timestamp) as last_time FROM chat_message " +
            "WHERE is_deleted = 0 AND session_id IN " +
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY session_id" +
            "</script>")
    List<Map<String, Object>> selectLastMessageTimesBySessionIds(@Param("sessionIds") List<String> sessionIds);

    // ========== 费用统计聚合查询 ==========

    // 总体统计：总费用、总token数、总消息数
    @Select("SELECT COALESCE(SUM(cost), 0) as totalCost, COALESCE(SUM(tokens), 0) as totalTokens, COUNT(*) as totalMessages FROM chat_message WHERE is_deleted = 0 AND role = 'assistant' AND cost > 0")
    Map<String, Object> selectOverallStats();

    // 按模型分组统计
    @Select("SELECT model, COUNT(*) as messageCount, COALESCE(SUM(cost), 0) as totalCost, COALESCE(SUM(tokens), 0) as totalTokens FROM chat_message WHERE is_deleted = 0 AND role = 'assistant' AND cost > 0 GROUP BY model ORDER BY totalCost DESC")
    List<Map<String, Object>> selectCostByModel();

    // 按用户分组统计
    @Select("SELECT user_id as userId, COUNT(*) as messageCount, COALESCE(SUM(cost), 0) as totalCost, COALESCE(SUM(tokens), 0) as totalTokens FROM chat_message WHERE is_deleted = 0 AND role = 'assistant' AND cost > 0 GROUP BY user_id ORDER BY totalCost DESC")
    List<Map<String, Object>> selectCostByUser();

    // 按日期分组统计（最近N天）
    @Select("SELECT DATE(timestamp) as date, COUNT(*) as messageCount, COALESCE(SUM(cost), 0) as totalCost, COALESCE(SUM(tokens), 0) as totalTokens FROM chat_message WHERE is_deleted = 0 AND role = 'assistant' AND cost > 0 AND timestamp >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(timestamp) ORDER BY date")
    List<Map<String, Object>> selectCostByDate(int days);

    // 按模型+日期分组统计（最近N天）
    @Select("SELECT DATE(timestamp) as date, model, COUNT(*) as messageCount, COALESCE(SUM(cost), 0) as totalCost FROM chat_message WHERE is_deleted = 0 AND role = 'assistant' AND cost > 0 AND timestamp >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(timestamp), model ORDER BY date")
    List<Map<String, Object>> selectCostByDateAndModel(int days);
}
