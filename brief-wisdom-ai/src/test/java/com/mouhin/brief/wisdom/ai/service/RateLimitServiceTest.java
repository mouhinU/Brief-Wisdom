package com.mouhin.brief.wisdom.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitService 接口限流服务单元测试
 * <p>
 * 使用 Mockito 模拟 StringRedisTemplate，验证秒级/天级限流逻辑。
 *
 * @author Brief-Wisdom
 * @date 2026-07-21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 限流服务测试")
class RateLimitServiceTest {

    private static final String USER_ID = "user-test-001";

    /**
     * 每秒最大请求数
     */
    private static final int MAX_REQUESTS_PER_SECOND = 20;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("isRateLimited - 正常请求（计数未超限）不应被限流")
    void testIsRateLimited_normalRequestUnderLimit() {
        // Lua 脚本返回 1（首次请求，远低于限制）
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(1L);

        boolean result = rateLimitService.isRateLimited(USER_ID);

        assertFalse(result);
        // 秒级和天级各调用一次
        verify(redisTemplate, times(2)).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("isRateLimited - 秒级请求超限应被限流")
    void testIsRateLimited_secondLimitExceeded() {
        // 秒级计数超过 20（Lua 脚本返回 21）
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(21L);

        boolean result = rateLimitService.isRateLimited(USER_ID);

        assertTrue(result);
        // 秒级超限后不再检查天级，只调用一次
        verify(redisTemplate, times(1)).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("isRateLimited - 天级请求超限应被限流")
    void testIsRateLimited_dayLimitExceeded() {
        // 第一次调用（秒级）返回正常值，第二次调用（天级）返回超限值
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(5L)
         .thenReturn(1440001L);

        boolean result = rateLimitService.isRateLimited(USER_ID);

        assertTrue(result);
        verify(redisTemplate, times(2)).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("isRateLimited - 秒级恰好等于限制值不应被限流")
    void testIsRateLimited_secondCountExactlyAtLimit() {
        // 秒级计数恰好等于 20（未超过），天级正常
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn((long) MAX_REQUESTS_PER_SECOND)
         .thenReturn(100L);

        boolean result = rateLimitService.isRateLimited(USER_ID);

        assertFalse(result);
    }

    @Test
    @DisplayName("getRateLimitMessage - 返回非空且包含限流数值的提示消息")
    void testGetRateLimitMessage_containsLimitInfo() {
        String message = rateLimitService.getRateLimitMessage();

        assertNotNull(message);
        assertFalse(message.isBlank());
        assertTrue(message.contains("20"));
        assertTrue(message.contains("请求过于频繁"));
    }
}
