package com.mouhin.brief.wisdom.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.AiModelMapper;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI模型配置管理服务
 */
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelMapper aiModelMapper;

    /**
     * 获取所有模型列表
     */
    public List<AiModel> listModels() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    /**
     * 获取所有启用的模型
     */
    public List<AiModel> listEnabledModels() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsEnabled, 1)
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    /**
     * 获取当前激活的模型
     */
    public AiModel getActiveModel() {
        return aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsActive, 1)
                        .eq(AiModel::getIsEnabled, 1)
        );
    }

    /**
     * 获取当前激活模型的名称（model_name）
     */
    public String getActiveModelName() {
        AiModel model = getActiveModel();
        return model != null ? model.getModelName() : "qwen-plus";
    }

    /**
     * 切换激活模型
     */
    @Transactional
    public void activateModel(Long modelId) {
        // 先取消所有模型的激活状态
        aiModelMapper.deactivateAll();
        // 激活指定模型
        AiModel model = aiModelMapper.selectById(modelId);
        if (model != null) {
            model.setIsActive(1);
            aiModelMapper.updateById(model);
        }
    }

    /**
     * 新增模型
     */
    public void createModel(AiModel model) {
        aiModelMapper.insert(model);
    }

    /**
     * 更新模型
     */
    public void updateModel(AiModel model) {
        aiModelMapper.updateById(model);
    }

    /**
     * 删除模型（逻辑删除）
     */
    public void deleteModel(Long id) {
        aiModelMapper.deleteById(id);
    }

    /**
     * 启用/禁用模型
     */
    public void toggleModelEnabled(Long id, boolean enabled) {
        AiModel model = aiModelMapper.selectById(id);
        if (model != null) {
            model.setIsEnabled(enabled ? 1 : 0);
            // 如果禁用的是当前激活模型，同时取消激活
            if (!enabled && model.getIsActive() == 1) {
                model.setIsActive(0);
            }
            aiModelMapper.updateById(model);
        }
    }
}
