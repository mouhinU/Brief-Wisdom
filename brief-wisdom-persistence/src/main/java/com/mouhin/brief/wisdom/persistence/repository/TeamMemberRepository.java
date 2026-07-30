package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.TeamMemberMapper;
import com.mouhin.brief.wisdom.persistence.model.TeamMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 团队成员 Repository
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Repository
@RequiredArgsConstructor
public class TeamMemberRepository {

    private final TeamMemberMapper teamMemberMapper;

    public void save(TeamMember member) {
        teamMemberMapper.insert(member);
    }

    public TeamMember findById(Long id) {
        return teamMemberMapper.selectById(id);
    }

    public TeamMember findByTeamIdAndUserId(Long teamId, String userId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId);
        return teamMemberMapper.selectOne(wrapper);
    }

    public List<TeamMember> findByTeamId(Long teamId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId)
                .orderByAsc(TeamMember::getCreateTime);
        return teamMemberMapper.selectList(wrapper);
    }

    public List<TeamMember> findByUserId(String userId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getUserId, userId)
                .orderByDesc(TeamMember::getCreateTime);
        return teamMemberMapper.selectList(wrapper);
    }

    public void update(TeamMember member) {
        teamMemberMapper.updateById(member);
    }

    public void deleteById(Long id) {
        teamMemberMapper.deleteById(id);
    }

    public void deleteByTeamId(Long teamId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId);
        teamMemberMapper.delete(wrapper);
    }

    public long countByTeamId(Long teamId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId);
        return teamMemberMapper.selectCount(wrapper);
    }
}
