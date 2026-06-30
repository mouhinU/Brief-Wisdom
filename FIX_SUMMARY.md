# StackOverflowError 修复总结

## 问题描述

应用启动后，在用户登录或访问时抛出 `StackOverflowError`：

```
jakarta.servlet.ServletException: Handler dispatch failed: java.lang.StackOverflowError
Caused by: java.lang.StackOverflowError: null
    at org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:220)
    at org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:220)
    at org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(DefaultedRedisConnection.java:220)
    ... (无限递归)
```

## 根本原因

1. **依赖冲突**：项目使用 `redisson-spring-boot-starter` 3.40.2，该 starter 会自动注册 `RedissonConnectionFactory` 作为 Spring Boot 的 `RedisConnectionFactory`

2. **方法实现缺陷**：`RedissonConnectionFactory` 创建的连接对象继承自 `DefaultedRedisConnection`，该接口的 `pExpire()` 默认方法存在无限递归 bug：
   ```java
   // DefaultedRedisConnection.java (spring-data-redis 3.5.5)
   default Boolean pExpire(byte[] key, long millis) {
       return pExpire(key, millis); // 递归调用自己！
   }
   ```

3. **触发场景**：Spring Session Redis 在设置会话 TTL 时调用 `pExpire()`，触发无限递归

## 解决方案

**核心思路**：让 Lettuce 处理 Redis 连接（用于 Session、Cache、RedisTemplate），Redisson 仅用于分布式锁。

### 修改内容

#### 1. pom.xml - 替换依赖

```xml
<!-- 修改前 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.40.2</version>
</dependency>

<!-- 修改后 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.40.2</version>
</dependency>
```

#### 2. 新增 RedissonConfig.java - 手动配置 RedissonClient

```java
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
```

## 修复效果

✅ **Redis 连接工厂**：Spring Boot 自动使用 Lettuce（`LettuceConnectionFactory`），正确实现 `pExpire()`  
✅ **分布式锁**：Redisson 客户端独立运行，不依赖 Spring Data Redis  
✅ **Spring Session**：正常工作，不再触发 StackOverflowError  
✅ **现有代码**：`DistributedLockService` 和 `DistributedLockAspect` 无需修改

## 验证步骤

1. 编译项目：`mvn clean compile`
2. 启动应用：`mvn spring-boot:run -pl brief-wisdom-web`
3. 访问登录页面并登录
4. 检查日志，确认无 StackOverflowError

## 技术细节

- **Spring Boot 版本**：3.5.7
- **spring-data-redis 版本**：3.5.5
- **Redisson 版本**：3.40.2
- **Redis 客户端**：Lettuce 6.6.0（Spring Boot 默认）

## 参考资料

- [Spring Data Redis DefaultedRedisConnection.pExpire 递归问题](https://github.com/spring-projects/spring-data-redis/issues/5132)
- [Redisson Spring Boot Starter 文档](https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks#145-spring-boot-starter)
