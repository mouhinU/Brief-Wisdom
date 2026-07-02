package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;

/**
 * 微信开放平台扫码登录服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface WechatAuthService {

    /**
     * 生成微信开放平台扫码授权 URL
     *
     * @param state 防 CSRF 随机字符串
     * @return 微信授权页面完整 URL
     */
    String buildAuthorizeUrl(String state);

    /**
     * 处理微信回调：code → access_token → 用户信息 → 本地用户
     *
     * @param code 微信授权码
     * @return 本地 ChatUser（已持久化）
     */
    ChatUser handleWechatCallback(String code);
}
