package com.mouhin.brief.wisdom.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 声明式分布式锁注解
 * <p>
 * 加在方法上，AOP 切面自动加锁/释放。支持 SpEL 表达式动态生成锁的 Key。
 * <p>
 * 使用示例：
 * <pre>
 * // 固定锁 Key
 * &#64;DistributedLock(key = "order:create")
 * public void createOrder() { ... }
 *
 * // 动态锁 Key（使用方法参数）
 * &#64;DistributedLock(key = "'user:' + #userId")
 * public void updateUser(String userId) { ... }
 *
 * // 自定义等待/持有时间
 * &#64;DistributedLock(key = "payment", waitTime = 5, leaseTime = 60)
 * public void processPayment() { ... }
 * </pre>
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的标识，支持 SpEL 表达式
     * <p>
     * 例如：{@code "'order:' + #orderId"} 或固定值 {@code "createOrder"}
     */
    String key();

    /**
     * 等待获取锁的最长时间（秒）
     */
    long waitTime() default 3;

    /**
     * 锁的持有时间（秒），到期自动释放
     * <p>
     * 设为 -1 时启用看门狗自动续期（默认 30 秒续一次）
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败时的行为
     * <p>
     * true: 抛出异常<br>
     * false: 静默跳过，方法不执行
     */
    boolean failFast() default false;

    /**
     * 获取锁失败时的提示消息
     */
    String message() default "操作正在处理中，请稍后再试";
}
