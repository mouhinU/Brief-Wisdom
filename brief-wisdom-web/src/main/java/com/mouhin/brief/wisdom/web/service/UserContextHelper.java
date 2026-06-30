package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.controller.WechatAuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 用户上下文工具类
 * <p>
 * 所有 Controller 都可以注入此组件获取当前登录用户信息。
 * 未登录时基于客户端 IP 生成唯一访客指纹作为 userId。
 */
/**
 * UserContextHelper
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Component
public class UserContextHelper {

    /**
     * 访客用户ID前缀
     */
    private static final String GUEST_PREFIX = "guest-";

    private static final String SPRING_SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

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
            ChatUser user = (ChatUser) session.getAttribute(WechatAuthController.SESSION_USER_KEY);
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
            ChatUser user = (ChatUser) session.getAttribute(WechatAuthController.SESSION_USER_KEY);
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

    // ========== 访客指纹生成 ==========

    /**
     * 基于客户端 IP + 浏览器类型 + 设备类型 生成唯一访客ID
     * <p>
     * 同一设备（同一 IP）+ 同一浏览器 + 同一设备类型 → 相同的指纹 → 相同的 userId，
     * 保证未登录访客的会话数据隔离与连续性。
     *
     * @param request HTTP 请求
     * @return 格式为 "guest-{SHA256前16位}" 的访客ID
     */
    private String generateGuestId(HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }

        // IP + 浏览器类型 + 设备类型组合作为指纹因子
        String browserType = extractBrowserType(userAgent);
        String deviceType = extractDeviceType(userAgent);
        String hash = sha256Hex(ip + browserType + deviceType);
        // 取前 16 位作为指纹，足够唯一且不会过长
        String fingerprint = hash.substring(0, 16);

        String guestId = GUEST_PREFIX + fingerprint;
        log.debug("生成访客ID: ip={}, browser={}, device={}, guestId={}", ip, browserType, deviceType, guestId);
        return guestId;
    }

    /**
     * 从 User-Agent 中提取浏览器类型（如 Chrome、Firefox、Safari、Edge 等）
     * <p>
     * 只取浏览器内核标识，忽略版本号，避免浏览器升级后访客ID变化。
     */
    private String extractBrowserType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        // 按优先级匹配（注意：Edge 的 UA 也包含 Chrome，需先判断 Edge）
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
        // 平板优先判断（部分平板 UA 同时包含 mobile 和 tablet）
        if (ua.contains("tablet") || ua.contains("ipad")) return "tablet";
        // 移动端特征
        if (ua.contains("mobile") || ua.contains("android") && !ua.contains("tablet")
                || ua.contains("iphone") || ua.contains("ipod")) return "mobile";
        return "pc";
    }

    /**
     * 无 request 时的回退方案（基于当前时间戳生成随机访客ID）
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
            // X-Forwarded-For 可能包含多个 IP，取第一个（客户端真实 IP）
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
            // SHA-256 是 Java 标准库必支持的，不会走到这里
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
