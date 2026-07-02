package com.mouhin.brief.wisdom.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import com.mouhin.brief.wisdom.persistence.repository.UserOauthRepository;
import com.mouhin.brief.wisdom.system.config.DingtalkProperties;
import com.mouhin.brief.wisdom.system.service.DingtalkAuthService;
import com.mouhin.brief.wisdom.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钉钉扫码登录服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingtalkAuthServiceImpl implements DingtalkAuthService {

    public static final String PROVIDER_DINGTALK = "dingtalk";

    private final DingtalkProperties dingtalkProperties;
    private final RestTemplate restTemplate;
    private final ChatUserRepository chatUserRepository;
    private final UserOauthRepository userOauthRepository;
    private final RoleService roleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成钉钉扫码授权 URL
     */
    @Override
    public String buildAuthorizeUrl(String state) {
        String url = dingtalkProperties.getAuthorizeUrl()
                + "?client_id=" + dingtalkProperties.getAppKey()
                + "&redirect_uri=" + URLEncoder.encode(dingtalkProperties.getRedirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=openid"
                + "&prompt=consent"
                + "&state=" + state;
        log.info("[钉钉登录] 生成授权 URL: {}", url);
        return url;
    }

    /**
     * 处理钉钉回调：authCode → access_token → 用户信息 → 本地用户
     */
    @Override
    @Transactional
    public ChatUser handleDingtalkCallback(String authCode) {
        // 1. authCode 换 access_token
        JsonNode tokenResp = fetchAccessToken(authCode);
        String accessToken = tokenResp.get("accessToken").asText();
        log.info("[钉钉登录] 获取到 access_token");

        // 2. access_token 获取钉钉用户信息
        JsonNode userInfo = fetchUserInfo(accessToken);
        String openid = userInfo.has("unionId") ? userInfo.get("unionId").asText() : userInfo.get("openId").asText();
        String nickname = userInfo.has("nick") ? userInfo.get("nick").asText() : null;
        String avatar = userInfo.has("avatarUrl") ? userInfo.get("avatarUrl").asText() : null;
        String unionId = userInfo.has("unionId") ? userInfo.get("unionId").asText() : null;
        log.info("[钉钉登录] 获取到用户信息: nickname={}", nickname);

        // 3. 通过 user_oauth 表查找绑定关系
        UserOauth oauth = userOauthRepository.findByProviderAndOpenid(PROVIDER_DINGTALK, openid);

        if (oauth != null) {
            ChatUser user = chatUserRepository.findByUserId(oauth.getUserId());
            if (user == null) {
                throw new RuntimeException("[钉钉登录] 用户不存在，请联系管理员");
            }
            oauth.setNickname(nickname);
            oauth.setAvatar(avatar);
            oauth.setUnionid(unionId);
            userOauthRepository.update(oauth);
            if (user.getNickname() == null || user.getNickname().isEmpty()) {
                user.setNickname(nickname);
            }
            if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
                user.setAvatar(avatar);
                chatUserRepository.update(user);
            }
            log.info("[钉钉登录] 老用户登录成功: userId={}, nickname={}", user.getUserId(), user.getNickname());
            return user;
        } else {
            String userId = UUID.randomUUID().toString();

            ChatUser user = new ChatUser();
            user.setUserId(userId);
            user.setUsername("dt_" + openid.substring(0, Math.min(16, openid.length())));
            user.setNickname(nickname);
            user.setAvatar(avatar);
            chatUserRepository.save(user);

            UserOauth newOauth = new UserOauth();
            newOauth.setUserId(userId);
            newOauth.setProvider(PROVIDER_DINGTALK);
            newOauth.setOpenid(openid);
            newOauth.setUnionid(unionId);
            newOauth.setNickname(nickname);
            newOauth.setAvatar(avatar);
            userOauthRepository.save(newOauth);

            // 分配默认角色（normal 普通用户）
            roleService.assignDefaultRole(userId);

            log.info("[钉钉登录] 新用户注册并绑定成功: userId={}, openid={}", userId, openid);
            return user;
        }
    }

    private JsonNode fetchAccessToken(String authCode) {
        String url = dingtalkProperties.getTokenUrl();
        try {
            Map<String, String> body = new HashMap<>();
            body.put("clientId", dingtalkProperties.getAppKey());
            body.put("clientSecret", dingtalkProperties.getAppSecret());
            body.put("code", authCode);
            body.put("grantType", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            String responseBody = restTemplate.postForObject(url, request, String.class);
            JsonNode json = objectMapper.readTree(responseBody);
            if (!json.has("accessToken")) {
                throw new RuntimeException("[钉钉登录] 获取 access_token 失败: " + responseBody);
            }
            return json;
        } catch (Exception e) {
            log.error("[钉钉登录] 调用 token 接口异常", e);
            throw new RuntimeException("[钉钉登录] 获取 access_token 异常: " + e.getMessage());
        }
    }

    private JsonNode fetchUserInfo(String accessToken) {
        String url = dingtalkProperties.getUserinfoUrl();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-acs-dingtalk-access-token", accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String responseBody = restTemplate.postForObject(url, request, String.class);
            JsonNode json = objectMapper.readTree(responseBody);
            if (json.has("code") && !json.get("code").asText().equals("0")) {
                throw new RuntimeException("[钉钉登录] 获取用户信息失败: " + json.get("message").asText());
            }
            return json;
        } catch (Exception e) {
            log.error("[钉钉登录] 调用 userinfo 接口异常", e);
            throw new RuntimeException("[钉钉登录] 获取用户信息异常: " + e.getMessage());
        }
    }
}
