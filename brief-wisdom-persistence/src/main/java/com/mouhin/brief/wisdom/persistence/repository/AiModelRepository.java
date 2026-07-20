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

    /**
     * 查询所有模型
     *
     * @return 模型列表
     */
    public List<AiModel> findAll() {
        return aiModelMapper.selectList(null);
    }

    /**
     * 查询所有启用的模型（按排序字段升序）
     *
     * @return 启用的模型列表
     */
    public List<AiModel> findByEnabledOrderBySortOrderAsc() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsEnabled, 1)
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    /**
     * 查询所有模型（按排序字段升序）
     *
     * @return 模型列表
     */
    public List<AiModel> findAllOrderBySortOrderAsc() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    /**
     * 查询当前激活且启用的模型
     *
     * @return 激活的模型，不存在返回 null
     */
    public AiModel findActiveModel() {
        return aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsActive, 1)
                        .eq(AiModel::getIsEnabled, 1)
        );
    }

    /**
     * 根据模型名称查询模型
     *
     * @param modelName 模型名称
     * @return 匹配的模型，不存在返回 null
     */
    public AiModel findByModelName(String modelName) {
        return aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelName, modelName)
        );
    }

    /**
     * 根据 ID 查询模型
     *
     * @param id 模型 ID
     * @return 匹配的模型，不存在返回 null
     */
    public AiModel findById(Long id) {
        return aiModelMapper.selectById(id);
    }

    /**
     * 保存新模型
     *
     * @param model 模型实体
     */
    public void save(AiModel model) {
        aiModelMapper.insert(model);
    }

    /**
     * 更新模型
     *
     * @param model 模型实体
     */
    public void update(AiModel model) {
        aiModelMapper.updateById(model);
    }

    /**
     * 根据 ID 删除模型
     *
     * @param id 模型 ID
     */
    public void deleteById(Long id) {
        aiModelMapper.deleteById(id);
    }

    /**
     * 将所有模型的激活状态置为非激活
     */
    public void deactivateAll() {
        aiModelMapper.deactivateAll();
    }
}
