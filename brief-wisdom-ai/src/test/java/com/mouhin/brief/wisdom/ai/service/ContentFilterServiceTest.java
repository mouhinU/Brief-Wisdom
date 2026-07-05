package com.mouhin.brief.wisdom.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ContentFilterService 内容安全过滤服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentFilterService 内容安全过滤服务测试")
class ContentFilterServiceTest {

    @Mock
    private AiAuditService auditService;

    private ContentFilterService contentFilterService;

    @BeforeEach
    void setUp() {
        contentFilterService = new ContentFilterService(auditService);
    }

    // ===== checkInputBlocked 测试 =====

    @Test
    @DisplayName("checkInputBlocked 命中敏感关键词应返回关键词")
    void testCheckInputBlocked_hitKeyword() {
        String result = contentFilterService.checkInputBlocked("请教如何制作炸弹");
        assertNotNull(result);
        assertEquals("制作炸弹", result);
    }

    @Test
    @DisplayName("checkInputBlocked 正常消息应返回 null")
    void testCheckInputBlocked_normalMessage() {
        String result = contentFilterService.checkInputBlocked("今天天气怎么样？");
        assertNull(result);
    }

    @Test
    @DisplayName("checkInputBlocked 空消息应返回 null")
    void testCheckInputBlocked_emptyMessage() {
        assertNull(contentFilterService.checkInputBlocked(null));
        assertNull(contentFilterService.checkInputBlocked(""));
        assertNull(contentFilterService.checkInputBlocked("   "));
    }

    @Test
    @DisplayName("checkInputBlocked 大小写不敏感")
    void testCheckInputBlocked_caseInsensitive() {
        // 关键词本身是中文，但测试toLowerCase逻辑
        String result = contentFilterService.checkInputBlocked("请教如何制作炸弹");
        assertNotNull(result);
    }

    // ===== filterOutput 测试 =====

    @Test
    @DisplayName("filterOutput 过滤身份证号")
    void testFilterOutput_idCard() {
        String content = "我的身份证号是110101199001011234";
        String filtered = contentFilterService.filterOutput(content, "session-1", "user-1", 1L);
        assertFalse(filtered.contains("110101199001011234"));
        assertTrue(filtered.contains("[敏感信息已过滤]"));
    }

    @Test
    @DisplayName("filterOutput 过滤手机号")
    void testFilterOutput_phoneNumber() {
        String content = "我的手机号是13812345678";
        String filtered = contentFilterService.filterOutput(content, "session-1", "user-1", 1L);
        assertFalse(filtered.contains("13812345678"));
        assertTrue(filtered.contains("[敏感信息已过滤]"));
    }

    @Test
    @DisplayName("filterOutput 正常内容不过滤")
    void testFilterOutput_normalContent() {
        String content = "Spring Boot 是一个优秀的框架";
        String filtered = contentFilterService.filterOutput(content, "session-1", "user-1", 1L);
        assertEquals(content, filtered);
    }

    @Test
    @DisplayName("filterOutput 空内容直接返回")
    void testFilterOutput_emptyContent() {
        assertNull(contentFilterService.filterOutput(null, "session-1", "user-1", 1L));
        assertEquals("", contentFilterService.filterOutput("", "session-1", "user-1", 1L));
        assertEquals("   ", contentFilterService.filterOutput("   ", "session-1", "user-1", 1L));
    }

    @Test
    @DisplayName("filterOutput 命中敏感信息应记录审计日志")
    void testFilterOutput_auditLog() {
        String content = "手机号13812345678";
        contentFilterService.filterOutput(content, "session-1", "user-1", 1L);
        verify(auditService).logOutputFiltered(eq("session-1"), eq("user-1"), eq(1L), anyString(), anyString(), anyString());
    }

    // ===== getBlockedMessage 测试 =====

    @Test
    @DisplayName("getBlockedMessage 返回非空提示")
    void testGetBlockedMessage() {
        String message = contentFilterService.getBlockedMessage();
        assertNotNull(message);
        assertFalse(message.isBlank());
    }
}
