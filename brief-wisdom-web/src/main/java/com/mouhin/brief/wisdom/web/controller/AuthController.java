package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.ai.service.RateLimitService;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.exception.AuthException;
import com.mouhin.brief.wisdom.exception.RateLimitException;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.system.service.*;
import com.mouhin.brief.wisdom.web.req.LoginRequest;
import com.mouhin.brief.wisdom.web.req.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证授权 Controller
 * <p>
 * 支持三种登录方式：
 * 1. 用户名 + 密码（POST /login）
 * 2. 手机号 + 验证码（POST /login/phone）
 * 3. SSO 令牌登录（GET /sso/validate）
 * <p>
 * 注册支持绑定手机号，绑定后用户级别为 vip。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证授权", description = "用户登录、注册、SSO相关接口")
public class AuthController {

    private final AuthService authService;
    private final RoleService roleService;
    private final RateLimitService rateLimitService;
    private final UserContextHelper userContextHelper;
    private final SmsService smsService;
    private final SsoTokenService ssoTokenService;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "使用用户名和密码注册新账号，可选绑定手机号")
    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String userId = userContextHelper.getCurrentUserId(httpRequest);
        if (rateLimitService.isRateLimited("register:" + userId)) {
            throw new RateLimitException("注册请求过于频繁，请稍后再试");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new AuthException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AuthException("密码不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new AuthException("密码长度不能少于6位");
        }

        // 如果提供了手机号，需要验证验证码
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (request.getSmsCode() == null || request.getSmsCode().isBlank()) {
                throw new AuthException("手机号注册需要提供验证码");
            }
            if (!smsService.verifyCode(request.getPhone(), request.getSmsCode())) {
                throw new AuthException("验证码错误或已过期");
            }
            return authService.registerWithPhone(
                    request.getUsername(), request.getPassword(),
                    request.getNickname(), request.getPhone());
        }

        return authService.register(request.getUsername(), request.getPassword(), request.getNickname());
    }

    /**
     * 用户登录（用户名 + 密码）
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录")
    @PostMapping("/login")
    public UserDTO login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitService.isRateLimited("login:" + clientIp)) {
            throw new RateLimitException("登录请求过于频繁，请稍后再试");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new AuthException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AuthException("密码不能为空");
        }
        UserDTO user = authService.login(request.getUsername(), request.getPassword());
        userContextHelper.loginSuccess(toChatUser(user), httpRequest);
        return user;
    }

    /**
     * 手机号 + 验证码登录
     */
    @Operation(summary = "手机号登录", description = "使用手机号和短信验证码登录，未注册的手机号自动创建账号")
    @PostMapping("/login/phone")
    public UserDTO loginByPhone(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitService.isRateLimited("login:" + clientIp)) {
            throw new RateLimitException("登录请求过于频繁，请稍后再试");
        }

        String phone = request.get("phone");
        String code = request.get("code");

        if (phone == null || phone.isBlank()) {
            throw new AuthException("手机号不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new AuthException("验证码不能为空");
        }

        UserDTO user = authService.loginByPhone(phone, code);
        userContextHelper.loginSuccess(toChatUser(user), httpRequest);
        return user;
    }

    /**
     * 发送短信验证码
     */
    @Operation(summary = "发送验证码", description = "向指定手机号发送短信验证码")
    @PostMapping("/sms/send")
    public Map<String, Object> sendSmsCode(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitService.isRateLimited("sms:" + clientIp)) {
            throw new RateLimitException("发送验证码过于频繁，请稍后再试");
        }

        String phone = request.get("phone");
        if (phone == null || phone.isBlank()) {
            throw new AuthException("手机号不能为空");
        }
        // 简单校验手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new AuthException("手机号格式不正确");
        }

        if (!smsService.canSend(phone)) {
            throw new RateLimitException("验证码发送过于频繁，请60秒后再试");
        }

        String code = smsService.sendVerificationCode(phone);
        if (code == null) {
            throw new AuthException("验证码发送失败，请稍后重试");
        }

        // 开发环境在日志中打印验证码（生产环境应删除此日志）
        log.debug("[短信验证码] phone={}, code={}（生产环境此验证码通过短信发送）", phone, code);
        return Map.of("success", true, "message", "验证码已发送");
    }

    /**
     * SSO 令牌验证 —— 供其他应用验证 SSO 令牌
     */
    @Operation(summary = "SSO令牌验证", description = "验证SSO令牌并返回用户信息，供接入SSO的其他应用调用")
    @GetMapping("/sso/validate")
    public Map<String, Object> ssoValidate(@RequestParam("token") String token) {
        Map<String, String> userInfo = ssoTokenService.validateToken(token);
        if (userInfo == null) {
            return Map.of("valid", false, "message", "令牌无效或已过期");
        }
        return Map.of("valid", true, "userId", userInfo.get("userId"), "username", userInfo.get("username"));
    }

    /**
     * SSO 获取令牌 —— 已登录用户获取 SSO 令牌
     */
    @Operation(summary = "获取SSO令牌", description = "已登录用户获取SSO令牌，用于跳转其他应用")
    @GetMapping("/sso/token")
    public Map<String, Object> ssoGetToken(HttpServletRequest httpRequest) {
        ChatUser user = (ChatUser) httpRequest.getSession().getAttribute(UserContextHelper.SESSION_USER_KEY);
        if (user == null) {
            return Map.of("success", false, "message", "未登录");
        }
        String token = ssoTokenService.generateToken(user.getUserId(), user.getUsername());
        return Map.of("success", true, "token", token);
    }

    /**
     * SSO 登录页面 URL
     */
    @Operation(summary = "SSO登录地址", description = "获取SSO登录页面URL，供其他应用跳转")
    @GetMapping("/sso/login-url")
    public Map<String, Object> ssoLoginUrl(@RequestParam("callback") String callback) {
        String loginUrl = ssoTokenService.getSsoLoginUrl(callback);
        return Map.of("loginUrl", loginUrl);
    }

    // ==================== 内部方法 ====================

    /**
     * UserDTO 转换为 ChatUser（用于写入 Session）
     */
    private ChatUser toChatUser(UserDTO dto) {
        ChatUser user = new ChatUser();
        user.setUserId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        return user;
    }

    /**
     * 手机号脱敏（中间4位用*替代）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
