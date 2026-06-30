package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
