package com.mouhin.brief.wisdom.web.service;

import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 用户名/密码 注册与登录服务
 */
/**
 * AuthService
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ChatUserRepository chatUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    /**
     * 用户注册
     *
     * @param username 用户名（唯一）
     * @param password 明文密码
     * @param nickname 昵称（可选）
     * @return 注册后的用户信息
     */
    @Transactional
    public UserDTO register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        ChatUser existing = chatUserRepository.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        String userId = UUID.randomUUID().toString();

        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setUserLevel("normal");
        chatUserRepository.save(user);

        // 分配默认角色（normal 普通用户）
        roleService.assignDefaultRole(userId);

        log.info("[注册] 新用户注册成功: userId={}, username={}", userId, username);
        return toDTO(user);
    }

    /**
     * 用户登录（验证用户名和密码）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功的用户信息
     */
    public UserDTO login(String username, String password) {
        ChatUser user = chatUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("该账号未设置密码，请使用其他方式登录");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("[登录] 用户登录成功: userId={}, username={}", user.getUserId(), user.getUsername());
        return toDTO(user);
    }

    private UserDTO toDTO(ChatUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setUserLevel(user.getUserLevel());
        dto.setCreateTime(user.getCreateTime());
        return dto;
    }
}
