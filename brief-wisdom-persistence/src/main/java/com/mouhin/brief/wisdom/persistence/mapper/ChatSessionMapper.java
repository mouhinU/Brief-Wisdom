package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    
    // 按更新时间倒序获取所有会话（未删除的）
    @Select("SELECT * FROM chat_session WHERE is_deleted = 0 ORDER BY update_time DESC")
    List<ChatSession> selectAllOrderByUpdateTimeDesc();
    
    // 获取指定用户的会话列表（未删除的）
    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY update_time DESC")
    List<ChatSession> selectByUserIdOrderByUpdateTimeDesc(String userId);
    
    // 统计用户的会话数量（未删除的）
    @Select("SELECT COUNT(*) FROM chat_session WHERE user_id = #{userId} AND is_deleted = 0")
    long countByUserId(String userId);
}
