package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.controller.WechatAuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户上下文工具类
 * <p>
 * 所有 Controller 都可以注入此组件获取当前登录用户信息。
 * 未登录时返回默认用户信息。
 */
@Component
public class UserContextHelper {

    /** 默认用户ID（未登录时使用） */
    private static final String DEFAULT_USER_ID = "default-user";
    /** 默认用户名 */
    private static final String DEFAULT_USERNAME = "anonymous";
    /** 默认昵称 */
    private static final String DEFAULT_NICKNAME = "访客用户";

    private static final String SPRING_SECURITY_CONTEXT_KEY =
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    /**
     * 获取当前登录用户的 userId
     * <p>
     * 优先从 Session 中获取，其次从 Spring Security 上下文获取，
     * 都没有则返回默认用户ID。
     *
     * @return 当前用户ID，不为 null
     */
    public String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return DEFAULT_USER_ID;
        }
        return getCurrentUserId(request);
    }

    /**
     * 获取当前登录用户的 userId（指定 request）
     *
     * @param httpRequest HTTP 请求
     * @return 当前用户ID，不为 null
     */
    public String getCurrentUserId(HttpServletRequest httpRequest) {
        // 1. 优先从 Session 获取
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            ChatUser user = (ChatUser) session.getAttribute(WechatAuthController.SESSION_USER_KEY);
            if (user != null && user.getUserId() != null) {
                return user.getUserId();
            }
        }

        // 2. 从 Spring Security 上下文获取
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            Authentication auth = securityContext.getAuthentication();
            if (auth.getPrincipal() instanceof String userId && !userId.isEmpty()) {
                return userId;
            }
        }

        // 3. 返回默认用户
        return DEFAULT_USER_ID;
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
     * 判断给定 userId 是否为默认用户
     *
     * @param userId 用户ID
     * @return true 表示是默认用户（未登录）
     */
    public boolean isDefaultUser(String userId) {
        return DEFAULT_USER_ID.equals(userId);
    }

    /**
     * 获取默认用户ID
     */
    public String getDefaultUserId() {
        return DEFAULT_USER_ID;
    }

    /**
     * 获取默认用户名
     */
    public String getDefaultUsername() {
        return DEFAULT_USERNAME;
    }

    /**
     * 获取默认昵称
     */
    public String getDefaultNickname() {
        return DEFAULT_NICKNAME;
    }

    /**
     * 从 RequestContextHolder 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
