package com.mouhin.brief.wisdom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝扫码登录配置
 * <p>
 * 使用支付宝网关接口（alipay.user.info.share）
 * <pre>
 * alipay:
 *   app-id: ${ALIPAY_APP_ID}
 *   private-key: ${ALIPAY_PRIVATE_KEY}
 *   alipay-public-key: ${ALIPAY_PUBLIC_KEY}
 *   redirect-uri: ${ALIPAY_REDIRECT_URI}
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 支付宝应用 APPID */
    private String appId;

    /** 应用私钥（RSA2） */
    private String privateKey;

    /** 支付宝公钥（RSA2） */
    private String alipayPublicKey;

    /** 授权回调地址 */
    private String redirectUri;

    /** 支付宝网关地址 */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
}
