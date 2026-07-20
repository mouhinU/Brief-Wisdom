package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.exception.AuthException;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import com.mouhin.brief.wisdom.system.service.RoleService;
import com.mouhin.brief.wisdom.system.service.SmsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 认证服务测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 认证服务测试")
class AuthServiceImplTest {

    @Mock
    private ChatUserRepository chatUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("register 用户名已存在时抛出 AuthException")
    void register_duplicateUsername() {
        when(chatUserRepository.findByUsername("alice")).thenReturn(new ChatUser());

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.register("alice", "pass123", "Alice"));
        assertTrue(ex.getMessage().contains("用户名已存在"));
        verify(chatUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("register 成功创建用户并分配默认角色")
    void register_success() {
        when(chatUserRepository.findByUsername("alice")).thenReturn(null);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");

        UserDTO dto = authService.register("alice", "pass123", "Alice");

        assertNotNull(dto.getUserId());
        assertEquals("alice", dto.getUsername());
        assertEquals("Alice", dto.getNickname());
        assertEquals("normal", dto.getUserLevel());

        ArgumentCaptor<ChatUser> captor = ArgumentCaptor.forClass(ChatUser.class);
        verify(chatUserRepository).save(captor.capture());
        assertEquals("encoded-pass", captor.getValue().getPassword());
        verify(roleService).assignDefaultRole(dto.getUserId());
    }

    @Test
    @DisplayName("login 用户不存在时抛出 AuthException")
    void login_userNotFound() {
        when(chatUserRepository.findByUsername("bob")).thenReturn(null);

        assertThrows(AuthException.class, () -> authService.login("bob", "pass123"));
    }

    @Test
    @DisplayName("login 密码错误时抛出 AuthException")
    void login_wrongPassword() {
        ChatUser user = buildUser("u1", "bob", "encoded");
        when(chatUserRepository.findByUsername("bob")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login("bob", "wrong"));
    }

    @Test
    @DisplayName("login 成功返回 UserDTO")
    void login_success() {
        ChatUser user = buildUser("u1", "bob", "encoded");
        when(chatUserRepository.findByUsername("bob")).thenReturn(user);
        when(passwordEncoder.matches("pass123", "encoded")).thenReturn(true);

        UserDTO dto = authService.login("bob", "pass123");

        assertEquals("u1", dto.getUserId());
        assertEquals("bob", dto.getUsername());
    }

    @Test
    @DisplayName("registerWithPhone 绑定手机号时用户级别为 vip")
    void registerWithPhone_vipLevel() {
        when(chatUserRepository.findByUsername("carol")).thenReturn(null);
        when(chatUserRepository.findByPhone("13800138000")).thenReturn(null);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");

        UserDTO dto = authService.registerWithPhone("carol", "pass123", "Carol", "13800138000");

        assertEquals("vip", dto.getUserLevel());
        ArgumentCaptor<ChatUser> captor = ArgumentCaptor.forClass(ChatUser.class);
        verify(chatUserRepository).save(captor.capture());
        assertEquals("13800138000", captor.getValue().getPhone());
    }

    @Test
    @DisplayName("loginByPhone 验证码错误时抛出 AuthException")
    void loginByPhone_invalidCode() {
        when(smsService.verifyCode("13800138000", "000000")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.loginByPhone("13800138000", "000000"));
    }

    @Test
    @DisplayName("loginByPhone 已有用户直接登录")
    void loginByPhone_existingUser() {
        ChatUser user = buildUser("u2", "phone_user", null);
        user.setPhone("13800138000");
        user.setUserLevel("vip");
        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(chatUserRepository.findByPhone("13800138000")).thenReturn(user);

        UserDTO dto = authService.loginByPhone("13800138000", "123456");

        assertEquals("u2", dto.getUserId());
        verify(chatUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("loginByPhone 新用户自动创建并分配默认角色")
    void loginByPhone_createNewUser() {
        when(smsService.verifyCode("13900139000", "654321")).thenReturn(true);
        when(chatUserRepository.findByPhone("13900139000")).thenReturn(null);

        UserDTO dto = authService.loginByPhone("13900139000", "654321");

        assertEquals("vip", dto.getUserLevel());
        verify(chatUserRepository).save(any(ChatUser.class));
        verify(roleService).assignDefaultRole(dto.getUserId());
    }

    private ChatUser buildUser(String userId, String username, String password) {
        ChatUser user = new ChatUser();
        user.setId(1L);
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(username);
        user.setUserLevel("normal");
        return user;
    }
}
