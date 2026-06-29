package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.web.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型管理 REST 接口
 */
@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@RequiresPermission("ai:manage")
public class AiModelController {

    private final AiModelService aiModelService;

    /**
     * 获取所有模型列表（管理页面用）
     */
    @GetMapping
    public List<AiModelDTO> listModels() {
        return aiModelService.listModels();
    }

    /**
     * 获取启用的模型列表（聊天页面选择器用）
     */
    @GetMapping("/enabled")
    public List<AiModelDTO> listEnabledModels() {
        return aiModelService.listEnabledModels();
    }

    /**
     * 获取当前激活的模型
     */
    @GetMapping("/active")
    public AiModelDTO getActiveModel() {
        return aiModelService.getActiveModel();
    }

    /**
     * 切换激活模型
     */
    @PutMapping("/activate/{id}")
    public Boolean activateModel(@PathVariable Long id) {
        aiModelService.activateModel(id);
        return true;
    }

    /**
     * 新增模型
     */
    @PostMapping
    public Boolean createModel(@RequestBody AiModel model) {
        aiModelService.createModel(model);
        return true;
    }

    /**
     * 更新模型
     */
    @PutMapping
    public Boolean updateModel(@RequestBody AiModel model) {
        aiModelService.updateModel(model);
        return true;
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public Boolean deleteModel(@PathVariable Long id) {
        aiModelService.deleteModel(id);
        return true;
    }

    /**
     * 启用/禁用模型
     */
    @PutMapping("/{id}/toggle")
    public Boolean toggleModel(@PathVariable Long id, @RequestParam boolean enabled) {
        aiModelService.toggleModelEnabled(id, enabled);
        return true;
    }
}
