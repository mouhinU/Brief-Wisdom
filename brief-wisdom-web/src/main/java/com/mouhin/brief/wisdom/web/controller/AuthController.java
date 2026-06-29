package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.req.LoginRequest;
import com.mouhin.brief.wisdom.web.req.RegisterRequest;
import com.mouhin.brief.wisdom.web.service.AuthService;
import com.mouhin.brief.wisdom.web.service.RoleService;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用户名/密码 注册与登录 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SPRING_SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
    private final AuthService authService;
    private final RoleService roleService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        return authService.register(request.getUsername(), request.getPassword(), request.getNickname());
    }

    /**
     * 用户登录（用户名 + 密码）
     */
    @PostMapping("/login")
    public UserDTO login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        UserDTO user = authService.login(request.getUsername(), request.getPassword());
        // 写入 Session（与微信登录保持一致的认证方式）
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(WechatAuthController.SESSION_USER_KEY, buildChatUserStub(user));

        // 加载用户角色，转换为 Spring Security 权限
        List<String> roleKeys = roleService.getUserRoleKeys(user.getUserId());
        List<SimpleGrantedAuthority> authorities = buildAuthorities(roleKeys);

        // 设置 Spring Security 认证上下文
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        authorities
                );
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        log.info("[登录] 用户登录成功, 写入 Session: userId={}, username={}, roles={}", user.getUserId(), user.getUsername(), roleKeys);
        return user;
    }

    /**
     * 根据角色 Key 列表构建权限列表
     * 每个角色转换为 ROLE_xxx 格式的权限
     */
    private List<SimpleGrantedAuthority> buildAuthorities(List<String> roleKeys) {
        return roleKeys.stream()
                .map(key -> new SimpleGrantedAuthority("ROLE_" + key))
                .collect(Collectors.toList());
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
