package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.KbShareMapper;
import com.mouhin.brief.wisdom.persistence.model.KbShare;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库共享授权 Repository
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Repository
@RequiredArgsConstructor
public class KbShareRepository {

    private final KbShareMapper kbShareMapper;

    public void save(KbShare share) {
        kbShareMapper.insert(share);
    }

    public KbShare findById(Long id) {
        return kbShareMapper.selectById(id);
    }

    public List<KbShare> findByKbId(Long kbId) {
        LambdaQueryWrapper<KbShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbShare::getKbId, kbId)
                .orderByDesc(KbShare::getCreateTime);
        return kbShareMapper.selectList(wrapper);
    }

    public List<KbShare> findByTarget(String targetType, String targetId) {
        LambdaQueryWrapper<KbShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbShare::getTargetType, targetType)
                .eq(KbShare::getTargetId, targetId);
        return kbShareMapper.selectList(wrapper);
    }

    public KbShare findByKbIdAndTarget(Long kbId, String targetType, String targetId) {
        LambdaQueryWrapper<KbShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbShare::getKbId, kbId)
                .eq(KbShare::getTargetType, targetType)
                .eq(KbShare::getTargetId, targetId);
        return kbShareMapper.selectOne(wrapper);
    }

    public void deleteById(Long id) {
        kbShareMapper.deleteById(id);
    }

    public void deleteByKbId(Long kbId) {
        LambdaQueryWrapper<KbShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbShare::getKbId, kbId);
        kbShareMapper.delete(wrapper);
    }
}
