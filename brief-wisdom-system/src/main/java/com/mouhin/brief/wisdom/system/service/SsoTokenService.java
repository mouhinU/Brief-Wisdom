package com.mouhin.brief.wisdom.system.service;

import java.util.Map;

/**
 * SSO 单点登录令牌服务接口
 * <p>
 * 提供基于 JWT 的 SSO 令牌生成和验证能力，支持跨应用单点登录。
 * 用户在任一应用登录后，可携带令牌访问其他接入 SSO 的应用。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
public interface SsoTokenService {

    /**
     * 为用户生成 SSO 令牌
     *
     * @param userId   用户ID（chat_user.user_id）
     * @param username 用户名
     * @return SSO 令牌字符串
     */
    String generateToken(String userId, String username);

    /**
     * 验证 SSO 令牌并提取用户信息
     *
     * @param token SSO 令牌
     * @return 用户信息 Map（包含 userId、username），无效令牌返回 null
     */
    Map<String, String> validateToken(String token);

    /**
     * 使令牌失效（用于登出）
     *
     * @param token SSO 令牌
     */
    void invalidateToken(String token);

    /**
     * 获取 SSO 登录页面 URL（供其他应用跳转）
     *
     * @param callbackUrl 登录成功后的回调地址
     * @return SSO 登录页面完整 URL
     */
    String getSsoLoginUrl(String callbackUrl);
}
