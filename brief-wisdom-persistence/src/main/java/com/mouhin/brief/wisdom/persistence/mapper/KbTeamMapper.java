package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.KbTeam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库-团队关联 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-07-26
 */
@Mapper
public interface KbTeamMapper extends BaseMapper<KbTeam> {
}
