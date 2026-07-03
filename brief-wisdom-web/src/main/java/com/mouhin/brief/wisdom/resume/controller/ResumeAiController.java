package com.mouhin.brief.wisdom.resume.controller;

import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.resume.req.TextPolishRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 简历AI辅助 REST 接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@RestController
@RequestMapping("/api/resume/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@RequiresPermission("resume:manage")
public class ResumeAiController {

    private final AiAgentService aiAgentService;

    /**
     * AI文本润色
     */
    @PostMapping("/polish")
    public Map<String, String> polishText(@RequestBody TextPolishRequest request) {
        log.info("AI文本润色请求, fieldType={}, textLength={}", request.getFieldType(),
                request.getText() != null ? request.getText().length() : 0);

        if (request.getText() == null || request.getText().isBlank()) {
            return Map.of("result", "", "error", "文本内容不能为空");
        }

        try {
            String systemPrompt = buildPolishSystemPrompt(request.getFieldType());
            String userMessage = buildUserMessage(request);
            String polished = aiAgentService.chatWithSystemPrompt(systemPrompt, userMessage);
            return Map.of("result", polished);
        } catch (Exception e) {
            log.error("AI文本润色失败", e);
            return Map.of("result", "", "error", "AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 根据字段类型构建系统提示词
     */
    private String buildPolishSystemPrompt(String fieldType) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深的简历优化专家，擅长用专业、精炼的语言提升简历内容的表达质量。");
        sb.append("请遵循以下原则：\n");
        sb.append("1. 保持原文的核心信息和事实不变\n");
        sb.append("2. 使用STAR法则（情境、任务、行动、结果）优化表述\n");
        sb.append("3. 突出量化成果和关键数据\n");
        sb.append("4. 使用动词开头的句式，增强行动力\n");
        sb.append("5. 语言简洁专业，避免冗余和口语化表达\n");
        sb.append("6. 直接输出润色后的文本，不要添加任何解释或前缀\n");

        if ("description".equals(fieldType)) {
            sb.append("\n当前优化目标：工作经历描述。突出岗位职责的核心价值和个人贡献。\n");
        } else if ("background".equals(fieldType)) {
            sb.append("\n当前优化目标：项目背景描述。清晰说明项目的业务价值和技术挑战。\n");
        } else if ("duty".equals(fieldType)) {
            sb.append("\n当前优化目标：个人职责描述。突出个人在项目中的具体贡献和技术能力。\n");
        } else if ("achievement".equals(fieldType)) {
            sb.append("\n当前优化目标：项目成果描述。量化成果，突出业务影响和技术突破。\n");
        }

        return sb.toString();
    }

    /**
     * 构建用户消息
     */
    private String buildUserMessage(TextPolishRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("背景信息：").append(request.getContext()).append("\n\n");
        }
        sb.append("请润色以下文本：\n").append(request.getText());
        return sb.toString();
    }
}
