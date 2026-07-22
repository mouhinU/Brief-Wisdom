package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.exception.SystemSettingsException;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户上下文工具类
 * <p>
 * 所有 Controller 都可以注入此组件获取当前登录用户信息。
 * 未登录时基于客户端 IP 生成唯一访客指纹作为 userId。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextHelper {

    /**
     * Session 中存储用户信息的 Key
     */
    public static final String SESSION_USER_KEY = "SESSION_USER";

    /**
     * 访客用户ID前缀
     */
    private static final String GUEST_PREFIX = "guest-";

    private static final String SPRING_SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    private final RoleService roleService;

    /**
     * 获取当前登录用户的 userId
     * <p>
     * 优先从 Session 中获取，其次从 Spring Security 上下文获取，
     * 都没有则基于客户端 IP 生成唯一访客指纹。
     *
     * @return 当前用户ID，不为 null
     */
    public String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return generateFallbackGuestId();
        }
        return getCurrentUserId(request);
    }

    /**
     * 获取当前登录用户的 userId（指定 request）
     * <p>
     * 未登录时基于客户端 IP 生成唯一访客指纹，
     * 同一设备的未登录访客始终获得相同的 userId。
     *
     * @param httpRequest HTTP 请求
     * @return 当前用户ID，不为 null
     */
    public String getCurrentUserId(HttpServletRequest httpRequest) {
        // 1. 优先从 Session 获取（已登录用户）
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            ChatUser user = (ChatUser) session.getAttribute(SESSION_USER_KEY);
            if (user != null && user.getUserId() != null) {
                return user.getUserId();
            }
        }

        // 2. 从 Spring Security 上下文获取（排除匿名访问）
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            Authentication auth = securityContext.getAuthentication();
            if (auth.getPrincipal() instanceof String userId
                    && !userId.isEmpty()
                    && !"anonymousUser".equals(userId)) {
                return userId;
            }
        }

        // 3. 未登录：基于客户端 IP 生成唯一访客指纹
        return generateGuestId(httpRequest);
    }

    /**
     * 获取当前登录用户的 ChatUser 对象
     *
     * @return 当前用户，未登录返回 null
     */
    public ChatUser getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return getCurrentUser(request);
    }

    /**
     * 获取当前登录用户的 ChatUser 对象（指定 request）
     *
     * @param httpRequest HTTP 请求
     * @return 当前用户，未登录返回 null
     */
    public ChatUser getCurrentUser(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            ChatUser user = (ChatUser) session.getAttribute(SESSION_USER_KEY);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return true 表示已登录
     */
    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    /**
     * 判断给定 userId 是否为访客用户
     *
     * @param userId 用户ID
     * @return true 表示是访客用户（未登录）
     */
    public boolean isGuestUser(String userId) {
        return userId != null && userId.startsWith(GUEST_PREFIX);
    }

    /**
     * 从 RequestContextHolder 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    // ========== 登录成功处理 ==========

    /**
     * 登录成功后写入 Session + SecurityContext
     * <p>
     * 所有登录方式（本地账号、OAuth 三方登录、SSO）统一调用此方法，
     * 避免各 Controller 重复实现。
     *
     * @param user    登录成功的用户
     * @param request 当前 HTTP 请求
     */
    public void loginSuccess(ChatUser user, HttpServletRequest request) {
        // 防止 Session 固定攻击：使旧 Session 失效，创建新 Session
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);

        session.setAttribute(SESSION_USER_KEY, user);

        // 加载用户角色，转换为 Spring Security 权限
        List<String> roleKeys = roleService.getUserRoleKeys(user.getUserId());
        List<SimpleGrantedAuthority> authorities = roleKeys.stream()
                .map(key -> new SimpleGrantedAuthority("ROLE_" + key))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getUserId(), null, authorities);
        Map<String, String> details = new HashMap<>(4);
        details.put("nickname", user.getNickname());
        details.put("avatar", user.getAvatar());
        authToken.setDetails(details);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        log.info("[登录] 用户登录成功: userId={}, nickname={}, roles={}", user.getUserId(), user.getNickname(), roleKeys);
    }

    /**
     * 校验 OAuth 回调的 state 参数，防止 CSRF 攻击
     * <p>
     * 从 Session 中取出发起授权时存储的 state，与回调传入的 state 比对，
     * 校验完成后立即从 Session 中移除（一次性使用）。
     *
     * @param session       当前 HTTP Session
     * @param sessionKey    Session 中存储 state 的 Key（如 "wechat_state"）
     * @param callbackState 回调参数中携带的 state
     * @return true 表示校验通过
     */
    public boolean validateOAuthState(HttpSession session, String sessionKey, String callbackState) {
        String expectedState = (String) session.getAttribute(sessionKey);
        // 立即移除，防止重放
        session.removeAttribute(sessionKey);
        if (expectedState == null || callbackState == null) {
            log.warn("[OAuth] state 校验失败: expectedState={}, callbackState={}", expectedState, callbackState);
            return false;
        }
        return expectedState.equals(callbackState);
    }

    // ========== 访客指纹生成 ==========

    /**
     * 基于客户端 IP + 浏览器类型 + 设备类型 生成唯一访客ID
     */
    private String generateGuestId(HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }

        String browserType = extractBrowserType(userAgent);
        String deviceType = extractDeviceType(userAgent);
        String hash = sha256Hex(ip + browserType + deviceType);
        String fingerprint = hash.substring(0, 16);

        String guestId = GUEST_PREFIX + fingerprint;
        log.debug("生成访客ID: ip={}, browser={}, device={}, guestId={}", ip, browserType, deviceType, guestId);
        return guestId;
    }

    /**
     * 从 User-Agent 中提取浏览器类型
     */
    private String extractBrowserType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "edge";
        if (ua.contains("chrome")) return "chrome";
        if (ua.contains("firefox")) return "firefox";
        if (ua.contains("safari")) return "safari";
        if (ua.contains("opera") || ua.contains("opr/")) return "opera";
        return "other";
    }

    /**
     * 从 User-Agent 中提取设备类型（pc / mobile / tablet）
     */
    private String extractDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("tablet") || ua.contains("ipad")) return "tablet";
        if (ua.contains("mobile") || (ua.contains("android") && !ua.contains("tablet"))
                || ua.contains("iphone") || ua.contains("ipod")) return "mobile";
        return "pc";
    }

    /**
     * 无 request 时的回退方案
     */
    private String generateFallbackGuestId() {
        return GUEST_PREFIX + "fallback-" + Thread.currentThread().getId();
    }

    /**
     * 获取客户端真实 IP（支持反向代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * SHA-256 哈希，返回十六进制字符串
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new SystemSettingsException("SHA-256 算法不可用");
        }
    }
}
