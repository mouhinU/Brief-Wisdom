package com.mouhin.brief.wisdom.ai.service.tools;

import com.mouhin.brief.wisdom.ai.service.ChatMemoryService;
import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import com.mouhin.brief.wisdom.persistence.model.ChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户记忆管理工具
 * <p>
 * 允许 AI 助手主动管理用户长期记忆：查看、保存、删除记忆条目。
 * 用户可以说"记住这个"、"你还记得吗"、"忘掉XX"来触发。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryManagementTool {

    private final ChatMemoryService chatMemoryService;
    private final ToolContextProvider toolContextProvider;

    /**
     * 查看用户的所有记忆
     *
     * @return 格式化的记忆列表
     */
    @Tool(description = "查看当前用户的所有长期记忆条目。当用户问'你还记得我吗'、'我有哪些记忆'、'列出记忆'时调用。")
    public String listMemories() {
        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] listMemories 被调用: userId={}", userId);

        try {
            List<ChatMemory> memories = chatMemoryService.listMemories(userId);
            if (memories.isEmpty()) {
                return "当前没有记住任何关于你的信息。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("我记住了以下关于你的信息（共 ").append(memories.size()).append(" 条）：\n\n");
            for (ChatMemory memory : memories) {
                sb.append("- [").append(memory.getCategory()).append("] ")
                        .append(memory.getMemoryKey()).append(": ")
                        .append(memory.getMemoryValue()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] listMemories 执行失败: {}", e.getMessage(), e);
            return "查看记忆失败: " + e.getMessage();
        }
    }

    /**
     * 保存一条新记忆
     *
     * @param category 记忆分类
     * @param key      记忆键
     * @param value    记忆值
     * @return 操作结果
     */
    @Tool(description = "保存一条新的用户记忆。当用户说'记住这个'、'请记住'、'以后记得'时调用。")
    public String saveMemory(
            @ToolParam(description = "记忆分类: preference(偏好)/fact(事实)/context(上下文)") String category,
            @ToolParam(description = "记忆键，简短描述，如 name、tech_stack、code_style") String key,
            @ToolParam(description = "记忆值，具体内容") String value) {

        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] saveMemory 被调用: userId={}, category={}, key={}", userId, category, key);

        try {
            String validCategory = validateCategory(category);
            chatMemoryService.saveMemory(userId, validCategory, key, value, null);
            return "已记住: [" + validCategory + "] " + key + " = " + value;
        } catch (Exception e) {
            log.error("[Tool] saveMemory 执行失败: {}", e.getMessage(), e);
            return "保存记忆失败: " + e.getMessage();
        }
    }

    /**
     * 删除指定记忆
     *
     * @param key 要删除的记忆键
     * @return 操作结果
     */
    @Tool(description = "删除指定的用户记忆。当用户说'忘掉XX'、'不要再记住XX'、'删除记忆'时调用。")
    public String deleteMemory(
            @ToolParam(description = "要删除的记忆键名称，如 name、tech_stack") String key) {

        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] deleteMemory 被调用: userId={}, key={}", userId, key);

        try {
            List<ChatMemory> memories = chatMemoryService.listMemories(userId);
            ChatMemory target = memories.stream()
                    .filter(m -> key.equals(m.getMemoryKey()))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return "没有找到键为「" + key + "」的记忆条目。";
            }

            chatMemoryService.deleteMemory(target.getId());
            return "已删除记忆: " + key + " = " + target.getMemoryValue();
        } catch (Exception e) {
            log.error("[Tool] deleteMemory 执行失败: {}", e.getMessage(), e);
            return "删除记忆失败: " + e.getMessage();
        }
    }

    /**
     * 校验并规范化记忆分类
     */
    private String validateCategory(String category) {
        if (category == null || category.isBlank()) {
            return "fact";
        }
        return switch (category.toLowerCase()) {
            case "preference", "pref" -> "preference";
            case "fact", "info" -> "fact";
            case "context", "ctx" -> "context";
            default -> "fact";
        };
    }
}
