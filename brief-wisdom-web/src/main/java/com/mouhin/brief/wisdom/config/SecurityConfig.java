package com.mouhin.brief.wisdom.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 * <p>
 * 策略：基于 RBAC 的权限控制
 * <ul>
 *   <li>静态资源、公开 API 全部开放</li>
 *   <li>管理接口需要对应角色权限</li>
 *   <li>超级管理员 (super_admin) 拥有所有权限</li>
 * </ul>
 * <p>
 * CORS 统一配置：通过 app.cors.allowed-origins 指定允许的域名，
 * 开发环境默认为 localhost:*，生产环境应配置为实际域名。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * CORS 允许的域名列表，逗号分隔
     */
    @Value("${app.cors.allowed-origins:http://localhost:8090,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离，使用 Session 认证）
                .csrf(AbstractHttpConfigurer::disable)
                // 启用统一 CORS 配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 允许 H2 Console 使用 frame
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // 路由权限配置
                .authorizeHttpRequests(auth -> auth
                        // 静态资源全部公开
                        .requestMatchers("/", "/index.html", "/about.html", "/login.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/data/**", "/images/**").permitAll()
                        // AI 助手管理需要 admin 或 super_admin 角色（必须在 /api/ai/** 之前）
                        .requestMatchers("/api/ai/manage/**").hasAnyRole("admin", "super_admin")
                        .requestMatchers("/ai-manage.html").hasAnyRole("admin", "super_admin")
                        // AI 聊天接口全部公开（含会话管理、同步、配置、模型列表）
                        .requestMatchers("/api/ai/models/enabled").permitAll()
                        .requestMatchers("/api/ai/**").permitAll()
                        // Actuator：health/info 公开（Docker 探活），其余需管理员
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasAnyRole("admin", "super_admin")
                        // WebSocket 同步端点公开
                        .requestMatchers("/ws/**").permitAll()
                        // 知识库管理需要 admin 或 super_admin 角色
                        .requestMatchers("/api/knowledge/**").hasAnyRole("admin", "super_admin")
                        // 简历数据接口公开
                        .requestMatchers("/api/resume/experiences/**").permitAll()
                        // 菜单接口公开
                        .requestMatchers("/api/menu/list", "/api/menu/tree").permitAll()
                        // 菜单管理需要 admin 或 super_admin 角色
                        .requestMatchers("/api/menu/**").hasAnyRole("admin", "super_admin")
                        // 简历管理页面公开（展示用）
                        .requestMatchers("/resume-manage.html").permitAll()
                        // 简历数据管理需要 admin 或 super_admin 角色
                        .requestMatchers("/api/resume/manage/**").hasAnyRole("admin", "super_admin")
                        // 系统设置页面需要 admin 或 super_admin 角色
                        .requestMatchers("/system-settings.html").hasAnyRole("admin", "super_admin")
                        // 用户管理需要 admin 或 super_admin 角色
                        .requestMatchers("/api/user/**").hasAnyRole("admin", "super_admin")
                        // 角色管理需要 super_admin 角色
                        .requestMatchers("/api/role/**").hasRole("super_admin")
                        // 认证相关接口公开（含手机号登录、短信验证码、SSO）
                        .requestMatchers("/auth/**", "/api/auth/status", "/api/auth/login/wechat",
                                "/api/auth/register", "/api/auth/login", "/api/auth/login/phone",
                                "/api/auth/sms/**", "/api/auth/sso/**").permitAll()
                        // 获取当前用户信息需要登录
                        .requestMatchers("/api/auth/user").authenticated()
                        // 当前用户菜单需要登录
                        .requestMatchers("/api/menu/my-menus").authenticated()
                        // 其余全部公开
                        .anyRequest().permitAll()
                )
                // Session 管理
                .sessionManagement(session -> session
                        .maximumSessions(1)  // 同一用户最多一个会话
                        .maxSessionsPreventsLogin(false)  // 新登录踢掉旧会话
                )
                // 禁用默认登录页
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 未认证时的处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isPageRequest(request)) {
                                response.sendRedirect("/");
                            } else {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"error\":\"未登录\"}");
                            }
                        })
                        // 无权限时的处理
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isPageRequest(request)) {
                                response.sendRedirect("/");
                            } else {
                                response.setStatus(403);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"error\":\"权限不足\"}");
                            }
                        })
                );

        return http.build();
    }

    /**
     * 统一 CORS 配置源
     * <p>
     * 通过 app.cors.allowed-origins 配置允许的域名（逗号分隔），
     * 替代各 Controller 上分散的 @CrossOrigin(origins = "*") 注解。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 判断是否为页面请求（非 AJAX/Fetch）
     * 页面请求返回 HTML，应重定向；API 请求返回 JSON
     */
    private boolean isPageRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xRequested = request.getHeader("X-Requested-With");
        // Accept 包含 text/html 且不是 AJAX 请求，视为页面请求
        return accept != null && accept.contains("text/html") && !"XMLHttpRequest".equals(xRequested);
    }

    /**
     * RestClient 用于调用第三方 OAuth API（微信、钉钉、支付宝）
     */
    @Bean
    public RestClient restClient() {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 密码加密器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
