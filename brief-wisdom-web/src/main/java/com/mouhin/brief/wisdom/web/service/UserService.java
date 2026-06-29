package com.mouhin.brief.wisdom.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.model.SysRole;
import com.mouhin.brief.wisdom.persistence.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ChatUserRepository chatUserRepository;
    private final RoleService roleService;

    /**
     * 分页获取用户列表（支持按级别、关键词筛选）
     */
    public PageResult<UserDTO> listUsersPaged(int page, int size, String level, String keyword) {
        LambdaQueryWrapper<ChatUser> query = new LambdaQueryWrapper<>();

        if (level != null && !level.isEmpty()) {
            query.eq(ChatUser::getUserLevel, level);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w
                    .like(ChatUser::getUsername, keyword)
                    .or().like(ChatUser::getNickname, keyword)
            );
        }
        query.orderByDesc(ChatUser::getCreateTime);

        Page<ChatUser> result = chatUserRepository.findPage(page, size, query);

        PageResult<UserDTO> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords().stream().map(this::toUserDTO).toList());
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setPages(result.getPages());
        pageResult.setHasMore(result.getCurrent() < result.getPages());
        return pageResult;
    }

    /**
     * 获取所有用户级别选项
     */
    public java.util.List<String> listUserLevels() {
        return java.util.List.of("admin", "vip", "normal");
    }

    /**
     * 修改用户级别
     */
    public void updateLevel(Long id, String level) {
        ChatUser user = chatUserRepository.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setUserLevel(level);
        chatUserRepository.update(user);
        log.info("修改用户级别: userId={}, level={}", user.getUserId(), level);
    }

    /**
     * 删除用户（逻辑删除）
     */
    public void deleteUser(Long id) {
        ChatUser user = chatUserRepository.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        chatUserRepository.deleteById(id);
        log.info("删除用户: userId={}, username={}", user.getUserId(), user.getUsername());
    }

    /**
     * 重置用户密码（清空密码，用户需重新设置）
     */
    public void resetPassword(Long id) {
        ChatUser user = chatUserRepository.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(null);
        chatUserRepository.update(user);
        log.info("重置用户密码: userId={}, username={}", user.getUserId(), user.getUsername());
    }

    /**
     * ChatUser 实体转 UserDTO（自动清除密码字段，包含角色信息）
     */
    private UserDTO toUserDTO(ChatUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setUserLevel(user.getUserLevel());
        dto.setCreateTime(user.getCreateTime());

        // 加载用户角色信息
        List<SysRole> roles = roleService.getUserRoles(user.getUserId());
        dto.setRoleKeys(roles.stream().map(SysRole::getRoleKey).toList());
        dto.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());

        return dto;
    }
}
