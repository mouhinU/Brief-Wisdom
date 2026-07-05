package com.mouhin.brief.wisdom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * <p>
 * 统一管理项目中的异步任务线程池，避免使用公共 ForkJoinPool。
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 项目通用异步线程池
     * <p>
     * 用于 SSE 流式响应、异步通知等场景。
     * 拒绝策略使用 CallerRunsPolicy，保证任务不丢失。
     */
    @Bean("briefWisdomExecutor")
    public Executor briefWisdomExecutor() {
        return new ThreadPoolExecutor(
                4,
                8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                r -> {
                    Thread t = new Thread(r, "brief-wisdom-pool-" + System.nanoTime() % 10000);
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
