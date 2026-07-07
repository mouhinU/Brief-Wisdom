package com.mouhin.brief.wisdom.system.service.impl;

import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import com.mouhin.brief.wisdom.exception.AuthException;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import com.mouhin.brief.wisdom.system.service.AuthService;
import com.mouhin.brief.wisdom.system.service.RoleService;
import com.mouhin.brief.wisdom.system.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 用户名/密码 注册与登录服务实现
 * <p>
 * 支持传统用户名密码登录和手机号验证码登录。
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ChatUserRepository chatUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final SmsService smsService;

    /**
     * 用户注册
     */
    @Override
    @Transactional
    public UserDTO register(String username, String password, String nickname) {
        ChatUser existing = chatUserRepository.findByUsername(username);
        if (existing != null) {
            throw new AuthException(BizExceptionEnums.PARAM_ERROR, "用户名已存在");
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
     * 用户注册（带手机号）
     * <p>
     * 绑定手机号的用户默认级别为 vip。
     */
    @Override
    @Transactional
    public UserDTO registerWithPhone(String username, String password, String nickname, String phone) {
        ChatUser existing = chatUserRepository.findByUsername(username);
        if (existing != null) {
            throw new AuthException(BizExceptionEnums.PARAM_ERROR, "用户名已存在");
        }

        // 检查手机号是否已被绑定
        if (phone != null && !phone.isBlank()) {
            ChatUser phoneUser = chatUserRepository.findByPhone(phone);
            if (phoneUser != null) {
                throw new AuthException(BizExceptionEnums.PARAM_ERROR, "该手机号已被其他账号绑定");
            }
        }

        String userId = UUID.randomUUID().toString();

        ChatUser user = new ChatUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setUserLevel("normal");

        // 绑定手机号，默认升级为 vip
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
            user.setUserLevel("vip");
        }

        chatUserRepository.save(user);

        // 分配默认角色（normal 普通用户）
        roleService.assignDefaultRole(userId);

        log.info("[注册] 新用户注册成功: userId={}, username={}, phone={}, level={}",
                userId, username, phone != null ? maskPhone(phone) : "null", user.getUserLevel());
        return toDTO(user);
    }

    /**
     * 用户登录（验证用户名和密码）
     */
    @Override
    public UserDTO login(String username, String password) {
        ChatUser user = chatUserRepository.findByUsername(username);
        if (user == null) {
            throw new AuthException(BizExceptionEnums.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new AuthException(BizExceptionEnums.UNAUTHORIZED, "该账号未设置密码，请使用其他方式登录");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException(BizExceptionEnums.UNAUTHORIZED, "用户名或密码错误");
        }

        log.info("[登录] 用户登录成功: userId={}, username={}", user.getUserId(), user.getUsername());
        return toDTO(user);
    }

    /**
     * 手机号验证码登录
     * <p>
     * 若手机号已绑定用户则直接登录，否则自动创建用户（级别为 vip）。
     */
    @Override
    @Transactional
    public UserDTO loginByPhone(String phone, String code) {
        // 校验验证码
        if (!smsService.verifyCode(phone, code)) {
            throw new AuthException(BizExceptionEnums.PARAM_ERROR, "验证码错误或已过期");
        }

        // 查找手机号对应的用户
        ChatUser user = chatUserRepository.findByPhone(phone);

        if (user != null) {
            // 已有用户，直接登录
            log.info("[手机号登录] 用户登录成功: userId={}, phone={}", user.getUserId(), maskPhone(phone));
            return toDTO(user);
        }

        // 新用户，自动创建（级别为 vip）
        String userId = UUID.randomUUID().toString();
        user = new ChatUser();
        user.setUserId(userId);
        user.setUsername("user_" + phone + "_" + UUID.randomUUID().toString().substring(0, 6));
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setUserLevel("vip");
        chatUserRepository.save(user);

        // 分配默认角色
        roleService.assignDefaultRole(userId);

        log.info("[手机号登录] 自动创建新用户: userId={}, phone={}, level=vip", userId, maskPhone(phone));
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

    /**
     * 手机号脱敏（中间4位用*替代）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
