package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.TeamMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队成员 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}
