package com.mouhin.brief.wisdom.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台扫码登录配置
 * <p>
 * 配置项在 application.yml 中：
 * <pre>
 * wechat:
 *   open:
 *     app-id: ${WECHAT_APP_ID}
 *     app-secret: ${WECHAT_APP_SECRET}
 *     redirect-uri: ${WECHAT_REDIRECT_URI}
 * </pre>
 */
/**
 * WechatProperties
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.open")
public class WechatProperties {

    /**
     * 微信开放平台 AppID
     */
    private String appId;

    /**
     * 微信开放平台 AppSecret
     */
    private String appSecret;

    /**
     * 授权回调地址
     */
    private String redirectUri;

    /**
     * 微信授权页地址
     */
    private String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect";

    /**
     * 微信 token 接口
     */
    private String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * 微信用户信息接口
     */
    private String userinfoUrl = "https://api.weixin.qq.com/sns/userinfo";
}
