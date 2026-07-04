package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMemory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话记忆 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemory> {
}
