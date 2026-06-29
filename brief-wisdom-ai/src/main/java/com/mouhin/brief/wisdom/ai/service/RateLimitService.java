package com.mouhin.brief.wisdom.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 接口限流服务（基于 Redis 的滑动窗口限流）
 * <p>
 * 防止单个用户恶意刷接口，保护 AI 模型调用成本和服务稳定性。
 * <ul>
 *   <li>每用户每分钟最多请求数：{@link #MAX_REQUESTS_PER_MINUTE}</li>
 *   <li>每用户每天最多请求数：{@link #MAX_REQUESTS_PER_DAY}</li>
 * </ul>
 * 使用 Redis INCR + EXPIRE 实现分布式限流，支持多实例部署。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_REQUESTS_PER_DAY = 200;

    private static final String KEY_PREFIX = "bw:ratelimit:";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查用户是否被限流
     *
     * @param userId 用户ID
     * @return true 表示被限流，应拒绝请求
     */
    public boolean isRateLimited(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        // 检查分钟级限制
        String minuteKey = KEY_PREFIX + "m:" + userId;
        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        if (minuteCount != null && minuteCount == 1) {
            // 首次设置，过期时间 60 秒
            redisTemplate.expire(minuteKey, Duration.ofSeconds(65));
        }
        if (minuteCount != null && minuteCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("[限流] 用户 {} 分钟级请求超限: {}/{}", userId, minuteCount, MAX_REQUESTS_PER_MINUTE);
            return true;
        }

        // 检查天级限制
        String dayKey = KEY_PREFIX + "d:" + userId + ":" + LocalDateTime.now().format(DAY_FORMAT);
        Long dayCount = redisTemplate.opsForValue().increment(dayKey);
        if (dayCount != null && dayCount == 1) {
            // 首次设置，过期时间 25 小时（留 1 小时缓冲）
            redisTemplate.expire(dayKey, Duration.ofHours(25));
        }
        if (dayCount != null && dayCount > MAX_REQUESTS_PER_DAY) {
            log.warn("[限流] 用户 {} 天级请求超限: {}/{}", userId, dayCount, MAX_REQUESTS_PER_DAY);
            return true;
        }

        return false;
    }

    /**
     * 获取限流提示消息
     */
    public String getRateLimitMessage() {
        return "请求过于频繁，请稍后再试。每分钟最多 " + MAX_REQUESTS_PER_MINUTE + " 次，每天最多 " + MAX_REQUESTS_PER_DAY + " 次。";
    }
}
