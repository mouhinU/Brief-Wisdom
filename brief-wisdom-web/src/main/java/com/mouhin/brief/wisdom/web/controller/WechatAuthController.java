package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.system.service.RoleService;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import com.mouhin.brief.wisdom.system.service.WechatAuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 微信扫码登录 + 认证状态 Controller
 */
/**
 * WechatAuthController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WechatAuthController {

    /**
     * Spring Security 上下文存储到 Session 的 Key
     */
    private static final String SPRING_SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
    private final WechatAuthService wechatAuthService;
    private final RoleService roleService;

    /**
     * 发起微信扫码登录
     * <p>
     * 前端调用此接口获取微信授权 URL，然后跳转到微信扫码页面。
     *
     * @return { authorizeUrl: "https://open.weixin.qq.com/..." }
     */
    @GetMapping("/auth/wechat/login")
    public ResponseEntity<Map<String, Object>> wechatLogin(HttpSession session) {
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        session.setAttribute("wechat_state", state);

        String authorizeUrl = wechatAuthService.buildAuthorizeUrl(state);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("authorizeUrl", authorizeUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * 微信授权回调
     * <p>
     * 用户扫码后，微信将 code 回调至此接口。
     * 处理流程：code → access_token → 用户信息 → 写入 Session → 重定向到 about.html
     */
    @GetMapping("/auth/wechat/callback")
    public void wechatCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("[微信登录] 收到回调, code={}, state={}", code, state);

        try {
            // 1. 处理微信登录，获取本地用户
            ChatUser user = wechatAuthService.handleWechatCallback(code);

            // 2. 写入 HTTP Session
            HttpSession session = request.getSession(true);
            session.setAttribute(UserContextHelper.SESSION_USER_KEY, user);

            // 3. 加载用户角色，转换为 Spring Security 权限
            List<String> roleKeys = roleService.getUserRoleKeys(user.getUserId());
            List<SimpleGrantedAuthority> authorities = roleKeys.stream()
                    .map(key -> new SimpleGrantedAuthority("ROLE_" + key))
                    .collect(Collectors.toList());

            // 4. 设置并持久化 Spring Security 认证上下文
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user.getUserId(),
                            null,
                            authorities
                    );
            Map<String, String> details = new HashMap<>();
            details.put("nickname", user.getNickname());
            details.put("avatar", user.getAvatar());
            authToken.setDetails(details);
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);
            // 将 SecurityContext 写入 Session，重启后仍然有效
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

            log.info("[微信登录] 登录成功, userId={}, nickname={}, roles={}", user.getUserId(), user.getNickname(), roleKeys);

            // 4. 重定向到关于我页面
            response.sendRedirect("/about.html");

        } catch (Exception e) {
            log.error("[微信登录] 回调处理失败", e);
            // 登录失败，重定向到首页并附带错误参数
            response.sendRedirect("/?login_error=1&msg=" + e.getMessage());
        }
    }

    /**
     * 检查登录状态（前端 JS 调用）
     *
     * @return { loggedIn: true/false, user: {...} }
     */
    @GetMapping("/api/auth/status")
    public ResponseEntity<Map<String, Object>> authStatus(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        if (session != null) {
            ChatUser user = (ChatUser) session.getAttribute(UserContextHelper.SESSION_USER_KEY);
            if (user != null) {
                result.put("loggedIn", true);
                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("userId", user.getUserId());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                result.put("user", userInfo);
                // 返回用户角色 Key 列表，供前端权限校验
                List<String> roles = roleService.getUserRoleKeys(user.getUserId());
                result.put("roles", roles);
                // 返回用户权限标识列表（super_admin 为 null 表示拥有所有权限）
                List<String> permissions = roleService.getUserPermissions(user.getUserId());
                result.put("permissions", permissions);
                result.put("isSuperAdmin", permissions == null);
                return ResponseEntity.ok(result);
            }
        }
        result.put("loggedIn", false);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前登录用户详细信息
     * <p>
     * 此接口需要登录才能访问（SecurityConfig 配置）
     */
    @GetMapping("/api/auth/user")
    public ResponseEntity<Map<String, Object>> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        ChatUser user = session != null ? (ChatUser) session.getAttribute(UserContextHelper.SESSION_USER_KEY) : null;
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "未登录"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        result.put("user", userInfo);
        return ResponseEntity.ok(result);
    }

    /**
     * 退出登录
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        log.info("[登录] 用户已退出");
        return ResponseEntity.ok(Map.of("success", true, "message", "已退出登录"));
    }
}
