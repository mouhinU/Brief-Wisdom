package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * AiModelMapper 接口
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */

@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {

    /**
     * 将所有模型的 is_active 设为 0
     */
    @Update("UPDATE ai_model SET is_active = 0 WHERE is_deleted = 0")
    int deactivateAll();
}
