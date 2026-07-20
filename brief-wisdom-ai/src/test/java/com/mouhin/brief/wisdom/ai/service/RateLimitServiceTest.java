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
import static org.mockito.Mockito.when;

/**
 * RateLimitService 限流服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 限流服务测试")
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("isRateLimited 首次请求不应被限流")
    void testIsRateLimited_firstRequest() {
        // Lua 脚本返回 1（首次请求）
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(1L);

        boolean result = rateLimitService.isRateLimited("user-001");
        assertFalse(result);
    }

    @Test
    @DisplayName("isRateLimited 超过秒级限制应被限流")
    void testIsRateLimited_exceedSecondLimit() {
        // 秒级计数超过 20（Lua 脚本返回 21）
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(21L);

        boolean result = rateLimitService.isRateLimited("user-001");
        assertTrue(result);
    }

    @Test
    @DisplayName("getRateLimitMessage 返回非空提示")
    void testGetRateLimitMessage() {
        String message = rateLimitService.getRateLimitMessage();
        assertNotNull(message);
        assertFalse(message.isBlank());
        assertTrue(message.contains("请求过于频繁"));
    }
}
