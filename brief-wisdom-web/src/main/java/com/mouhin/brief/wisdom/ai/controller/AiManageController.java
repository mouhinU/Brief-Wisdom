package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.web.service.AiManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI助手管理 REST 接口 - 按用户级别查询会话历史
 */
/**
 * AiManageController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/ai/manage")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@RequiresPermission("ai:manage")
public class AiManageController {

    private final AiManageService aiManageService;

    /**
     * 获取用户列表（支持按级别筛选）
     */
    @GetMapping("/users")
    public List<UserDTO> listUsers(@RequestParam(value = "level", required = false) String level) {
        return (level != null && !level.isEmpty())
                ? aiManageService.listUsersByLevel(level)
                : aiManageService.listUsers();
    }

    /**
     * 获取所有用户级别
     */
    @GetMapping("/levels")
    public List<String> listLevels() {
        return aiManageService.listUserLevels();
    }

    /**
     * 按用户ID查询会话列表
     */
    @GetMapping("/sessions/user/{userId}")
    public List<SessionDTO> listSessionsByUser(@PathVariable String userId) {
        return aiManageService.listSessionsByUserId(userId);
    }

    /**
     * 按用户级别查询会话列表
     */
    @GetMapping("/sessions/level/{level}")
    public List<SessionDTO> listSessionsByLevel(@PathVariable String level) {
        return aiManageService.listSessionsByUserLevel(level);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/session/{sessionId}/messages")
    public List<MessageDTO> getSessionMessages(@PathVariable String sessionId) {
        return aiManageService.getSessionMessages(sessionId);
    }
}
