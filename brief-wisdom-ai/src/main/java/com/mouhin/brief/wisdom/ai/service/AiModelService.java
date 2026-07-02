package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.persistence.model.AiModel;

import java.util.List;

/**
 * AI模型配置管理服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface AiModelService {

    /**
     * 获取所有模型列表
     */
    List<AiModelDTO> listModels();

    /**
     * 获取所有启用的模型
     */
    List<AiModelDTO> listEnabledModels();

    /**
     * 获取当前激活的模型
     */
    AiModelDTO getActiveModel();

    /**
     * 获取当前激活模型的名称（model_name）
     */
    String getActiveModelName();

    /**
     * 切换激活模型
     */
    void activateModel(Long modelId);

    /**
     * 新增模型
     */
    void createModel(AiModel model);

    /**
     * 更新模型
     */
    void updateModel(AiModel model);

    /**
     * 删除模型（逻辑删除）
     */
    void deleteModel(Long id);

    /**
     * 启用/禁用模型
     */
    void toggleModelEnabled(Long id, boolean enabled);
}
