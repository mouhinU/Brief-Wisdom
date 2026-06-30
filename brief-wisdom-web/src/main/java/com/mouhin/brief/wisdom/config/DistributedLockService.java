package com.mouhin.brief.wisdom.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务（基于 Redisson）
 * <p>
 * 提供编程式和声明式两种使用方式：
 * <ul>
 *   <li>编程式：直接调用 {@link #lock}, {@link #tryLock}, {@link #unlock} 方法</li>
 *   <li>声明式：使用 {@link DistributedLock} 注解 + AOP 切面自动加锁/释放</li>
 * </ul>
 */
/**
 * DistributedLockService
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_KEY_PREFIX = "bw:lock:";

    private final RedissonClient redissonClient;

    /**
     * 尝试获取锁并执行业务逻辑（非阻塞）
     *
     * @param lockKey  锁的标识
     * @param waitTime 等待获取锁的最长时间
     * @param leaseTime 锁的持有时间（自动释放）
     * @param unit     时间单位
     * @param action   要执行的业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值；获取锁失败返回 null
     */
    public <T> T tryLockAndExecute(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                log.warn("[分布式锁] 获取锁失败: {}", lockKey);
                return null;
            }
            try {
                return action.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取锁被中断: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 尝试获取锁并执行（默认等待 3 秒，持有 30 秒）
     */
    public <T> T tryLockAndExecute(String lockKey, Supplier<T> action) {
        return tryLockAndExecute(lockKey, 3, 30, TimeUnit.SECONDS, action);
    }

    /**
     * 尝试获取锁并执行（无返回值）
     */
    public void tryLockAndRun(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        tryLockAndExecute(lockKey, waitTime, leaseTime, unit, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁并执行（无返回值，默认等待 3 秒，持有 30 秒）
     */
    public void tryLockAndRun(String lockKey, Runnable action) {
        tryLockAndExecute(lockKey, 3, 30, TimeUnit.SECONDS, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 阻塞获取锁并执行
     *
     * @param lockKey   锁的标识
     * @param leaseTime 锁的持有时间（-1 表示启用看门狗自动续期）
     * @param unit      时间单位
     * @param action    要执行的业务逻辑
     * @param <T>       返回值类型
     * @return 业务逻辑的返回值
     */
    public <T> T lockAndExecute(String lockKey, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + lockKey);
        lock.lock(leaseTime, unit);
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 阻塞获取锁并执行（启用看门狗自动续期）
     */
    public <T> T lockAndExecute(String lockKey, Supplier<T> action) {
        return lockAndExecute(lockKey, -1, TimeUnit.SECONDS, action);
    }

    /**
     * 手动获取锁对象（用于高级场景）
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(LOCK_KEY_PREFIX + lockKey);
    }
}
