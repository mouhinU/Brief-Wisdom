package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.TeamMapper;
import com.mouhin.brief.wisdom.persistence.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 团队 Repository
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Repository
@RequiredArgsConstructor
public class TeamRepository {

    private final TeamMapper teamMapper;

    public void save(Team team) {
        teamMapper.insert(team);
    }

    public Team findById(Long id) {
        return teamMapper.selectById(id);
    }

    public List<Team> findByOwnerId(String ownerId) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getOwnerId, ownerId)
                .orderByDesc(Team::getCreateTime);
        return teamMapper.selectList(wrapper);
    }

    public List<Team> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return teamMapper.selectBatchIds(ids);
    }

    public void update(Team team) {
        teamMapper.updateById(team);
    }

    public void deleteById(Long id) {
        teamMapper.deleteById(id);
    }
}
