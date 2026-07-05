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
     *
     * @return 模型 DTO 列表
     */
    List<AiModelDTO> listModels();

    /**
     * 获取所有启用的模型
     *
     * @return 启用的模型 DTO 列表
     */
    List<AiModelDTO> listEnabledModels();

    /**
     * 获取当前激活的模型
     *
     * @return 激活的模型 DTO，无激活模型返回 null
     */
    AiModelDTO getActiveModel();

    /**
     * 获取当前激活模型的名称（model_name）
     *
     * @return 模型名称，无激活模型返回默认值
     */
    String getActiveModelName();

    /**
     * 切换激活模型
     *
     * @param modelId 要激活的模型 ID
     */
    void activateModel(Long modelId);

    /**
     * 新增模型
     *
     * @param model 模型实体
     */
    void createModel(AiModel model);

    /**
     * 更新模型
     *
     * @param model 模型实体
     */
    void updateModel(AiModel model);

    /**
     * 删除模型（逻辑删除）
     *
     * @param id 模型 ID
     */
    void deleteModel(Long id);

    /**
     * 启用/禁用模型
     *
     * @param id      模型 ID
     * @param enabled true 启用，false 禁用
     */
    void toggleModelEnabled(Long id, boolean enabled);
}
