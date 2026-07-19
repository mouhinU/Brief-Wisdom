package com.mouhin.brief.wisdom.interceptor;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MDC 日志上下文过滤器
 * <p>
 * 从请求头或 Session 中提取 userId、sessionId 放入 MDC，
 * 使日志输出自动携带用户和会话上下文信息，便于问题排查和审计追踪。
 *
 * @author Brief-Wisdom
 * @date 2026-07-19
 */
@Component
@Order(1)
@WebFilter("/*")
public class MdcLoggingFilter implements Filter {

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_SESSION_ID = "sessionId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // 从请求头获取 userId（由认证拦截器设置）
                String userId = httpRequest.getHeader("X-User-Id");
                if (userId == null || userId.isBlank()) {
                    // 尝试从 Session 属性获取
                    Object sessionUser = httpRequest.getSession(false) != null
                            ? httpRequest.getSession(false).getAttribute("userId")
                            : null;
                    if (sessionUser != null) {
                        userId = sessionUser.toString();
                    }
                }

                // 从请求参数或请求头获取 sessionId
                String sessionId = httpRequest.getHeader("X-Session-Id");
                if (sessionId == null || sessionId.isBlank()) {
                    sessionId = httpRequest.getParameter("sessionId");
                }

                // 设置 MDC 上下文
                if (userId != null && !userId.isBlank()) {
                    MDC.put(MDC_USER_ID, userId);
                }
                if (sessionId != null && !sessionId.isBlank()) {
                    MDC.put(MDC_SESSION_ID, sessionId);
                }
            }

            chain.doFilter(request, response);
        } finally {
            // 请求结束后清理 MDC，避免线程复用导致上下文污染
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_SESSION_ID);
        }
    }
}
