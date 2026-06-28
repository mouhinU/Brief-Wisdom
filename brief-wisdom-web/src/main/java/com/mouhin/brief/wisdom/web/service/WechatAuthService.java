package com.mouhin.brief.wisdom.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.config.WechatProperties;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import com.mouhin.brief.wisdom.persistence.repository.UserOauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 微信开放平台扫码登录服务
 * <p>
 * 流程：
 * <ol>
 *   <li>生成微信授权 URL，前端跳转</li>
 *   <li>用户扫码授权后，微信回调带 code</li>
 *   <li>用 code 换取 access_token + openid</li>
 *   <li>用 access_token + openid 获取用户信息</li>
 *   <li>通过 user_oauth 表查找绑定关系，找到则登录，未找到则自动注册并绑定</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatAuthService {

    public static final String PROVIDER_WECHAT = "wechat";

    private final WechatProperties wechatProperties;
    private final RestTemplate restTemplate;
    private final ChatUserRepository chatUserRepository;
    private final UserOauthRepository userOauthRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成微信开放平台扫码授权 URL
     *
     * @param state 防 CSRF 随机字符串
     * @return 微信授权页面完整 URL
     */
    public String buildAuthorizeUrl(String state) {
        String url = wechatProperties.getAuthorizeUrl()
                + "?appid=" + wechatProperties.getAppId()
                + "&redirect_uri=" + URLEncoder.encode(wechatProperties.getRedirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state
                + "#wechat_redirect";
        log.info("[微信登录] 生成授权 URL: {}", url);
        return url;
    }

    /**
     * 处理微信回调：code → access_token → 用户信息 → 本地用户
     *
     * @param code 微信授权码
     * @return 本地 ChatUser（已持久化）
     */
    @Transactional
    public ChatUser handleWechatCallback(String code) {
        // 1. code 换 access_token
        JsonNode tokenResp = fetchAccessToken(code);
        String accessToken = tokenResp.get("access_token").asText();
        String openid = tokenResp.get("openid").asText();
        String unionid = tokenResp.has("unionid") ? tokenResp.get("unionid").asText() : null;
        log.info("[微信登录] 获取到 access_token, openid={}, unionid={}", openid, unionid);

        // 2. access_token + openid 获取微信用户信息
        JsonNode userInfo = fetchUserInfo(accessToken, openid);
        String wxNickname = userInfo.has("nickname") ? userInfo.get("nickname").asText() : null;
        String wxAvatar = userInfo.has("headimgurl") ? userInfo.get("headimgurl").asText() : null;
        log.info("[微信登录] 获取到用户信息: nickname={}", wxNickname);

        // 3. 通过 user_oauth 表查找绑定关系
        UserOauth oauth = userOauthRepository.findByProviderAndOpenid(PROVIDER_WECHAT, openid);

        if (oauth != null) {
            // 已绑定：直接登录，更新 OAuth 记录
            ChatUser user = chatUserRepository.findByUserId(oauth.getUserId());
            if (user == null) {
                throw new RuntimeException("[微信登录] 用户不存在，请联系管理员");
            }
            // 更新 OAuth 绑定信息
            oauth.setNickname(wxNickname);
            oauth.setAvatar(wxAvatar);
            oauth.setUnionid(unionid);
            userOauthRepository.update(oauth);
            // 若 chat_user 没有昵称/头像，用 OAuth 信息补全
            if (user.getNickname() == null || user.getNickname().isEmpty()) {
                user.setNickname(wxNickname);
            }
            if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
                user.setAvatar(wxAvatar);
                chatUserRepository.update(user);
            }
            log.info("[微信登录] 老用户登录成功: userId={}, nickname={}", user.getUserId(), user.getNickname());
            return user;
        } else {
            // 未绑定：自动注册新用户 + 写入 OAuth 绑定
            String userId = UUID.randomUUID().toString();

            ChatUser user = new ChatUser();
            user.setUserId(userId);
            user.setUsername("wx_" + openid);
            user.setNickname(wxNickname);
            user.setAvatar(wxAvatar);
            chatUserRepository.save(user);

            UserOauth newOauth = new UserOauth();
            newOauth.setUserId(userId);
            newOauth.setProvider(PROVIDER_WECHAT);
            newOauth.setOpenid(openid);
            newOauth.setUnionid(unionid);
            newOauth.setNickname(wxNickname);
            newOauth.setAvatar(wxAvatar);
            userOauthRepository.save(newOauth);

            log.info("[微信登录] 新用户注册并绑定成功: userId={}, openid={}", userId, openid);
            return user;
        }
    }

    /**
     * 用 code 换取 access_token
     */
    private JsonNode fetchAccessToken(String code) {
        String url = wechatProperties.getTokenUrl()
                + "?appid=" + wechatProperties.getAppId()
                + "&secret=" + wechatProperties.getAppSecret()
                + "&code=" + code
                + "&grant_type=authorization_code";
        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.has("errcode")) {
                throw new RuntimeException("[微信登录] 获取 access_token 失败: " + json.get("errmsg").asText());
            }
            return json;
        } catch (Exception e) {
            log.error("[微信登录] 调用 token 接口异常", e);
            throw new RuntimeException("[微信登录] 获取 access_token 异常: " + e.getMessage());
        }
    }

    /**
     * 用 access_token + openid 获取微信用户信息
     */
    private JsonNode fetchUserInfo(String accessToken, String openid) {
        String url = wechatProperties.getUserinfoUrl()
                + "?access_token=" + accessToken
                + "&openid=" + openid
                + "&lang=zh_CN";
        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.has("errcode")) {
                throw new RuntimeException("[微信登录] 获取用户信息失败: " + json.get("errmsg").asText());
            }
            return json;
        } catch (Exception e) {
            log.error("[微信登录] 调用 userinfo 接口异常", e);
            throw new RuntimeException("[微信登录] 获取用户信息异常: " + e.getMessage());
        }
    }
}
