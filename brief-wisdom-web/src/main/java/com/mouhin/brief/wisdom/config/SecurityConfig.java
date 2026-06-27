package com.mouhin.brief.wisdom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Security 配置
 * <p>
 * 策略：前端 JS 守卫 + 后端 Session 认证
 * <ul>
 *   <li>静态资源、AI API、认证 API 全部公开访问</li>
 *   <li>/api/auth/user 需要登录才能获取用户信息</li>
 *   <li>前端通过 /api/auth/status 判断登录状态，未登录则弹出微信扫码弹窗</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离，使用 Session 认证）
            .csrf(AbstractHttpConfigurer::disable)
            // 允许 H2 Console 使用 frame
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // 路由权限配置
            .authorizeHttpRequests(auth -> auth
                // 静态资源全部公开
                .requestMatchers("/", "/index.html", "/about.html", "/login.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/data/**", "/images/**").permitAll()
                // AI 聊天接口公开（现有功能不需要登录）
                .requestMatchers("/api/ai/**").permitAll()
                // 简历数据接口公开
                .requestMatchers("/api/resume/**").permitAll()
                // 菜单接口公开
                .requestMatchers("/api/menu/**").permitAll()
                // 简历管理页面公开
                .requestMatchers("/resume-manage.html").permitAll()
                // 系统设置页面公开
                .requestMatchers("/system-settings.html").permitAll()
                // AI助手管理页面公开
                .requestMatchers("/ai-manage.html").permitAll()
                // 认证相关接口公开（登录/回调/状态检查/注册）
                .requestMatchers("/auth/**", "/api/auth/status", "/api/auth/login/wechat", "/api/auth/register", "/api/auth/login").permitAll()
                // 获取当前用户信息需要登录
                .requestMatchers("/api/auth/user").authenticated()
                // 其余全部公开
                .anyRequest().permitAll()
            )
            // Session 管理
            .sessionManagement(session -> session
                .maximumSessions(1)  // 同一用户最多一个会话
                .maxSessionsPreventsLogin(false)  // 新登录踢掉旧会话
            )
            // 禁用默认登录页（使用自定义微信扫码登录）
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 未认证时的处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"error\":\"未登录\"}");
                })
            );

        return http.build();
    }

    /**
     * RestTemplate 用于调用微信 API
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 密码加密器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
