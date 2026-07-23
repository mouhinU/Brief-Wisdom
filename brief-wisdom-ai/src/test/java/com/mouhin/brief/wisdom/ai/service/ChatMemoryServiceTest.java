package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.ChatMemory;
import com.mouhin.brief.wisdom.persistence.repository.ChatMemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChatMemoryService 对话记忆服务单元测试
 * <p>
 * 覆盖记忆的增删改查、上下文构建、自动提取等核心能力。
 *
 * @author Brief-Wisdom
 * @date 2026-07-21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemoryService 对话记忆服务测试")
class ChatMemoryServiceTest {

    private static final String USER_ID = "test-user-001";
    private static final String SESSION_ID = "session-001";
    private static final String CATEGORY_FACT = "fact";
    private static final String CATEGORY_PREFERENCE = "preference";
    private static final String CATEGORY_CONTEXT = "context";

    @Mock
    private ChatMemoryRepository chatMemoryRepository;

    @InjectMocks
    private ChatMemoryService chatMemoryService;

    // ===== saveMemory 测试 =====

    @Test
    @DisplayName("saveMemory - 新增记忆：findByUserIdAndKey 返回 null 时应调用 save")
    void testSaveMemory_newMemory() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "name")).thenReturn(null);

        chatMemoryService.saveMemory(USER_ID, CATEGORY_FACT, "name", "张三", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());
        verify(chatMemoryRepository, never()).update(any(ChatMemory.class));

        ChatMemory saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(CATEGORY_FACT, saved.getCategory());
        assertEquals("name", saved.getMemoryKey());
        assertEquals("张三", saved.getMemoryValue());
        assertEquals(SESSION_ID, saved.getSourceSessionId());
        assertEquals(0, saved.getAccessCount());
    }

    @Test
    @DisplayName("saveMemory - 更新已有记忆：findByUserIdAndKey 返回已有记录时应调用 update")
    void testSaveMemory_existingMemory() {
        ChatMemory existing = new ChatMemory();
        existing.setId(1L);
        existing.setUserId(USER_ID);
        existing.setCategory(CATEGORY_FACT);
        existing.setMemoryKey("name");
        existing.setMemoryValue("旧名字");
        existing.setSourceSessionId("old-session");

        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "name")).thenReturn(existing);

        chatMemoryService.saveMemory(USER_ID, CATEGORY_FACT, "name", "新名字", SESSION_ID);

        verify(chatMemoryRepository).update(existing);
        verify(chatMemoryRepository, never()).save(any(ChatMemory.class));

        assertEquals("新名字", existing.getMemoryValue());
        assertEquals(CATEGORY_FACT, existing.getCategory());
        assertEquals(SESSION_ID, existing.getSourceSessionId());
    }

    // ===== buildMemoryContext 测试 =====

    @Test
    @DisplayName("buildMemoryContext - 无记忆时应返回空字符串")
    void testBuildMemoryContext_emptyMemories() {
        when(chatMemoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        String context = chatMemoryService.buildMemoryContext(USER_ID, SESSION_ID);

        assertEquals("", context);
    }

    @Test
    @DisplayName("buildMemoryContext - 有记忆时应返回格式化上下文字符串")
    void testBuildMemoryContext_withMemories() {
        ChatMemory mem1 = new ChatMemory();
        mem1.setMemoryKey("name");
        mem1.setMemoryValue("张三");
        mem1.setSourceSessionId(SESSION_ID);

        ChatMemory mem2 = new ChatMemory();
        mem2.setMemoryKey("tech_stack");
        mem2.setMemoryValue("Java, Spring Boot");
        mem2.setSourceSessionId(SESSION_ID);

        when(chatMemoryRepository.findByUserId(USER_ID)).thenReturn(List.of(mem1, mem2));

        String context = chatMemoryService.buildMemoryContext(USER_ID, SESSION_ID);

        assertTrue(context.contains("--- 用户记忆 ---"));
        assertTrue(context.contains("以下是你在本会话中记住的关于该用户的信息"));
        assertTrue(context.contains("- name: 张三"));
        assertTrue(context.contains("- tech_stack: Java, Spring Boot"));
        assertTrue(context.contains("--- 记忆结束 ---"));
    }

    @Test
    @DisplayName("buildMemoryContext - 其他会话来源的记忆不应注入当前会话（会话隔离）")
    void testBuildMemoryContext_sessionIsolation() {
        ChatMemory currentSessionMemory = new ChatMemory();
        currentSessionMemory.setMemoryKey("name");
        currentSessionMemory.setMemoryValue("张三");
        currentSessionMemory.setSourceSessionId(SESSION_ID);

        ChatMemory otherSessionMemory = new ChatMemory();
        otherSessionMemory.setMemoryKey("current_project");
        otherSessionMemory.setMemoryValue("其他会话的项目");
        otherSessionMemory.setSourceSessionId("other-session");

        when(chatMemoryRepository.findByUserId(USER_ID))
                .thenReturn(List.of(currentSessionMemory, otherSessionMemory));

        String context = chatMemoryService.buildMemoryContext(USER_ID, SESSION_ID);

        assertTrue(context.contains("- name: 张三"));
        assertFalse(context.contains("其他会话的项目"));
        assertFalse(context.contains("current_project"));
    }

    @Test
    @DisplayName("buildMemoryContext - sessionId 为空时应返回空字符串")
    void testBuildMemoryContext_nullSessionId() {
        String context = chatMemoryService.buildMemoryContext(USER_ID, null);

        assertEquals("", context);
        verifyNoInteractions(chatMemoryRepository);
    }

    // ===== extractMemoriesFromMessage 测试 =====

    @Test
    @DisplayName("extractMemoriesFromMessage - '我叫张三' 应提取 fact/name")
    void testExtractMemories_name() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "name")).thenReturn(null);

        chatMemoryService.extractMemoriesFromMessage(USER_ID, "我叫张三", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());

        ChatMemory saved = captor.getValue();
        assertEquals(CATEGORY_FACT, saved.getCategory());
        assertEquals("name", saved.getMemoryKey());
        assertEquals("张三", saved.getMemoryValue());
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - '我在阿里巴巴工作' 应提取 fact/company")
    void testExtractMemories_company() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "company")).thenReturn(null);

        chatMemoryService.extractMemoriesFromMessage(USER_ID, "我在阿里巴巴工作", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());

        ChatMemory saved = captor.getValue();
        assertEquals(CATEGORY_FACT, saved.getCategory());
        assertEquals("company", saved.getMemoryKey());
        assertEquals("阿里巴巴", saved.getMemoryValue());
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - '我是Java开发' 应提取 fact/role")
    void testExtractMemories_role() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "role")).thenReturn(null);

        chatMemoryService.extractMemoriesFromMessage(USER_ID, "我是Java开发", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());

        ChatMemory saved = captor.getValue();
        assertEquals(CATEGORY_FACT, saved.getCategory());
        assertEquals("role", saved.getMemoryKey());
        assertEquals("Java", saved.getMemoryValue());
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - '我用Spring框架' 应提取 preference/tech_stack")
    void testExtractMemories_techStack() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "tech_stack")).thenReturn(null);

        chatMemoryService.extractMemoriesFromMessage(USER_ID, "我用Spring框架", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());

        ChatMemory saved = captor.getValue();
        assertEquals(CATEGORY_PREFERENCE, saved.getCategory());
        assertEquals("tech_stack", saved.getMemoryKey());
        assertEquals("Spring", saved.getMemoryValue());
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - null 消息不应有任何交互")
    void testExtractMemories_nullMessage() {
        chatMemoryService.extractMemoriesFromMessage(USER_ID, null, SESSION_ID);

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - 空白消息不应有任何交互")
    void testExtractMemories_blankMessage() {
        chatMemoryService.extractMemoriesFromMessage(USER_ID, "", SESSION_ID);
        chatMemoryService.extractMemoriesFromMessage(USER_ID, "   ", SESSION_ID);

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    @DisplayName("extractMemoriesFromMessage - 无匹配模式的消息不应保存记忆")
    void testExtractMemories_noPatternMatch() {
        chatMemoryService.extractMemoriesFromMessage(USER_ID, "今天天气真不错", SESSION_ID);

        verify(chatMemoryRepository, never()).save(any(ChatMemory.class));
        verify(chatMemoryRepository, never()).update(any(ChatMemory.class));
    }

    // ===== listMemories 测试 =====

    @Test
    @DisplayName("listMemories - 应调用 repository.findByUserId")
    void testListMemories() {
        ChatMemory mem = new ChatMemory();
        mem.setMemoryKey("name");
        mem.setMemoryValue("张三");
        when(chatMemoryRepository.findByUserId(USER_ID)).thenReturn(List.of(mem));

        List<ChatMemory> result = chatMemoryService.listMemories(USER_ID);

        assertEquals(1, result.size());
        assertEquals("name", result.get(0).getMemoryKey());
        verify(chatMemoryRepository).findByUserId(USER_ID);
    }

    // ===== deleteMemory 测试 =====

    @Test
    @DisplayName("deleteMemory - 应调用 repository.deleteById")
    void testDeleteMemory() {
        Long memoryId = 42L;

        chatMemoryService.deleteMemory(memoryId);

        verify(chatMemoryRepository).deleteById(memoryId);
    }

    // ===== clearMemories 测试 =====

    @Test
    @DisplayName("clearMemories - 应调用 repository.deleteByUserId")
    void testClearMemories() {
        chatMemoryService.clearMemories(USER_ID);

        verify(chatMemoryRepository).deleteByUserId(USER_ID);
    }
}
