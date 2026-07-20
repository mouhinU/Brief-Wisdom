package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.ai.service.impl.AiManageServiceImpl;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatSession;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.ChatMessageRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatSessionRepository;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AiManageServiceImpl AI助手管理服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiManageServiceImpl AI助手管理服务测试")
class AiManageServiceImplTest {

    @Mock
    private ChatUserRepository chatUserRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private AiManageServiceImpl aiManageService;

    @Test
    @DisplayName("listUsers 返回所有用户")
    void testListUsers() {
        ChatUser user = buildUser(1L, "user-001", "张三");
        when(chatUserRepository.findAllOrderByCreateTimeDesc()).thenReturn(List.of(user));
        when(chatSessionRepository.countSessionsGroupedByUserIds(List.of("user-001")))
                .thenReturn(Map.of("user-001", 5L));

        List<UserDTO> result = aiManageService.listUsers();
        assertEquals(1, result.size());
        assertEquals("user-001", result.get(0).getUserId());
        assertEquals(5, result.get(0).getSessionCount());
    }

    @Test
    @DisplayName("listUsersByLevel 按级别筛选用户")
    void testListUsersByLevel() {
        ChatUser user = buildUser(1L, "user-001", "张三");
        when(chatUserRepository.findByUserLevelOrderByCreateTimeDesc("vip")).thenReturn(List.of(user));
        when(chatSessionRepository.countSessionsGroupedByUserIds(List.of("user-001")))
                .thenReturn(Map.of("user-001", 3L));

        List<UserDTO> result = aiManageService.listUsersByLevel("vip");
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getNickname());
    }

    @Test
    @DisplayName("listUserLevels 返回三个级别")
    void testListUserLevels() {
        List<String> levels = aiManageService.listUserLevels();
        assertEquals(3, levels.size());
        assertTrue(levels.contains("admin"));
        assertTrue(levels.contains("vip"));
        assertTrue(levels.contains("normal"));
    }

    @Test
    @DisplayName("listSessionsByUserId 返回用户会话列表")
    void testListSessionsByUserId() {
        ChatSession session = buildSession("session-001", "user-001", "测试会话");
        when(chatSessionRepository.findByUserIdOrderByUpdateTimeDesc("user-001")).thenReturn(List.of(session));
        when(chatMessageRepository.findLastMessageTimesBySessionIds(List.of("session-001")))
                .thenReturn(List.of());

        List<SessionDTO> result = aiManageService.listSessionsByUserId("user-001");
        assertEquals(1, result.size());
        assertEquals("session-001", result.get(0).getSessionId());
    }

    @Test
    @DisplayName("listSessionsByUserLevel 无用户时返回空列表")
    void testListSessionsByUserLevel_noUsers() {
        when(chatUserRepository.findByUserLevel("unknown")).thenReturn(List.of());

        List<SessionDTO> result = aiManageService.listSessionsByUserLevel("unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getSessionMessages 返回会话消息")
    void testGetSessionMessages() {
        when(chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-001")).thenReturn(List.of());

        var result = aiManageService.getSessionMessages("session-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private ChatUser buildUser(Long id, String userId, String nickname) {
        ChatUser user = new ChatUser();
        user.setId(id);
        user.setUserId(userId);
        user.setUsername(userId);
        user.setNickname(nickname);
        user.setUserLevel("normal");
        return user;
    }

    private ChatSession buildSession(String sessionId, String userId, String title) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(title);
        session.setMessageCount(10);
        return session;
    }
}
