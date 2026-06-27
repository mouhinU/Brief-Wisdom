package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.web.service.AiManageService;
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
    public ApiResponse<List<UserDTO>> listUsers(@RequestParam(value = "level", required = false) String level) {
        try {
            var users = (level != null && !level.isEmpty())
                    ? aiManageService.listUsersByLevel(level)
                    : aiManageService.listUsers();
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败: ", e);
            return ApiResponse.fail("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户级别
     */
    @GetMapping("/levels")
    public ApiResponse<List<String>> listLevels() {
        try {
            return ApiResponse.success(aiManageService.listUserLevels());
        } catch (Exception e) {
            log.error("获取用户级别列表失败: ", e);
            return ApiResponse.fail("获取用户级别列表失败: " + e.getMessage());
        }
    }

    /**
     * 按用户ID查询会话列表
     */
    @GetMapping("/sessions/user/{userId}")
    public ApiResponse<List<SessionDTO>> listSessionsByUser(@PathVariable String userId) {
        try {
            return ApiResponse.success(aiManageService.listSessionsByUserId(userId));
        } catch (Exception e) {
            log.error("获取用户会话列表失败: ", e);
            return ApiResponse.fail("获取用户会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 按用户级别查询会话列表
     */
    @GetMapping("/sessions/level/{level}")
    public ApiResponse<List<SessionDTO>> listSessionsByLevel(@PathVariable String level) {
        try {
            return ApiResponse.success(aiManageService.listSessionsByUserLevel(level));
        } catch (Exception e) {
            log.error("按级别获取会话列表失败: ", e);
            return ApiResponse.fail("按级别获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<MessageDTO>> getSessionMessages(@PathVariable String sessionId) {
        try {
            return ApiResponse.success(aiManageService.getSessionMessages(sessionId));
        } catch (Exception e) {
            log.error("获取会话消息失败: ", e);
            return ApiResponse.fail("获取会话消息失败: " + e.getMessage());
        }
    }
}
