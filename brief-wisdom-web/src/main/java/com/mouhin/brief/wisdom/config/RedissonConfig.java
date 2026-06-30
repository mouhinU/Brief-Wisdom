package com.mouhin.brief.wisdom.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 手动配置
 * <p>
 * 不使用 redisson-spring-boot-starter 的自动配置（它会注册 RedissonConnectionFactory，
 * 导致 DefaultedRedisConnection.pExpire() 无限递归 → StackOverflowError）。
 * <p>
 * 这里仅创建 RedissonClient 用于分布式锁，Redis 连接工厂由 Spring Boot 默认的 Lettuce 提供。
 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setPassword(password != null && !password.isEmpty() ? password : null);
        log.info("[Redisson] 初始化完成，连接地址: {}，数据库: {}", address, database);
        return Redisson.create(config);
    }
}
