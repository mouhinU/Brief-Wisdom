package com.mouhin.brief.wisdom.ai.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 存储模块配置
 * <p>
 * 启用 {@link StorageProperties} 配置绑定。具体的 {@link StorageService} 实现
 * 由 {@code app.storage.type} 通过 {@code @ConditionalOnProperty} 自动装配
 * （local 或 minio）。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
