package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.service.AiManageService;
import com.mouhin.brief.wisdom.common.manage.CostStatisticsDTO;
import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI助手管理 REST 接口 - 按用户级别查询会话历史
 * AiManageController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/ai/manage")
@RequiredArgsConstructor
@Slf4j
@RequiresPermission("ai:manage")
@Tag(name = "AI管理", description = "AI 会话历史管理、费用统计等管理接口")
public class AiManageController {

    private final AiManageService aiManageService;

    /**
     * 获取用户列表（支持按级别筛选）
     */
    @Operation(summary = "获取用户列表", description = "支持按级别筛选")
    @GetMapping("/users")
    public List<UserDTO> listUsers(
            @Parameter(description = "用户级别筛选") @RequestParam(value = "level", required = false) String level) {
        return (level != null && !level.isEmpty())
                ? aiManageService.listUsersByLevel(level)
                : aiManageService.listUsers();
    }

    /**
     * 获取所有用户级别
     */
    @Operation(summary = "获取用户级别列表")
    @GetMapping("/levels")
    public List<String> listLevels() {
        return aiManageService.listUserLevels();
    }

    /**
     * 按用户ID查询会话列表
     */
    @Operation(summary = "按用户ID查询会话")
    @GetMapping("/sessions/user/{userId}")
    public List<SessionDTO> listSessionsByUser(
            @Parameter(description = "用户ID", required = true) @PathVariable String userId) {
        return aiManageService.listSessionsByUserId(userId);
    }

    /**
     * 按用户级别查询会话列表
     */
    @Operation(summary = "按用户级别查询会话")
    @GetMapping("/sessions/level/{level}")
    public List<SessionDTO> listSessionsByLevel(
            @Parameter(description = "用户级别", required = true) @PathVariable String level) {
        return aiManageService.listSessionsByUserLevel(level);
    }

    /**
     * 获取会话消息历史
     */
    @Operation(summary = "获取会话消息历史")
    @GetMapping("/session/{sessionId}/messages")
    public List<MessageDTO> getSessionMessages(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        return aiManageService.getSessionMessages(sessionId);
    }

    /**
     * 获取费用统计数据
     */
    @Operation(summary = "获取费用统计")
    @GetMapping("/cost-statistics")
    public CostStatisticsDTO getCostStatistics(
            @Parameter(description = "统计天数范围，默认30") @RequestParam(value = "days", defaultValue = "30") int days) {
        return aiManageService.getCostStatistics(days);
    }
}
