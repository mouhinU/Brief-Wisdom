package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.ai.ChatMessageDTO;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.ai.SessionMetaDTO;
import com.mouhin.brief.wisdom.config.PaginationProperties;
import com.mouhin.brief.wisdom.web.service.UserContextHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final PaginationProperties paginationProperties;
    private final UserContextHelper userContextHelper;

    /**
     * 简单聊天接口（无上下文）
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        try {
            String response = aiAgentService.chat(request.getMessage());
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.fail("AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 带上下文的聊天接口
     */
    @PostMapping("/chat/session/{sessionId}")
    public ApiResponse<String> chatWithSession(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        String userId = userContextHelper.getCurrentUserId();
        log.info("收到聊天请求 - sessionId: {}, userId: {}, message: {}, model: {}", sessionId, userId, request.getMessage(), request.getModel());
        try {
            String response = aiAgentService.chatWithSession(sessionId, userId, request.getMessage(), request.getModel());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("聊天失败: ", e);
            return ApiResponse.fail("AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public ApiResponse<String> createSession() {
        try {
            String userId = userContextHelper.getCurrentUserId();
            String sessionId = aiAgentService.createSession(userId);
            return ApiResponse.success(sessionId);
        } catch (Exception e) {
            return ApiResponse.fail("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        try {
            aiAgentService.deleteSession(sessionId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.fail("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户的会话列表（支持分页）
     * <p>
     * 默认每页条数和最大值由 application.yml 中 app.pagination.session-list 配置
     *
     * @param page   当前页码，从 1 开始，默认 1
     * @param size   每页大小，不传则使用配置的默认值，超过配置的最大值会被截断
     */
    @GetMapping("/sessions")
    public ApiResponse<PageResult<SessionMetaDTO>> listSessions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            String userId = userContextHelper.getCurrentUserId();
            PaginationProperties.PageConfig config = paginationProperties.getSessionList();
            int resolvedSize = config.resolveSize(size);
            var result = aiAgentService.listSessionsPaged(userId, page, resolvedSize);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.fail("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/session/{sessionId}/history")
    public ApiResponse<List<ChatMessageDTO>> getSessionHistory(@PathVariable String sessionId) {
        try {
            var history = aiAgentService.getSessionHistory(sessionId);
            return ApiResponse.success(history);
        } catch (Exception e) {
            return ApiResponse.fail("获取历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取分页配置信息
     * <p>
     * 前端可调用此接口获取各业务的默认分页大小，避免硬编码
     */
    @GetMapping("/config/pagination")
    public ApiResponse<Map<String, Object>> getPaginationConfig() {
        try {
            Map<String, Object> config = Map.of(
                "sessionList", Map.of(
                    "defaultSize", paginationProperties.getSessionList().getDefaultSize(),
                    "maxSize", paginationProperties.getSessionList().getMaxSize()
                ),
                "messageHistory", Map.of(
                    "defaultSize", paginationProperties.getMessageHistory().getDefaultSize(),
                    "maxSize", paginationProperties.getMessageHistory().getMaxSize()
                )
            );
            return ApiResponse.success(config);
        } catch (Exception e) {
            return ApiResponse.fail("获取分页配置失败: " + e.getMessage());
        }
    }

    /**
     * 带系统提示的聊天接口
     */
    @PostMapping("/chat-with-prompt")
    public ApiResponse<String> chatWithPrompt(@RequestBody ChatWithPromptRequest request) {
        try {
            String response = aiAgentService.chatWithSystemPrompt(
                    request.getSystemPrompt(), 
                    request.getUserMessage()
            );
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.fail("AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 智能问答接口
     */
    @PostMapping("/ask")
    public ApiResponse<String> ask(@RequestBody QuestionRequest request) {
        try {
            String response = aiAgentService.askQuestion(request.getQuestion());
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.fail("AI 服务异常: " + e.getMessage());
        }
    }



    // 请求和响应 DTO

    @Data
    public static class ChatRequest {
        private String message;
        private String model;  // 可选：指定使用的模型
    }

    @Data
    public static class ChatWithPromptRequest {
        private String systemPrompt;
        private String userMessage;
    }

    @Data
    public static class QuestionRequest {
        private String question;
    }
}
