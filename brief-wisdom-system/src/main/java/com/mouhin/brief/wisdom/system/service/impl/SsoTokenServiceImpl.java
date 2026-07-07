package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.system.service.SsoTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SSO 单点登录令牌服务实现
 * <p>
 * 基于 HMAC-SHA256 签名的轻量级令牌方案（不引入 JWT 依赖）。
 * 令牌格式：base64(header).base64(payload).signature
 * 令牌存储在 Redis 中，支持主动失效和过期控制。
 * <p>
 * 生产环境建议升级为标准 JWT（io.jsonwebtoken）或 OAuth2 授权服务器。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoTokenServiceImpl implements SsoTokenService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 令牌有效期（小时） */
    @Value("${app.sso.token-expire-hours:24}")
    private int tokenExpireHours;

    /** 签名密钥（生产环境应通过配置中心管理） */
    @Value("${app.sso.secret:brief-wisdom-sso-secret-key-2026}")
    private String secret;

    /** SSO 服务地址 */
    @Value("${app.sso.server-url:http://localhost:8090}")
    private String serverUrl;

    private static final String TOKEN_KEY_PREFIX = "bw:sso:token:";

    @Override
    public String generateToken(String userId, String username) {
        // 生成令牌 ID
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + (long) tokenExpireHours * 3600 * 1000;

        // 构建 payload
        String payload = userId + "|" + username + "|" + expireAt;

        // 生成签名
        String signature = sign(tokenId + "." + payload);

        // 组装令牌
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tokenId.getBytes(StandardCharsets.UTF_8))
                + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature.getBytes(StandardCharsets.UTF_8));

        // 存储到 Redis
        String redisKey = TOKEN_KEY_PREFIX + tokenId;
        stringRedisTemplate.opsForValue().set(redisKey, userId, tokenExpireHours, TimeUnit.HOURS);

        log.info("SSO 令牌已生成: userId={}, expireHours={}", userId, tokenExpireHours);
        return token;
    }

    @Override
    public Map<String, String> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("SSO 令牌格式错误");
                return null;
            }

            String tokenId = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String signature = new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);

            // 验证签名
            String expectedSignature = sign(tokenId + "." + payload);
            if (!expectedSignature.equals(signature)) {
                log.warn("SSO 令牌签名验证失败");
                return null;
            }

            // 解析 payload
            String[] payloadParts = payload.split("\\|");
            if (payloadParts.length != 3) {
                log.warn("SSO 令牌 payload 格式错误");
                return null;
            }

            String userId = payloadParts[0];
            String username = payloadParts[1];
            long expireAt = Long.parseLong(payloadParts[2]);

            // 检查是否过期
            if (System.currentTimeMillis() > expireAt) {
                log.warn("SSO 令牌已过期: userId={}", userId);
                return null;
            }

            // 检查 Redis 中是否存在（支持主动失效）
            String redisKey = TOKEN_KEY_PREFIX + tokenId;
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(redisKey))) {
                log.warn("SSO 令牌已失效: userId={}", userId);
                return null;
            }

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("username", username);
            return userInfo;

        } catch (Exception e) {
            log.error("SSO 令牌验证异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidateToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String tokenId = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                stringRedisTemplate.delete(TOKEN_KEY_PREFIX + tokenId);
                log.info("SSO 令牌已失效: tokenId={}", tokenId);
            }
        } catch (Exception e) {
            log.warn("SSO 令牌失效处理异常: {}", e.getMessage());
        }
    }

    @Override
    public String getSsoLoginUrl(String callbackUrl) {
        return serverUrl + "/login.html?sso=1&callback=" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(callbackUrl.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HMAC-SHA256 签名
     */
    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }
}
