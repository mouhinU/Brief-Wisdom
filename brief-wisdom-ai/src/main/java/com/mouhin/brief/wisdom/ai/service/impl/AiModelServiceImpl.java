package com.mouhin.brief.wisdom.ai.service.impl;

import com.mouhin.brief.wisdom.ai.service.AiModelService;
import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.constants.CachePrefix;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.persistence.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI模型配置管理服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    /** 默认模型名称（当无激活模型时使用） */
    private static final String DEFAULT_MODEL_NAME = "qwen-plus";

    private final AiModelRepository aiModelRepository;

    /**
     * 获取所有模型列表
     */
    @Override
    @Cacheable(value = CachePrefix.AI_MODEL_CACHE, key = "'all'")
    public List<AiModelDTO> listModels() {
        return aiModelRepository.findAllOrderBySortOrderAsc().stream().map(this::toDTO).toList();
    }

    /**
     * 获取所有启用的模型
     */
    @Override
    @Cacheable(value = CachePrefix.AI_MODEL_CACHE, key = "'enabled'")
    public List<AiModelDTO> listEnabledModels() {
        return aiModelRepository.findByEnabledOrderBySortOrderAsc().stream().map(this::toDTO).toList();
    }

    /**
     * 获取当前激活的模型
     */
    @Override
    @Cacheable(value = CachePrefix.AI_MODEL_CACHE, key = "'active'", unless = "#result == null")
    public AiModelDTO getActiveModel() {
        AiModel model = aiModelRepository.findActiveModel();
        return model != null ? toDTO(model) : null;
    }

    /**
     * 获取当前激活模型的名称（model_name）
     */
    @Override
    public String getActiveModelName() {
        AiModelDTO model = getActiveModel();
        return model != null ? model.getModelName() : DEFAULT_MODEL_NAME;
    }

    /**
     * 切换激活模型
     */
    @Override
    @CacheEvict(value = CachePrefix.AI_MODEL_CACHE, allEntries = true)
    @Transactional
    public void activateModel(Long modelId) {
        // 先取消所有模型的激活状态
        aiModelRepository.deactivateAll();
        // 激活指定模型
        AiModel model = aiModelRepository.findById(modelId);
        if (model != null) {
            model.setIsActive(1);
            aiModelRepository.update(model);
        }
    }

    /**
     * 新增模型
     */
    @Override
    @CacheEvict(value = CachePrefix.AI_MODEL_CACHE, allEntries = true)
    public void createModel(AiModel model) {
        aiModelRepository.save(model);
    }

    /**
     * 更新模型
     */
    @Override
    @CacheEvict(value = CachePrefix.AI_MODEL_CACHE, allEntries = true)
    public void updateModel(AiModel model) {
        aiModelRepository.update(model);
    }

    /**
     * 删除模型（逻辑删除）
     */
    @Override
    @CacheEvict(value = CachePrefix.AI_MODEL_CACHE, allEntries = true)
    public void deleteModel(Long id) {
        aiModelRepository.deleteById(id);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    @CacheEvict(value = CachePrefix.AI_MODEL_CACHE, allEntries = true)
    public void toggleModelEnabled(Long id, boolean enabled) {
        AiModel model = aiModelRepository.findById(id);
        if (model != null) {
            model.setIsEnabled(enabled ? 1 : 0);
            // 如果禁用的是当前激活模型，同时取消激活
            if (!enabled && model.getIsActive() == 1) {
                model.setIsActive(0);
            }
            aiModelRepository.update(model);
        }
    }

    private AiModelDTO toDTO(AiModel m) {
        AiModelDTO dto = new AiModelDTO();
        dto.setId(m.getId());
        dto.setModelName(m.getModelName());
        dto.setDisplayName(m.getDisplayName());
        dto.setProvider(m.getProvider());
        dto.setDescription(m.getDescription());
        dto.setIsActive(m.getIsActive());
        dto.setIsEnabled(m.getIsEnabled());
        dto.setSortOrder(m.getSortOrder());
        dto.setInputPricePerMillion(m.getInputPricePerMillion());
        dto.setOutputPricePerMillion(m.getOutputPricePerMillion());
        dto.setThinkingMode(m.getThinkingMode());
        dto.setCreateTime(m.getCreateTime());
        dto.setUpdateTime(m.getUpdateTime());
        return dto;
    }
}
