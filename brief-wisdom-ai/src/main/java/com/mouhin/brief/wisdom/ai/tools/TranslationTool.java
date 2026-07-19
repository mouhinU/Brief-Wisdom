package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.ai.service.ChatModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文本翻译工具
 * <p>
 * 利用现有的 ChatModel 实现文本翻译能力。
 * 支持多语言翻译，通过 Prompt 工程实现高质量翻译。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationTool {

    private final ChatModelRegistry chatModelRegistry;

    /**
     * 翻译文本到目标语言
     *
     * @param text       要翻译的文本
     * @param targetLang 目标语言代码
     * @return 翻译结果
     */
    @Tool(description = "将文本翻译为目标语言。当用户明确要求翻译某段文字、翻译为英文/中文/日文等时调用。支持中英日韩法德西等主要语言。")
    public String translateText(
            @ToolParam(description = "要翻译的文本内容") String text,
            @ToolParam(description = "目标语言代码: en(英文)/zh(中文)/ja(日文)/ko(韩文)/fr(法文)/de(德文)/es(西班牙文)") String targetLang) {

        log.info("[Tool] translateText 被调用: targetLang={}, textLength={}", targetLang,
                text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            return "翻译文本不能为空。";
        }

        String targetLanguageName = getLanguageName(targetLang);

        try {
            ChatModel chatModel = chatModelRegistry.getDefaultChatModel();
            if (chatModel == null) {
                return "翻译服务暂不可用：没有可用的 AI 模型。";
            }

            String systemPrompt = String.format(
                    "你是一个专业的翻译引擎。请将用户提供的文本翻译为%s。"
                            + "规则：1. 只输出翻译结果，不要添加任何解释或注释。"
                            + "2. 保持原文的格式和段落结构。"
                            + "3. 专有名词保持原文或提供括号注释。"
                            + "4. 翻译要自然流畅，符合目标语言的表达习惯。",
                    targetLanguageName);

            var response = chatModel.call(new Prompt(
                    java.util.List.of(
                            new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                            new org.springframework.ai.chat.messages.UserMessage(text))));

            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String translated = response.getResult().getOutput().getText();
                return "翻译结果（" + targetLanguageName + "）：\n\n" + translated;
            }

            return "翻译失败：未获取到翻译结果。";
        } catch (Exception e) {
            log.error("[Tool] translateText 执行失败: {}", e.getMessage(), e);
            return "翻译失败: " + e.getMessage();
        }
    }

    /**
     * 语言代码映射为语言名称
     */
    private String getLanguageName(String langCode) {
        if (langCode == null) {
            return "英文";
        }
        return LANGUAGE_MAP.getOrDefault(langCode.toLowerCase(), langCode);
    }

    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
            Map.entry("en", "英文"),
            Map.entry("zh", "中文"),
            Map.entry("ja", "日文"),
            Map.entry("ko", "韩文"),
            Map.entry("fr", "法文"),
            Map.entry("de", "德文"),
            Map.entry("es", "西班牙文"),
            Map.entry("ru", "俄文"),
            Map.entry("pt", "葡萄牙文"),
            Map.entry("it", "意大利文"),
            Map.entry("ar", "阿拉伯文"),
            Map.entry("th", "泰文"),
            Map.entry("vi", "越南文"));
}
