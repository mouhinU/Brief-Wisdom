package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    // 按时间顺序获取会话的所有消息（未删除的）
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0 ORDER BY timestamp ASC")
    List<ChatMessage> selectBySessionIdOrderByTimestampAsc(String sessionId);
    
    // 获取指定用户的消息数量（未删除的）
    @Select("SELECT COUNT(*) FROM chat_message WHERE user_id = #{userId} AND is_deleted = 0")
    long countByUserId(String userId);
    
    // 获取指定会话的消息数量（未删除的）
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0")
    long countBySessionId(String sessionId);
    
    // 获取最近N条消息（用于上下文，未删除的）
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0 ORDER BY timestamp DESC LIMIT #{limit}")
    List<ChatMessage> selectRecentMessages(String sessionId, int limit);
    
    // 获取会话的最后一条消息时间
    @Select("SELECT MAX(timestamp) FROM chat_message WHERE session_id = #{sessionId} AND is_deleted = 0")
    java.time.LocalDateTime selectLastMessageTime(String sessionId);
}
