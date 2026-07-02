package com.mouhin.brief.wisdom.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import com.mouhin.brief.wisdom.persistence.repository.UserOauthRepository;
import com.mouhin.brief.wisdom.system.config.AlipayProperties;
import com.mouhin.brief.wisdom.system.service.AlipayAuthService;
import com.mouhin.brief.wisdom.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支付宝扫码登录服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayAuthServiceImpl implements AlipayAuthService {

    public static final String PROVIDER_ALIPAY = "alipay";
    private static final String SIGN_TYPE = "RSA2";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayProperties alipayProperties;
    private final RestTemplate restTemplate;
    private final ChatUserRepository chatUserRepository;
    private final UserOauthRepository userOauthRepository;
    private final RoleService roleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成支付宝扫码授权 URL
     */
    @Override
    public String buildAuthorizeUrl(String state) {
        String url = alipayProperties.getGatewayUrl()
                + "?" + "app_id=" + alipayProperties.getAppId()
                + "&scope=auth_user"
                + "&redirect_uri=" + URLEncoder.encode(alipayProperties.getRedirectUri(), StandardCharsets.UTF_8)
                + "&state=" + state;
        log.info("[支付宝登录] 生成授权 URL: {}", url);
        return url;
    }

    /**
     * 处理支付宝回调：auth_code → access_token → 用户信息 → 本地用户
     */
    @Override
    @Transactional
    public ChatUser handleAlipayCallback(String authCode) {
        // 1. auth_code 换取 access_token
        JsonNode tokenResp = fetchAccessToken(authCode);
        String accessToken = tokenResp.get("access_token").asText();
        String userId = tokenResp.get("user_id").asText();
        log.info("[支付宝登录] 获取到 access_token, user_id={}", userId);

        // 2. access_token 获取支付宝用户信息
        JsonNode userInfo = fetchUserInfo(accessToken);
        String openid = userId;
        String nickname = userInfo.has("nick_name") ? userInfo.get("nick_name").asText() : null;
        String avatar = userInfo.has("avatar") ? userInfo.get("avatar").asText() : null;
        log.info("[支付宝登录] 获取到用户信息: nickname={}", nickname);

        // 3. 通过 user_oauth 表查找绑定关系
        UserOauth oauth = userOauthRepository.findByProviderAndOpenid(PROVIDER_ALIPAY, openid);

        if (oauth != null) {
            ChatUser user = chatUserRepository.findByUserId(oauth.getUserId());
            if (user == null) {
                throw new RuntimeException("[支付宝登录] 用户不存在，请联系管理员");
            }
            oauth.setNickname(nickname);
            oauth.setAvatar(avatar);
            userOauthRepository.update(oauth);
            if (user.getNickname() == null || user.getNickname().isEmpty()) {
                user.setNickname(nickname);
            }
            if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
                user.setAvatar(avatar);
                chatUserRepository.update(user);
            }
            log.info("[支付宝登录] 老用户登录成功: userId={}, nickname={}", user.getUserId(), user.getNickname());
            return user;
        } else {
            String newUserId = UUID.randomUUID().toString();

            ChatUser user = new ChatUser();
            user.setUserId(newUserId);
            user.setUsername("ap_" + openid.substring(0, Math.min(16, openid.length())));
            user.setNickname(nickname);
            user.setAvatar(avatar);
            chatUserRepository.save(user);

            UserOauth newOauth = new UserOauth();
            newOauth.setUserId(newUserId);
            newOauth.setProvider(PROVIDER_ALIPAY);
            newOauth.setOpenid(openid);
            newOauth.setNickname(nickname);
            newOauth.setAvatar(avatar);
            userOauthRepository.save(newOauth);

            // 分配默认角色（normal 普通用户）
            roleService.assignDefaultRole(newUserId);

            log.info("[支付宝登录] 新用户注册并绑定成功: userId={}, openid={}", newUserId, openid);
            return user;
        }
    }

    private JsonNode fetchAccessToken(String authCode) {
        try {
            Map<String, String> params = buildCommonParams("alipay.system.oauth.token");
            params.put("code", authCode);
            params.put("grant_type", "authorization_code");

            String signContent = buildSignContent(params);
            params.put("sign", rsaSign(signContent));

            String responseBody = callGateway(params);
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode response = json.get("alipay_system_oauth_token_response");
            if (response == null || response.has("code") && !response.get("code").asText().equals("10000")) {
                String errMsg = response != null && response.has("sub_msg") ? response.get("sub_msg").asText() : responseBody;
                throw new RuntimeException("[支付宝登录] 获取 access_token 失败: " + errMsg);
            }
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[支付宝登录] 调用 token 接口异常", e);
            throw new RuntimeException("[支付宝登录] 获取 access_token 异常: " + e.getMessage());
        }
    }

    private JsonNode fetchUserInfo(String accessToken) {
        try {
            Map<String, String> params = buildCommonParams("alipay.user.info.share");
            params.put("auth_token", accessToken);

            String signContent = buildSignContent(params);
            params.put("sign", rsaSign(signContent));

            String responseBody = callGateway(params);
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode response = json.get("alipay_user_info_share_response");
            if (response == null || response.has("code") && !response.get("code").asText().equals("10000")) {
                String errMsg = response != null && response.has("sub_msg") ? response.get("sub_msg").asText() : responseBody;
                throw new RuntimeException("[支付宝登录] 获取用户信息失败: " + errMsg);
            }
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[支付宝登录] 调用 userinfo 接口异常", e);
            throw new RuntimeException("[支付宝登录] 获取用户信息异常: " + e.getMessage());
        }
    }

    private Map<String, String> buildCommonParams(String method) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", alipayProperties.getAppId());
        params.put("method", method);
        params.put("charset", "utf-8");
        params.put("sign_type", SIGN_TYPE);
        params.put("timestamp", LocalDateTime.now().format(DT_FMT));
        params.put("version", "1.0");
        params.put("format", "JSON");
        return params;
    }

    private String buildSignContent(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private String rsaSign(String content) {
        try {
            String privateKeyStr = alipayProperties.getPrivateKey();
            privateKeyStr = privateKeyStr
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("[支付宝登录] RSA 签名失败", e);
            throw new RuntimeException("[支付宝登录] 签名失败: " + e.getMessage());
        }
    }

    private String callGateway(Map<String, String> params) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        params.forEach(formData::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        return restTemplate.postForObject(alipayProperties.getGatewayUrl(), request, String.class);
    }
}
