package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.req.LoginRequest;
import com.mouhin.brief.wisdom.web.req.RegisterRequest;
import com.mouhin.brief.wisdom.web.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 用户名/密码 注册与登录 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String SPRING_SECURITY_CONTEXT_KEY =
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@RequestBody RegisterRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ApiResponse.fail("用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ApiResponse.fail("密码不能为空");
            }
            if (request.getPassword().length() < 6) {
                return ApiResponse.fail("密码长度不能少于6位");
            }
            UserDTO user = authService.register(request.getUsername(), request.getPassword(), request.getNickname());
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("[注册] 注册失败: ", e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 用户登录（用户名 + 密码）
     */
    @PostMapping("/login")
    public ApiResponse<UserDTO> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ApiResponse.fail("用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ApiResponse.fail("密码不能为空");
            }

            UserDTO user = authService.login(request.getUsername(), request.getPassword());

            // 写入 Session（与微信登录保持一致的认证方式）
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(WechatAuthController.SESSION_USER_KEY, buildChatUserStub(user));

            // 设置 Spring Security 认证上下文
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user.getUserId(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

            log.info("[登录] 用户登录成功, 写入 Session: userId={}, username={}", user.getUserId(), user.getUsername());
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("[登录] 登录失败: ", e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 根据 UserDTO 构建一个轻量 ChatUser 用于存入 Session
     */
    private ChatUser buildChatUserStub(UserDTO dto) {
        ChatUser user = new ChatUser();
        user.setUserId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        return user;
    }

}
