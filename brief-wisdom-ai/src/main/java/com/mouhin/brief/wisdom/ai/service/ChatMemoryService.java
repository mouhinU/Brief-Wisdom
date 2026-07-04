package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.ChatMemory;
import com.mouhin.brief.wisdom.persistence.repository.ChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话记忆服务 —— 跨会话记住用户偏好和关键信息
 * <p>
 * 提供两种能力：
 * 1. 手动记忆：用户或系统主动存储/查询/删除记忆
 * 2. 自动提取：从 AI 对话中提取用户偏好（后续可扩展）
 * <p>
 * 记忆分类：
 * - preference：用户偏好（如：preferred_language, code_style）
 * - fact：用户事实（如：name, company, tech_stack）
 * - context：上下文信息（如：current_project, working_on）
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatMemoryRepository chatMemoryRepository;

    /**
     * 存储或更新用户记忆
     *
     * @param userId    用户ID
     * @param category  分类（preference/fact/context）
     * @param key       记忆键
     * @param value     记忆值
     * @param sessionId 来源会话ID
     */
    public void saveMemory(String userId, String category, String key, String value, String sessionId) {
        ChatMemory existing = chatMemoryRepository.findByUserIdAndKey(userId, key);
        if (existing != null) {
            existing.setMemoryValue(value);
            existing.setCategory(category);
            existing.setSourceSessionId(sessionId);
            chatMemoryRepository.update(existing);
            log.info("更新用户记忆: userId={}, key={}", userId, key);
        } else {
            ChatMemory memory = new ChatMemory();
            memory.setUserId(userId);
            memory.setCategory(category);
            memory.setMemoryKey(key);
            memory.setMemoryValue(value);
            memory.setSourceSessionId(sessionId);
            memory.setAccessCount(0);
            chatMemoryRepository.save(memory);
            log.info("新增用户记忆: userId={}, key={}", userId, key);
        }
    }

    /**
     * 获取用户的所有记忆，构建为 AI 上下文
     *
     * @param userId 用户ID
     * @return 格式化的记忆上下文，可注入系统提示词
     */
    public String buildMemoryContext(String userId) {
        List<ChatMemory> memories = chatMemoryRepository.findByUserId(userId);
        if (memories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n--- 用户记忆 ---\n");
        sb.append("以下是你记住的关于该用户的信息，请在回答时参考：\n");

        for (ChatMemory memory : memories) {
            sb.append("- ").append(memory.getMemoryKey()).append(": ").append(memory.getMemoryValue()).append("\n");
        }

        sb.append("--- 记忆结束 ---\n");
        return sb.toString();
    }

    /**
     * 获取用户的所有记忆（原始列表）
     */
    public List<ChatMemory> listMemories(String userId) {
        return chatMemoryRepository.findByUserId(userId);
    }

    /**
     * 按分类获取用户记忆
     */
    public List<ChatMemory> listMemories(String userId, String category) {
        return chatMemoryRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * 删除指定记忆
     */
    public void deleteMemory(Long memoryId) {
        chatMemoryRepository.deleteById(memoryId);
    }

    /**
     * 清除用户的所有记忆
     */
    public void clearMemories(String userId) {
        chatMemoryRepository.deleteByUserId(userId);
        log.info("已清除用户所有记忆: userId={}", userId);
    }

    /**
     * 记录记忆访问（增加访问次数）
     */
    public void recordAccess(Long memoryId) {
        chatMemoryRepository.incrementAccessCount(memoryId);
    }

    /**
     * 从对话消息中自动提取记忆
     * <p>
     * 简单实现：基于正则匹配常见的用户信息表达模式。
     * 后续可升级为让 AI 模型自动提取。
     *
     * @param userId      用户ID
     * @param userMessage 用户消息
     * @param sessionId   会话ID
     */
    public void extractMemoriesFromMessage(String userId, String userMessage, String sessionId) {
        if (userMessage == null || userMessage.isBlank()) {
            return;
        }

        // 提取模式映射：正则 -> (category, key)
        Map<Pattern, String[]> extractionPatterns = Map.of(
                Pattern.compile("我叫([^，。,.\\s]{2,10})"), new String[]{"fact", "name"},
                Pattern.compile("我在(.{2,20})(公司|企业|单位|工作)"), new String[]{"fact", "company"},
                Pattern.compile("我是(.{2,20})(开发|工程师|设计师|产品经理|运营)"), new String[]{"fact", "role"},
                Pattern.compile("我用(.{2,20})(语言|框架|技术)"), new String[]{"preference", "tech_stack"},
                Pattern.compile("我喜欢(.{2,30})(风格|方式|模式)"), new String[]{"preference", "style"},
                Pattern.compile("我在做(.{2,30})(项目|系统|平台)"), new String[]{"context", "current_project"}
        );

        for (var entry : extractionPatterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(userMessage);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isBlank() && value.length() >= 2) {
                    String[] meta = entry.getValue();
                    saveMemory(userId, meta[0], meta[1], value, sessionId);
                }
            }
        }
    }
}
