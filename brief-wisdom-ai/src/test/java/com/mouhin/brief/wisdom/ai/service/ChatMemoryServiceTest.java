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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChatMemoryService 对话记忆服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemoryService 对话记忆服务测试")
class ChatMemoryServiceTest {

    private static final String USER_ID = "test-user-001";
    private static final String SESSION_ID = "session-001";
    @Mock
    private ChatMemoryRepository chatMemoryRepository;
    @InjectMocks
    private ChatMemoryService chatMemoryService;

    @Test
    @DisplayName("saveMemory 新增记忆应调用 repository.save")
    void testSaveNewMemory() {
        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "name")).thenReturn(null);

        chatMemoryService.saveMemory(USER_ID, "fact", "name", "张三", SESSION_ID);

        ArgumentCaptor<ChatMemory> captor = ArgumentCaptor.forClass(ChatMemory.class);
        verify(chatMemoryRepository).save(captor.capture());

        ChatMemory saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("fact", saved.getCategory());
        assertEquals("name", saved.getMemoryKey());
        assertEquals("张三", saved.getMemoryValue());
        assertEquals(SESSION_ID, saved.getSourceSessionId());
        assertEquals(0, saved.getAccessCount());
    }

    @Test
    @DisplayName("saveMemory 更新已有记忆应调用 repository.update")
    void testUpdateExistingMemory() {
        ChatMemory existing = new ChatMemory();
        existing.setId(1L);
        existing.setUserId(USER_ID);
        existing.setMemoryKey("name");
        existing.setMemoryValue("旧名字");

        when(chatMemoryRepository.findByUserIdAndKey(USER_ID, "name")).thenReturn(existing);

        chatMemoryService.saveMemory(USER_ID, "fact", "name", "新名字", SESSION_ID);

        verify(chatMemoryRepository).update(existing);
        assertEquals("新名字", existing.getMemoryValue());
    }

    @Test
    @DisplayName("buildMemoryContext 无记忆时返回空字符串")
    void testBuildMemoryContextEmpty() {
        when(chatMemoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        String context = chatMemoryService.buildMemoryContext(USER_ID);
        assertEquals("", context);
    }

    @Test
    @DisplayName("buildMemoryContext 有记忆时应格式化输出")
    void testBuildMemoryContextWithMemories() {
        ChatMemory mem1 = new ChatMemory();
        mem1.setMemoryKey("name");
        mem1.setMemoryValue("张三");

        ChatMemory mem2 = new ChatMemory();
        mem2.setMemoryKey("tech_stack");
        mem2.setMemoryValue("Java, Spring Boot");

        when(chatMemoryRepository.findByUserId(USER_ID)).thenReturn(List.of(mem1, mem2));

        String context = chatMemoryService.buildMemoryContext(USER_ID);

        assertTrue(context.contains("用户记忆"));
        assertTrue(context.contains("name: 张三"));
        assertTrue(context.contains("tech_stack: Java, Spring Boot"));
    }

    @Test
    @DisplayName("extractMemoriesFromMessage 应提取用户姓名")
    void testExtractName() {
        chatMemoryService.extractMemoriesFromMessage(USER_ID, "我叫张三，请多关照", SESSION_ID);

        verify(chatMemoryRepository).findByUserIdAndKey(USER_ID, "name");
        verify(chatMemoryRepository).save(any(ChatMemory.class));
    }

    @Test
    @DisplayName("extractMemoriesFromMessage 空消息不应提取")
    void testExtractFromEmptyMessage() {
        chatMemoryService.extractMemoriesFromMessage(USER_ID, "", SESSION_ID);
        chatMemoryService.extractMemoriesFromMessage(USER_ID, null, SESSION_ID);

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    @DisplayName("clearMemories 应调用 repository.deleteByUserId")
    void testClearMemories() {
        chatMemoryService.clearMemories(USER_ID);

        verify(chatMemoryRepository).deleteByUserId(USER_ID);
    }
}
