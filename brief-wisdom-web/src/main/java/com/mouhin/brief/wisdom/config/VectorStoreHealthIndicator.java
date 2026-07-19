package com.mouhin.brief.wisdom.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Redis Vector Store 健康检查指示器
 * <p>
 * 将 LazyVectorStore 的可用性状态暴露到 Actuator /actuator/health 端点，
 * 便于监控 RediSearch 模块是否正常工作。
 *
 * @author Brief-Wisdom
 * @date 2026-07-16
 */
@Component
@RequiredArgsConstructor
public class VectorStoreHealthIndicator implements HealthIndicator {

    private final LazyVectorStore lazyVectorStore;

    @Override
    public Health health() {
        if (lazyVectorStore.isAvailable()) {
            return Health.up()
                    .withDetail("index", "brief-wisdom-knowledge")
                    .withDetail("status", "RediSearch module available")
                    .build();
        }
        return Health.down()
                    .withDetail("index", "brief-wisdom-knowledge")
                    .withDetail("status", "RediSearch module not available, vector search disabled")
                    .build();
    }
}
