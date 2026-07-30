package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.KbTeamMapper;
import com.mouhin.brief.wisdom.persistence.model.KbTeam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库-团队关联 Repository
 *
 * @author Brief-Wisdom
 * @date 2026-07-26
 */
@Repository
@RequiredArgsConstructor
public class KbTeamRepository {

    private final KbTeamMapper kbTeamMapper;

    /**
     * 保存单条关联
     */
    public void save(KbTeam kbTeam) {
        kbTeamMapper.insert(kbTeam);
    }

    /**
     * 查询知识库关联的所有团队记录
     */
    public List<KbTeam> findByKbId(Long kbId) {
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbTeam::getKbId, kbId);
        return kbTeamMapper.selectList(wrapper);
    }

    /**
     * 查询团队关联的所有知识库记录
     */
    public List<KbTeam> findByTeamId(Long teamId) {
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbTeam::getTeamId, teamId);
        return kbTeamMapper.selectList(wrapper);
    }

    /**
     * 批量查询多个知识库的团队关联记录（用于列表填充 teamIds，避免 N+1）
     */
    public List<KbTeam> findByKbIds(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KbTeam::getKbId, kbIds);
        return kbTeamMapper.selectList(wrapper);
    }

    /**
     * 查询关联到任意指定团队的知识库 ID 列表（用于可见性过滤）
     */
    public List<Long> findKbIdsByTeamIds(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KbTeam::getTeamId, teamIds);
        wrapper.select(KbTeam::getKbId);
        return kbTeamMapper.selectList(wrapper).stream()
                .map(KbTeam::getKbId)
                .distinct()
                .toList();
    }

    /**
     * 判断知识库是否关联指定团队
     */
    public boolean existsByKbIdAndTeamId(Long kbId, Long teamId) {
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbTeam::getKbId, kbId)
                .eq(KbTeam::getTeamId, teamId);
        return kbTeamMapper.selectCount(wrapper) > 0;
    }

    /**
     * 删除知识库的全部团队关联（删除知识库时调用）
     */
    public void deleteByKbId(Long kbId) {
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbTeam::getKbId, kbId);
        kbTeamMapper.delete(wrapper);
    }

    /**
     * 删除知识库与指定团队的关联
     */
    public void deleteByKbIdAndTeamId(Long kbId, Long teamId) {
        LambdaQueryWrapper<KbTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbTeam::getKbId, kbId)
                .eq(KbTeam::getTeamId, teamId);
        kbTeamMapper.delete(wrapper);
    }
}
