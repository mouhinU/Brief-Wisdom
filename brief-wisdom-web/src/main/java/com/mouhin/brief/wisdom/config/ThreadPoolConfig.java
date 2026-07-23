package com.mouhin.brief.wisdom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 线程池配置类
 * <p>
 * 基于 JDK 21 虚拟线程，使用 newVirtualThreadPerTaskExecutor 为每个任务创建轻量虚拟线程。
 * 虚拟线程无需池化——创建成本极低（~几百字节栈），由 JVM 调度器高效管理。
 * <p>
 * 配合 spring.threads.virtual.enabled=true，Spring Boot 4 的 @Async 和 @Scheduled
 * 也自动使用虚拟线程，无需额外配置。
 *
 * @author Brief-Wisdom
 * @date 2026-07-05
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 项目通用异步线程池（JDK 21 虚拟线程）
     * <p>
     * 用于 SSE 流式响应、异步向量化、异步通知等场景。
     * 每个任务在独立虚拟线程上执行，无队列瓶颈、无池大小限制。
     * 若需背压控制，应在业务层使用 Semaphore 限流。
     */
    @Bean("briefWisdomExecutor")
    public Executor briefWisdomExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
