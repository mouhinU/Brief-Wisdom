package com.mouhin.brief.wisdom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * <p>
 * 统一管理项目中的异步任务线程池。
 * 使用 Java 21 虚拟线程工厂，配合有界队列和拒绝策略，
 * 在保证高并发的同时避免资源耗尽。
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 项目通用异步线程池（基于虚拟线程）
     * <p>
     * 用于 SSE 流式响应、异步通知等场景。
     * 使用虚拟线程工厂，每个任务在轻量虚拟线程上执行，
     * 有界队列 + CallerRunsPolicy 保证任务不丢失。
     */
    @Bean("briefWisdomExecutor")
    public Executor briefWisdomExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("bw-vt-", 0)
                .factory();
        return new ThreadPoolExecutor(
                4,
                32,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                virtualThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
