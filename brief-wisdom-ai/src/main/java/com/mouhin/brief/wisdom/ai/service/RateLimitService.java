package com.mouhin.brief.wisdom.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流服务（基于滑动窗口的简易限流）
 * <p>
 * 防止单个用户恶意刷接口，保护 AI 模型调用成本和服务稳定性。
 * <ul>
 *   <li>每用户每分钟最多请求数：{@link #MAX_REQUESTS_PER_MINUTE}</li>
 *   <li>每用户每天最多请求数：{@link #MAX_REQUESTS_PER_DAY}</li>
 * </ul>
 * 注：当前为内存级限流，重启后计数清零。分布式场景建议接入 Redis。
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * 每用户每分钟最大请求数
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    /**
     * 每用户每天最大请求数
     */
    private static final int MAX_REQUESTS_PER_DAY = 200;

    /**
     * 分钟级计数器：key = userId，value = 当前分钟的请求计数
     */
    private final Map<String, WindowCounter> minuteCounters = new ConcurrentHashMap<>();

    /**
     * 天级计数器：key = userId，value = 当天的请求计数
     */
    private final Map<String, WindowCounter> dayCounters = new ConcurrentHashMap<>();

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

        long now = System.currentTimeMillis();

        // 检查分钟级限制
        WindowCounter minuteCounter = minuteCounters.computeIfAbsent(userId, k -> new WindowCounter(now));
        if (!minuteCounter.isInWindow(now, 60_000L)) {
            minuteCounter.reset(now);
        }
        if (minuteCounter.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("[限流] 用户 {} 分钟级请求超限: {}/{}", userId, minuteCounter.getCount(), MAX_REQUESTS_PER_MINUTE);
            return true;
        }

        // 检查天级限制
        WindowCounter dayCounter = dayCounters.computeIfAbsent(userId, k -> new WindowCounter(now));
        if (!dayCounter.isInWindow(now, 86_400_000L)) {
            dayCounter.reset(now);
        }
        if (dayCounter.incrementAndGet() > MAX_REQUESTS_PER_DAY) {
            log.warn("[限流] 用户 {} 天级请求超限: {}/{}", userId, dayCounter.getCount(), MAX_REQUESTS_PER_DAY);
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

    /**
     * 滑动窗口计数器
     */
    private static class WindowCounter {
        private volatile long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        boolean isInWindow(long now, long windowSize) {
            return now - windowStart < windowSize;
        }

        void reset(long now) {
            this.windowStart = now;
            this.count.set(0);
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }
    }
}
