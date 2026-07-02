package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;

/**
 * 支付宝扫码登录服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface AlipayAuthService {

    /**
     * 生成支付宝扫码授权 URL
     *
     * @param state 防 CSRF 随机字符串
     * @return 支付宝授权页面完整 URL
     */
    String buildAuthorizeUrl(String state);

    /**
     * 处理支付宝回调：auth_code → access_token → 用户信息 → 本地用户
     *
     * @param authCode 支付宝授权码
     * @return 本地 ChatUser（已持久化）
     */
    ChatUser handleAlipayCallback(String authCode);
}
