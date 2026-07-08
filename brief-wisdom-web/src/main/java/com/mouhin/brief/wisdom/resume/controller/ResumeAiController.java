package com.mouhin.brief.wisdom.resume.controller;

import com.mouhin.brief.wisdom.ai.service.AiAgentService;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.resume.req.FullPolishRequest;
import com.mouhin.brief.wisdom.resume.req.ResumeSuggestRequest;
import com.mouhin.brief.wisdom.resume.req.TextPolishRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 简历AI辅助 REST 接口
 * <p>
 * 提供三种 AI 能力：
 * 1. 单字段润色（POST /polish）—— 对单个文本字段进行润色
 * 2. 全文润色（POST /polish/full）—— 对完整简历数据逐字段给出润色结果
 * 3. AI 建议（POST /suggest）—— 基于完整简历数据给出优化建议
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@RestController
@RequestMapping("/api/resume/ai")
@RequiredArgsConstructor
@Slf4j
@RequiresPermission("resume:manage")
@Tag(name = "简历AI", description = "简历文本润色与优化建议")
public class ResumeAiController {

    private final AiAgentService aiAgentService;

    /**
     * AI单字段文本润色
     */
    @PostMapping("/polish")
    @Operation(summary = "单字段文本润色")
    public Map<String, String> polishText(@RequestBody TextPolishRequest request) {
        log.info("AI文本润色请求, fieldType={}, textLength={}", request.getFieldType(),
                request.getText() != null ? request.getText().length() : 0);

        if (request.getText() == null || request.getText().isBlank()) {
            return Map.of("result", "", "error", "文本内容不能为空");
        }

        try {
            String systemPrompt = buildPolishSystemPrompt(request.getFieldType());
            String userMessage = buildPolishUserMessage(request);
            String polished = aiAgentService.chatWithSystemPrompt(systemPrompt, userMessage);
            return Map.of("result", polished);
        } catch (Exception e) {
            log.error("AI文本润色失败", e);
            return Map.of("result", "", "error", "AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI全文润色 —— 传入完整简历数据，返回每个字段的润色结果
     */
    @PostMapping("/polish/full")
    @Operation(summary = "全文润色", description = "对完整简历数据逐字段润色")
    public Map<String, Object> fullPolish(@RequestBody FullPolishRequest request) {
        log.info("AI全文润色请求");

        try {
            String systemPrompt = buildFullPolishSystemPrompt();
            String userMessage = buildFullPolishUserMessage(request);
            String result = aiAgentService.chatWithSystemPrompt(systemPrompt, userMessage);
            return Map.of("result", result, "success", true);
        } catch (Exception e) {
            log.error("AI全文润色失败", e);
            return Map.of("result", "", "success", false, "error", "AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI简历优化建议 —— 基于完整简历数据给出多维度优化建议
     */
    @PostMapping("/suggest")
    @Operation(summary = "AI 优化建议", description = "基于完整简历数据给出多维度优化建议")
    public Map<String, Object> suggest(@RequestBody ResumeSuggestRequest request) {
        log.info("AI简历建议请求, dimensions={}", request.getDimensions());

        try {
            String systemPrompt = buildSuggestSystemPrompt(request.getDimensions());
            String userMessage = buildSuggestUserMessage(request);
            String result = aiAgentService.chatWithSystemPrompt(systemPrompt, userMessage);
            return Map.of("result", result, "success", true);
        } catch (Exception e) {
            log.error("AI简历建议失败", e);
            return Map.of("result", "", "success", false, "error", "AI服务暂时不可用，请稍后重试");
        }
    }

    // ==================== 单字段润色 ====================

    private String buildPolishSystemPrompt(String fieldType) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深的简历优化专家，擅长用专业、精炼的语言提升简历内容的表达质量。\n");
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

    private String buildPolishUserMessage(TextPolishRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("背景信息：").append(request.getContext()).append("\n\n");
        }
        sb.append("请润色以下文本：\n").append(request.getText());
        return sb.toString();
    }

    // ==================== 全文润色 ====================

    private String buildFullPolishSystemPrompt() {
        return """
                你是一位资深的简历优化专家，拥有多年HR和技术面试经验。
                你将收到一份完整的简历数据，需要对每个字段进行润色优化。
                
                请遵循以下原则：
                1. 保持原文的核心信息和事实不变
                2. 使用STAR法则优化工作经历和项目描述的表述
                3. 突出量化成果和关键数据，适当补充合理的量化指标
                4. 使用动词开头的句式，增强行动力
                5. 语言简洁专业，避免冗余和口语化表达
                6. 个人描述要突出核心竞争力和求职意向
                
                请以JSON格式输出润色结果，格式如下：
                {
                  "personalSummary": "润色后的个人描述",
                  "experiences": [
                    {
                      "description": "润色后的工作经历描述",
                      "projects": [
                        {
                          "background": "润色后的项目背景",
                          "duty": "润色后的个人职责",
                          "achievements": ["润色后的成果1", "润色后的成果2"]
                        }
                      ]
                    }
                  ]
                }
                
                只输出JSON，不要添加任何其他文字。
                """;
    }

    private String buildFullPolishUserMessage(FullPolishRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是我的完整简历数据，请逐字段润色优化：\n\n");

        if (request.getPersonalSummary() != null && !request.getPersonalSummary().isBlank()) {
            sb.append("【个人描述】\n").append(request.getPersonalSummary()).append("\n\n");
        }

        if (request.getExperiences() != null) {
            for (int i = 0; i < request.getExperiences().size(); i++) {
                var exp = request.getExperiences().get(i);
                sb.append("【工作经历 ").append(i + 1).append("】\n");
                if (exp.getCompany() != null) {
                    sb.append("公司：").append(exp.getCompany()).append("\n");
                }
                if (exp.getPosition() != null) {
                    sb.append("职位：").append(exp.getPosition()).append("\n");
                }
                if (exp.getDescription() != null) {
                    sb.append("描述：").append(exp.getDescription()).append("\n");
                }

                if (exp.getProjects() != null) {
                    for (int j = 0; j < exp.getProjects().size(); j++) {
                        var proj = exp.getProjects().get(j);
                        sb.append("\n  【项目 ").append(j + 1).append("】\n");
                        if (proj.getName() != null) {
                            sb.append("  名称：").append(proj.getName()).append("\n");
                        }
                        if (proj.getBackground() != null) {
                            sb.append("  背景：").append(proj.getBackground()).append("\n");
                        }
                        if (proj.getDuty() != null) {
                            sb.append("  职责：").append(proj.getDuty()).append("\n");
                        }
                        if (proj.getAchievements() != null) {
                            sb.append("  成果：\n");
                            for (String achievement : proj.getAchievements()) {
                                sb.append("    - ").append(achievement).append("\n");
                            }
                        }
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    // ==================== AI 建议 ====================

    private String buildSuggestSystemPrompt(List<String> dimensions) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深的简历顾问，拥有丰富的HR经验和技术背景。\n");
        sb.append("你将收到一份完整的简历数据，需要从以下维度进行专业评估并给出具体可操作的优化建议。\n\n");

        if (dimensions == null || dimensions.isEmpty() || dimensions.contains("overall")) {
            sb.append("【整体评估】\n");
            sb.append("- 简历整体印象和竞争力评分（1-10分）\n");
            sb.append("- 最大的优势和最需要改进的地方\n");
            sb.append("- 针对目标岗位的匹配度分析\n\n");
        }

        if (dimensions == null || dimensions.isEmpty() || dimensions.contains("content")) {
            sb.append("【内容优化】\n");
            sb.append("- 工作经历描述是否突出量化成果\n");
            sb.append("- 项目经验是否体现技术深度和广度\n");
            sb.append("- 个人描述是否清晰传达核心价值\n");
            sb.append("- 具体的措辞改进建议\n\n");
        }

        if (dimensions == null || dimensions.isEmpty() || dimensions.contains("keywords")) {
            sb.append("【关键词优化】\n");
            sb.append("- 当前简历中缺失的关键技术词汇\n");
            sb.append("- 建议补充的行业热词和岗位关键词\n");
            sb.append("- ATS（简历筛选系统）友好性建议\n\n");
        }

        if (dimensions == null || dimensions.isEmpty() || dimensions.contains("layout")) {
            sb.append("【排版建议】\n");
            sb.append("- 信息层次是否清晰\n");
            sb.append("- 重点内容是否突出\n");
            sb.append("- 篇幅是否合理\n\n");
        }

        sb.append("请以清晰的Markdown格式输出建议，每个维度给出具体的改进方案，不要泛泛而谈。");
        return sb.toString();
    }

    private String buildSuggestUserMessage(ResumeSuggestRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是我的完整简历数据，请给出优化建议：\n\n");

        if (request.getPersonalSummary() != null && !request.getPersonalSummary().isBlank()) {
            sb.append("【个人描述】\n").append(request.getPersonalSummary()).append("\n\n");
        }

        if (request.getExperiences() != null) {
            for (int i = 0; i < request.getExperiences().size(); i++) {
                var exp = request.getExperiences().get(i);
                sb.append("【工作经历 ").append(i + 1).append("】\n");
                if (exp.getCompany() != null) {
                    sb.append("公司：").append(exp.getCompany()).append("\n");
                }
                if (exp.getPosition() != null) {
                    sb.append("职位：").append(exp.getPosition()).append("\n");
                }
                if (exp.getDescription() != null) {
                    sb.append("描述：").append(exp.getDescription()).append("\n");
                }

                if (exp.getProjects() != null) {
                    for (int j = 0; j < exp.getProjects().size(); j++) {
                        var proj = exp.getProjects().get(j);
                        sb.append("\n  【项目 ").append(j + 1).append("】\n");
                        if (proj.getName() != null) {
                            sb.append("  名称：").append(proj.getName()).append("\n");
                        }
                        if (proj.getBackground() != null) {
                            sb.append("  背景：").append(proj.getBackground()).append("\n");
                        }
                        if (proj.getDuty() != null) {
                            sb.append("  职责：").append(proj.getDuty()).append("\n");
                        }
                        if (proj.getAchievements() != null) {
                            sb.append("  成果：\n");
                            for (String achievement : proj.getAchievements()) {
                                sb.append("    - ").append(achievement).append("\n");
                            }
                        }
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
