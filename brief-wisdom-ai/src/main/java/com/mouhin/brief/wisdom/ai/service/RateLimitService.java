package com.mouhin.brief.wisdom.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 接口限流服务（基于 Redis 的滑动窗口限流）
 * <p>
 * 使用 Redis INCR + EXPIRE 实现分布式限流，支持多实例部署。
 * 限流维度：
 * <ul>
 *   <li>秒级：每秒最多 {@link #MAX_REQUESTS_PER_SECOND} 次请求</li>
 *   <li>天级：每天最多 {@link #MAX_REQUESTS_PER_SECOND} × {@link #DAILY_MULTIPLIER} 次请求</li>
 * </ul>
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * 每秒最大请求数
     */
    private static final int MAX_REQUESTS_PER_SECOND = 20;

    /**
     * 天级限流乘数（天级限制 = MAX_REQUESTS_PER_SECOND × DAILY_MULTIPLIER）
     */
    private static final int DAILY_MULTIPLIER = 20 * 60 * 60 * 24;

    /**
     * 天级最大请求数（基于秒级限制计算）
     */
    private static final int MAX_REQUESTS_PER_DAY = MAX_REQUESTS_PER_SECOND * DAILY_MULTIPLIER;

    private static final String SECOND_KEY_PREFIX = "bw:ratelimit:s:";
    private static final String DAY_KEY_PREFIX = "bw:ratelimit:day:";

    private static final DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Lua 脚本：原子性执行 INCR + EXPIRE，避免进程崩溃导致 key 永不过期
     */
    private static final String INCR_WITH_EXPIRE_LUA =
            "local key = KEYS[1] " +
            "local expire = tonumber(ARGV[1]) " +
            "local count = redis.call('INCR', key) " +
            "if count == 1 then " +
            "  redis.call('EXPIRE', key, expire) " +
            "end " +
            "return count";

    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_SCRIPT;
    static {
        INCR_WITH_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        INCR_WITH_EXPIRE_SCRIPT.setScriptText(INCR_WITH_EXPIRE_LUA);
        INCR_WITH_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查用户是否被限流（使用 Lua 脚本保证 INCR+EXPIRE 原子性）
     *
     * @param userId 用户ID
     * @return true 表示被限流，应拒绝请求
     */
    public boolean isRateLimited(String userId) {
        LocalDateTime now = LocalDateTime.now();

        // 秒级限流（原子操作）
        String secondKey = SECOND_KEY_PREFIX + userId + ":" + now.format(SECOND_FORMATTER);
        Long secondCount = redisTemplate.execute(
                INCR_WITH_EXPIRE_SCRIPT,
                Collections.singletonList(secondKey),
                String.valueOf(5) // 5秒过期
        );
        if (secondCount != null && secondCount > MAX_REQUESTS_PER_SECOND) {
            log.warn("[限流] 用户 {} 秒级请求超限: {}/{}", userId, secondCount, MAX_REQUESTS_PER_SECOND);
            return true;
        }

        // 天级限流（原子操作）
        String dayKey = DAY_KEY_PREFIX + userId + ":" + now.format(DAY_FORMATTER);
        Long dayCount = redisTemplate.execute(
                INCR_WITH_EXPIRE_SCRIPT,
                Collections.singletonList(dayKey),
                String.valueOf(172800) // 2天=172800秒
        );
        if (dayCount != null && dayCount > MAX_REQUESTS_PER_DAY) {
            log.warn("[限流] 用户 {} 天级请求超限: {}/{}", userId, dayCount, MAX_REQUESTS_PER_DAY);
            return true;
        }

        return false;
    }

    /**
     * 获取限流提示消息
     *
     * @return 限流提示
     */
    public String getRateLimitMessage() {
        return "请求过于频繁，请稍后再试。每秒最多 " + MAX_REQUESTS_PER_SECOND + " 次，每天最多 " + MAX_REQUESTS_PER_DAY + " 次。";
    }
}
