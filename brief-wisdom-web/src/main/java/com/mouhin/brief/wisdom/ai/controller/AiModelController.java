package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.common.ai.AiModelDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.ai.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型管理 REST 接口
 * AiModelController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
@Slf4j
@RequiresPermission("ai:manage")
@Tag(name = "AI管理", description = "AI 模型配置管理接口")
public class AiModelController {

    private final AiModelService aiModelService;

    /**
     * 获取所有模型列表（管理页面用）
     */
    @Operation(summary = "获取所有模型列表", description = "管理页面用，包含已禁用模型")
    @GetMapping
    public List<AiModelDTO> listModels() {
        return aiModelService.listModels();
    }

    /**
     * 获取当前激活的模型
     */
    @Operation(summary = "获取当前激活模型")
    @GetMapping("/active")
    public AiModelDTO getActiveModel() {
        return aiModelService.getActiveModel();
    }

    /**
     * 切换激活模型
     */
    @Operation(summary = "切换激活模型")
    @PutMapping("/activate/{id}")
    public Boolean activateModel(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id) {
        aiModelService.activateModel(id);
        return true;
    }

    /**
     * 新增模型
     */
    @Operation(summary = "新增模型")
    @PostMapping
    public Boolean createModel(@RequestBody AiModel model) {
        aiModelService.createModel(model);
        return true;
    }

    /**
     * 更新模型
     */
    @Operation(summary = "更新模型")
    @PutMapping
    public Boolean updateModel(@RequestBody AiModel model) {
        aiModelService.updateModel(model);
        return true;
    }

    /**
     * 删除模型
     */
    @Operation(summary = "删除模型", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public Boolean deleteModel(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id) {
        aiModelService.deleteModel(id);
        return true;
    }

    /**
     * 启用/禁用模型
     */
    @Operation(summary = "启用/禁用模型")
    @PutMapping("/{id}/toggle")
    public Boolean toggleModel(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        aiModelService.toggleModelEnabled(id, enabled);
        return true;
    }
}
