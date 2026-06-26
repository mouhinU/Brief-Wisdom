package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.web.service.AiManageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI助手管理 REST 接口 - 按用户级别查询会话历史
 */
@RestController
@RequestMapping("/api/ai/manage")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiManageController {

    private final AiManageService aiManageService;

    /**
     * 获取用户列表（支持按级别筛选）
     */
    @GetMapping("/users")
    public ApiResponse listUsers(@RequestParam(value = "level", required = false) String level) {
        try {
            var users = (level != null && !level.isEmpty())
                    ? aiManageService.listUsersByLevel(level)
                    : aiManageService.listUsers();
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败: ", e);
            return ApiResponse.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户级别
     */
    @GetMapping("/levels")
    public ApiResponse listLevels() {
        try {
            return ApiResponse.success(aiManageService.listUserLevels());
        } catch (Exception e) {
            log.error("获取用户级别列表失败: ", e);
            return ApiResponse.error("获取用户级别列表失败: " + e.getMessage());
        }
    }

    /**
     * 按用户ID查询会话列表
     */
    @GetMapping("/sessions/user/{userId}")
    public ApiResponse listSessionsByUser(@PathVariable String userId) {
        try {
            return ApiResponse.success(aiManageService.listSessionsByUserId(userId));
        } catch (Exception e) {
            log.error("获取用户会话列表失败: ", e);
            return ApiResponse.error("获取用户会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 按用户级别查询会话列表
     */
    @GetMapping("/sessions/level/{level}")
    public ApiResponse listSessionsByLevel(@PathVariable String level) {
        try {
            return ApiResponse.success(aiManageService.listSessionsByUserLevel(level));
        } catch (Exception e) {
            log.error("按级别获取会话列表失败: ", e);
            return ApiResponse.error("按级别获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse getSessionMessages(@PathVariable String sessionId) {
        try {
            return ApiResponse.success(aiManageService.getSessionMessages(sessionId));
        } catch (Exception e) {
            log.error("获取会话消息失败: ", e);
            return ApiResponse.error("获取会话消息失败: " + e.getMessage());
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
