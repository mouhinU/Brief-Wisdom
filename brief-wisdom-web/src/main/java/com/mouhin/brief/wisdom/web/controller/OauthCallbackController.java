package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.service.AlipayAuthService;
import com.mouhin.brief.wisdom.web.service.DingtalkAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钉钉 & 支付宝扫码登录 Controller
 * <p>
 * 提供发起登录和回调处理接口，
 * 回调成功后写入 Session + Spring Security 上下文，重定向到 about.html。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OauthCallbackController {

    private static final String SESSION_USER_KEY = "AUTH_USER";
    private static final String SPRING_SECURITY_CONTEXT_KEY =
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
    private final DingtalkAuthService dingtalkAuthService;
    private final AlipayAuthService alipayAuthService;

    // ==================== 钉钉登录 ====================

    /**
     * 发起钉钉扫码登录
     *
     * @return { success: true, authorizeUrl: "https://login.dingtalk.com/..." }
     */
    @GetMapping("/auth/dingtalk/login")
    public ResponseEntity<Map<String, Object>> dingtalkLogin(HttpSession session) {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        session.setAttribute("dingtalk_state", state);

        String authorizeUrl = dingtalkAuthService.buildAuthorizeUrl(state);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("authorizeUrl", authorizeUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * 钉钉授权回调
     */
    @GetMapping("/auth/dingtalk/callback")
    public void dingtalkCallback(
            @RequestParam("authCode") String authCode,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("[钉钉登录] 收到回调, authCode={}, state={}", authCode, state);

        try {
            ChatUser user = dingtalkAuthService.handleDingtalkCallback(authCode);
            doLoginSuccess(user, request, response);
        } catch (Exception e) {
            log.error("[钉钉登录] 回调处理失败", e);
            response.sendRedirect("/?login_error=1&msg=" + URLEncode(e.getMessage()));
        }
    }

    // ==================== 支付宝登录 ====================

    /**
     * 发起支付宝扫码登录
     *
     * @return { success: true, authorizeUrl: "https://openauth.alipay.com/..." }
     */
    @GetMapping("/auth/alipay/login")
    public ResponseEntity<Map<String, Object>> alipayLogin(HttpSession session) {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        session.setAttribute("alipay_state", state);

        String authorizeUrl = alipayAuthService.buildAuthorizeUrl(state);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("authorizeUrl", authorizeUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * 支付宝授权回调
     */
    @GetMapping("/auth/alipay/callback")
    public void alipayCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("[支付宝登录] 收到回调, auth_code={}, state={}", authCode, state);

        try {
            ChatUser user = alipayAuthService.handleAlipayCallback(authCode);
            doLoginSuccess(user, request, response);
        } catch (Exception e) {
            log.error("[支付宝登录] 回调处理失败", e);
            response.sendRedirect("/?login_error=1&msg=" + URLEncode(e.getMessage()));
        }
    }

    // ==================== 公共方法 ====================

    /**
     * 登录成功后写入 Session + SecurityContext，重定向到 about.html
     */
    private void doLoginSuccess(ChatUser user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
        Map<String, String> details = new HashMap<>();
        details.put("nickname", user.getNickname());
        details.put("avatar", user.getAvatar());
        authToken.setDetails(details);
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        log.info("[OAuth] 登录成功, userId={}, nickname={}", user.getUserId(), user.getNickname());
        response.sendRedirect("/about.html");
    }

    /**
     * URL 编码辅助方法
     */
    private String URLEncode(String text) {
        try {
            return java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return text;
        }
    }
}
