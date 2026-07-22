package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.system.service.AlipayAuthService;
import com.mouhin.brief.wisdom.system.service.DingtalkAuthService;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钉钉 & 支付宝扫码登录 Controller
 * <p>
 * 提供发起登录和回调处理接口，
 * 回调成功后写入 Session + Spring Security 上下文，重定向到 about.html。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "OAuth认证", description = "钉钉与支付宝扫码登录")
public class OauthCallbackController {

    private static final String DINGTALK_STATE_KEY = "dingtalk_state";
    private static final String ALIPAY_STATE_KEY = "alipay_state";

    private final DingtalkAuthService dingtalkAuthService;
    private final AlipayAuthService alipayAuthService;
    private final UserContextHelper userContextHelper;

    // ==================== 钉钉登录 ====================

    /**
     * 发起钉钉扫码登录
     *
     * @return { success: true, authorizeUrl: "https://login.dingtalk.com/..." }
     */
    @GetMapping("/auth/dingtalk/login")
    @Operation(summary = "发起钉钉扫码登录")
    public ResponseEntity<Map<String, Object>> dingtalkLogin(HttpSession session) {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        session.setAttribute("dingtalk_state", state);

        String authorizeUrl = dingtalkAuthService.buildAuthorizeUrl(state);
        Map<String, Object> result = new HashMap<>(4);
        result.put("success", true);
        result.put("authorizeUrl", authorizeUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * 钉钉授权回调
     */
    @GetMapping("/auth/dingtalk/callback")
    @Operation(summary = "钉钉授权回调")
    public void dingtalkCallback(
            @RequestParam("authCode") String authCode,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("[钉钉登录] 收到回调, authCode={}, state={}", authCode, state);

        try {
            // 校验 state 防止 CSRF
            HttpSession session = request.getSession(true);
            if (!userContextHelper.validateOAuthState(session, DINGTALK_STATE_KEY, state)) {
                log.warn("[钉钉登录] state 校验失败，拒绝登录");
                response.sendRedirect("/?login_error=1");
                return;
            }

            ChatUser user = dingtalkAuthService.handleDingtalkCallback(authCode);
            userContextHelper.loginSuccess(user, request);
            response.sendRedirect("/about.html");
        } catch (Exception e) {
            log.error("[钉钉登录] 回调处理失败", e);
            response.sendRedirect("/?login_error=1");
        }
    }

    // ==================== 支付宝登录 ====================

    /**
     * 发起支付宝扫码登录
     *
     * @return { success: true, authorizeUrl: "https://openauth.alipay.com/..." }
     */
    @GetMapping("/auth/alipay/login")
    @Operation(summary = "发起支付宝扫码登录")
    public ResponseEntity<Map<String, Object>> alipayLogin(HttpSession session) {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        session.setAttribute("alipay_state", state);

        String authorizeUrl = alipayAuthService.buildAuthorizeUrl(state);
        Map<String, Object> result = new HashMap<>(4);
        result.put("success", true);
        result.put("authorizeUrl", authorizeUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * 支付宝授权回调
     */
    @GetMapping("/auth/alipay/callback")
    @Operation(summary = "支付宝授权回调")
    public void alipayCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("[支付宝登录] 收到回调, auth_code={}, state={}", authCode, state);

        try {
            // 校验 state 防止 CSRF
            HttpSession session = request.getSession(true);
            if (!userContextHelper.validateOAuthState(session, ALIPAY_STATE_KEY, state)) {
                log.warn("[支付宝登录] state 校验失败，拒绝登录");
                response.sendRedirect("/?login_error=1");
                return;
            }

            ChatUser user = alipayAuthService.handleAlipayCallback(authCode);
            userContextHelper.loginSuccess(user, request);
            response.sendRedirect("/about.html");
        } catch (Exception e) {
            log.error("[支付宝登录] 回调处理失败", e);
            response.sendRedirect("/?login_error=1");
        }
    }

    /**
     * URL 编码辅助方法
     */
    private String urlEncode(String text) {
        try {
            return java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return text;
        }
    }
}
