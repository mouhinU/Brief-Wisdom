package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.AiModelMapper;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI 模型数据访问层
 */
/**
 * AiModelRepository
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class AiModelRepository {

    private final AiModelMapper aiModelMapper;

    public List<AiModel> findAll() {
        return aiModelMapper.selectList(null);
    }

    public List<AiModel> findByEnabledOrderBySortOrderAsc() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsEnabled, 1)
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    public List<AiModel> findAllOrderBySortOrderAsc() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    public AiModel findActiveModel() {
        return aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsActive, 1)
                        .eq(AiModel::getIsEnabled, 1)
        );
    }

    public AiModel findByModelName(String modelName) {
        return aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelName, modelName)
        );
    }

    public AiModel findById(Long id) {
        return aiModelMapper.selectById(id);
    }

    public void save(AiModel model) {
        aiModelMapper.insert(model);
    }

    public void update(AiModel model) {
        aiModelMapper.updateById(model);
    }

    public void deleteById(Long id) {
        aiModelMapper.deleteById(id);
    }

    public void deactivateAll() {
        aiModelMapper.deactivateAll();
    }
}
