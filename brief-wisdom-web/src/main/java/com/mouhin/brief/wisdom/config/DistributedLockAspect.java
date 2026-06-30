package com.mouhin.brief.wisdom.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁 AOP 切面
 * <p>
 * 拦截 {@link DistributedLock} 注解标注的方法，自动加锁/释放。
 * 支持 SpEL 表达式动态解析锁的 Key。
 */
/**
 * DistributedLockAspect
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_KEY_PREFIX = "bw:lock:";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = resolveLockKey(joinPoint, distributedLock);
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!acquired) {
                if (distributedLock.failFast()) {
                    throw new RuntimeException(distributedLock.message());
                }
                log.warn("[分布式锁] 获取锁失败，方法未执行: {} -> {}", joinPoint.getSignature().toShortString(), lockKey);
                return null;
            }

            log.debug("[分布式锁] 获取锁成功: {} -> {}", joinPoint.getSignature().toShortString(), lockKey);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取锁被中断: {}", lockKey, e);
            if (distributedLock.failFast()) {
                throw new RuntimeException("获取锁被中断");
            }
            return null;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[分布式锁] 释放锁: {}", lockKey);
            }
        }
    }

    /**
     * 解析锁的 Key（支持 SpEL 表达式）
     */
    private String resolveLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String keyExpression = distributedLock.key();

        // 如果不包含 SpEL 表达式特征（# 或 $），直接返回
        if (!keyExpression.contains("#") && !keyExpression.contains("$")) {
            return keyExpression;
        }

        // 解析 SpEL 表达式
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Object value = PARSER.parseExpression(keyExpression).getValue(context);
        return value != null ? value.toString() : keyExpression;
    }
}
