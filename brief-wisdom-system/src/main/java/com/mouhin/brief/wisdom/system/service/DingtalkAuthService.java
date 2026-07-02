package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;

/**
 * 钉钉扫码登录服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface DingtalkAuthService {

    /**
     * 生成钉钉扫码授权 URL
     *
     * @param state 防 CSRF 随机字符串
     * @return 钉钉授权页面完整 URL
     */
    String buildAuthorizeUrl(String state);

    /**
     * 处理钉钉回调：authCode → access_token → 用户信息 → 本地用户
     *
     * @param authCode 钉钉授权码
     * @return 本地 ChatUser（已持久化）
     */
    ChatUser handleDingtalkCallback(String authCode);
}
