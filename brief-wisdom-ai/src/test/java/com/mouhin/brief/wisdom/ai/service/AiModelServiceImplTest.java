package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.ai.service.impl.AiModelServiceImpl;
import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.persistence.repository.AiModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiModelServiceImpl AI模型管理服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelServiceImpl AI模型管理服务测试")
class AiModelServiceImplTest {

    @Mock
    private AiModelRepository aiModelRepository;

    @InjectMocks
    private AiModelServiceImpl aiModelService;

    @Test
    @DisplayName("listModels 返回所有模型列表")
    void testListModels() {
        AiModel model = buildModel(1L, "qwen-plus", "通义千问", true, true);
        when(aiModelRepository.findAllOrderBySortOrderAsc()).thenReturn(List.of(model));

        List<AiModelDTO> result = aiModelService.listModels();
        assertEquals(1, result.size());
        assertEquals("qwen-plus", result.get(0).getModelName());
    }

    @Test
    @DisplayName("listEnabledModels 仅返回启用模型")
    void testListEnabledModels() {
        AiModel enabled = buildModel(1L, "qwen-plus", "通义千问", true, true);
        when(aiModelRepository.findByEnabledOrderBySortOrderAsc()).thenReturn(List.of(enabled));

        List<AiModelDTO> result = aiModelService.listEnabledModels();
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsEnabled() == 1);
    }

    @Test
    @DisplayName("getActiveModel 返回激活模型")
    void testGetActiveModel() {
        AiModel active = buildModel(1L, "qwen-plus", "通义千问", true, true);
        when(aiModelRepository.findActiveModel()).thenReturn(active);

        AiModelDTO result = aiModelService.getActiveModel();
        assertNotNull(result);
        assertEquals("qwen-plus", result.getModelName());
    }

    @Test
    @DisplayName("getActiveModel 无激活模型时返回 null")
    void testGetActiveModel_none() {
        when(aiModelRepository.findActiveModel()).thenReturn(null);
        assertNull(aiModelService.getActiveModel());
    }

    @Test
    @DisplayName("getActiveModelName 默认返回 qwen-plus")
    void testGetActiveModelName_default() {
        when(aiModelRepository.findActiveModel()).thenReturn(null);
        assertEquals("qwen-plus", aiModelService.getActiveModelName());
    }

    @Test
    @DisplayName("activateModel 应取消所有模型激活并激活目标模型")
    void testActivateModel() {
        AiModel model = buildModel(1L, "gpt-4", "GPT-4", true, true);
        when(aiModelRepository.findById(1L)).thenReturn(model);

        aiModelService.activateModel(1L);

        verify(aiModelRepository).deactivateAll();
        verify(aiModelRepository).update(argThat(m -> m.getIsActive() == 1));
    }

    @Test
    @DisplayName("createModel 应调用 repository.save")
    void testCreateModel() {
        AiModel model = new AiModel();
        model.setModelName("test-model");

        aiModelService.createModel(model);
        verify(aiModelRepository).save(model);
    }

    @Test
    @DisplayName("deleteModel 应调用 repository.deleteById")
    void testDeleteModel() {
        aiModelService.deleteModel(1L);
        verify(aiModelRepository).deleteById(1L);
    }

    @Test
    @DisplayName("toggleModelEnabled 禁用模型同时取消激活")
    void testToggleModelEnabled_disableActive() {
        AiModel model = buildModel(1L, "qwen-plus", "通义千问", true, true);
        when(aiModelRepository.findById(1L)).thenReturn(model);

        aiModelService.toggleModelEnabled(1L, false);

        verify(aiModelRepository).update(argThat(m -> m.getIsEnabled() == 0 && m.getIsActive() == 0));
    }

    @Test
    @DisplayName("toggleModelEnabled 启用模型")
    void testToggleModelEnabled_enable() {
        AiModel model = buildModel(1L, "qwen-plus", "通义千问", false, false);
        when(aiModelRepository.findById(1L)).thenReturn(model);

        aiModelService.toggleModelEnabled(1L, true);

        verify(aiModelRepository).update(argThat(m -> m.getIsEnabled() == 1));
    }

    private AiModel buildModel(Long id, String modelName, String displayName, boolean active, boolean enabled) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setModelName(modelName);
        model.setDisplayName(displayName);
        model.setProvider("dashscope");
        model.setIsActive(active ? 1 : 0);
        model.setIsEnabled(enabled ? 1 : 0);
        model.setSortOrder(1);
        return model;
    }
}
