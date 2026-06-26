package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.web.service.AiModelService;
import lombok.Data;
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
public class AiModelController {

    private final AiModelService aiModelService;

    /**
     * 获取所有模型列表（管理页面用）
     */
    @GetMapping
    public ApiResponse listModels() {
        try {
            return ApiResponse.success(aiModelService.listModels());
        } catch (Exception e) {
            log.error("获取模型列表失败: ", e);
            return ApiResponse.error("获取模型列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取启用的模型列表（聊天页面选择器用）
     */
    @GetMapping("/enabled")
    public ApiResponse listEnabledModels() {
        try {
            return ApiResponse.success(aiModelService.listEnabledModels());
        } catch (Exception e) {
            log.error("获取启用模型列表失败: ", e);
            return ApiResponse.error("获取启用模型列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前激活的模型
     */
    @GetMapping("/active")
    public ApiResponse getActiveModel() {
        try {
            AiModel model = aiModelService.getActiveModel();
            return ApiResponse.success(model);
        } catch (Exception e) {
            log.error("获取激活模型失败: ", e);
            return ApiResponse.error("获取激活模型失败: " + e.getMessage());
        }
    }

    /**
     * 切换激活模型
     */
    @PutMapping("/activate/{id}")
    public ApiResponse activateModel(@PathVariable Long id) {
        try {
            aiModelService.activateModel(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("切换模型失败: ", e);
            return ApiResponse.error("切换模型失败: " + e.getMessage());
        }
    }

    /**
     * 新增模型
     */
    @PostMapping
    public ApiResponse createModel(@RequestBody AiModel model) {
        try {
            aiModelService.createModel(model);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("新增模型失败: ", e);
            return ApiResponse.error("新增模型失败: " + e.getMessage());
        }
    }

    /**
     * 更新模型
     */
    @PutMapping
    public ApiResponse updateModel(@RequestBody AiModel model) {
        try {
            aiModelService.updateModel(model);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新模型失败: ", e);
            return ApiResponse.error("更新模型失败: " + e.getMessage());
        }
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public ApiResponse deleteModel(@PathVariable Long id) {
        try {
            aiModelService.deleteModel(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除模型失败: ", e);
            return ApiResponse.error("删除模型失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用模型
     */
    @PutMapping("/{id}/toggle")
    public ApiResponse toggleModel(@PathVariable Long id, @RequestParam boolean enabled) {
        try {
            aiModelService.toggleModelEnabled(id, enabled);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("切换模型状态失败: ", e);
            return ApiResponse.error("切换模型状态失败: " + e.getMessage());
        }
    }

    // ===== 响应 DTO =====

    @Data
    public static class ApiResponse {
        private boolean success;
        private Object data;
        private String error;

        public static ApiResponse success(Object data) {
            ApiResponse r = new ApiResponse();
            r.setSuccess(true);
            r.setData(data);
            return r;
        }

        public static ApiResponse error(String error) {
            ApiResponse r = new ApiResponse();
            r.setSuccess(false);
            r.setError(error);
            return r;
        }
    }
}
