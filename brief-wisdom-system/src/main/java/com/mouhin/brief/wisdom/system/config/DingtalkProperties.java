package com.mouhin.brief.wisdom.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉扫码登录配置
 * <p>
 * 使用钉钉新版 OAuth2.0 协议（企业内部应用）
 * <pre>
 * dingtalk:
 *   app-key: ${DINGTALK_APP_KEY}
 *   app-secret: ${DINGTALK_APP_SECRET}
 *   redirect-uri: ${DINGTALK_REDIRECT_URI}
 * </pre>
 */

/**
 * DingtalkProperties
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "dingtalk")
public class DingtalkProperties {

    /**
     * 钉钉应用 AppKey（即 client_id）
     */
    private String appKey;

    /**
     * 钉钉应用 AppSecret（即 client_secret）
     */
    private String appSecret;

    /**
     * 授权回调地址
     */
    private String redirectUri;

    /**
     * 钉钉授权页地址
     */
    private String authorizeUrl = "https://login.dingtalk.com/oauth2/auth";

    /**
     * 钉钉获取 token 接口
     */
    private String tokenUrl = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

    /**
     * 钉钉获取用户信息接口
     */
    private String userinfoUrl = "https://api.dingtalk.com/v1.0/contact/users/me";
}
