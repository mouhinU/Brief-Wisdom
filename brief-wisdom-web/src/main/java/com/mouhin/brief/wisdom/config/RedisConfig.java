package com.mouhin.brief.wisdom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mouhin.brief.wisdom.constants.CachePrefix;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.mouhin.brief.wisdom.constants.CachePrefix.USER_PERMS_CACHE;

/**
 * Redis 配置类
 * <p>
 * 自定义 RedisTemplate 序列化策略 + Spring Cache 缓存管理。
 * <p>
 * Redis Key 统一命名规范（项目前缀 bw:，按业务域划分）：
 * <ul>
 *   <li>bw:menu:{...} — 菜单缓存</li>
 *   <li>bw:user:{...} — 用户/角色/权限缓存</li>
 *   <li>bw:resume:{...} — 简历缓存</li>
 *   <li>bw:ratelimit:{...} — 接口限流计数</li>
 *   <li>bw:lock:{name} — 分布式锁</li>
 *   <li>bw:session:{id} — 用户会话</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfig {


    /**
     * 缓存专用 ObjectMapper（共享实例，线程安全）
     * <p>
     * 配置要点：
     * - 启用默认类型信息（@class），用于反序列化时还原具体类型
     * - 注册 JavaTimeModule，LocalDateTime 序列化为 ISO 字符串而非数组
     * - 禁用时间戳格式，确保日期可读
     */
    private static ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    /**
     * 包装序列化器：确保顶层值始终携带 @class 类型信息
     * <p>
     * GenericJackson2JsonRedisSerializer 对顶层集合（如 ArrayList）不添加 @class，
     * 导致 Spring Cache 以 Object 类型反序列化时无法还原原始类型。
     * 此包装器将值先包装为 Object[] {value}，强制顶层也携带类型信息。
     */
    private static class TypePreservingSerializer implements RedisSerializer<Object> {
        private final GenericJackson2JsonRedisSerializer delegate;

        TypePreservingSerializer(ObjectMapper mapper) {
            this.delegate = new GenericJackson2JsonRedisSerializer(mapper);
        }

        @Override
        public byte[] serialize(Object value) throws SerializationException {
            if (value == null) return null;
            return delegate.serialize(new Object[]{value});
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) return null;
            Object[] wrapper = (Object[]) delegate.deserialize(bytes);
            return wrapper != null ? wrapper[0] : null;
        }
    }


    /**
     * 通用 RedisTemplate（Key=String, Value=JSON）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化（JSON）— 使用包装序列化器确保顶层类型信息
        RedisSerializer<Object> jsonSerializer = new TypePreservingSerializer(cacheObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate（用于简单的字符串操作）
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Spring Cache 管理器（基于 Redis）
     * <p>
     * Key 格式: {cacheName}::{key}（cacheName 已包含业务域前缀）
     * <p>
     * 不同缓存域设置不同的 TTL：
     * <ul>
     *   <li>bw:menu:tree: 菜单树，10 分钟（菜单变更不频繁）</li>
     *   <li>bw:menu:public: 公开菜单，10 分钟</li>
     *   <li>bw:user:roles: 用户角色，5 分钟（角色变更需较快生效）</li>
     *   <li>bw:user:perms: 用户权限，5 分钟</li>
     *   <li>bw:user:role: 角色信息，30 分钟</li>
     *   <li>bw:resume:experiences: 简历工作经历，30 分钟（简历数据极少变动）</li>
     * </ul>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置（Key 前缀直接使用 cacheName，因为 cacheName 已包含业务域前缀）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(cacheName -> cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new TypePreservingSerializer(cacheObjectMapper())))
                .disableCachingNullValues();

        // 各缓存域自定义 TTL
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put(CachePrefix.MENU_TREE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CachePrefix.MENU_PUBLIC_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CachePrefix.USER_ROLES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configMap.put(CachePrefix.USER_PERMS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configMap.put(CachePrefix.USER_ROLE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configMap.put(CachePrefix.MENU_ALL_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CachePrefix.USER_ROLE_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CachePrefix.RESUME_EXPERIENCES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configMap.put(CachePrefix.AI_MODEL_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configMap.put(CachePrefix.AI_SESSION_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();
    }
}
