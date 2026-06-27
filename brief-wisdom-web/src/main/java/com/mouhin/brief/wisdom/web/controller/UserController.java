package com.mouhin.brief.wisdom.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mouhin.brief.wisdom.common.ApiResponse;
import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.persistence.mapper.ChatUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller - 系统设置页面使用
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final ChatUserMapper chatUserMapper;

    /**
     * 分页获取用户列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResult<ChatUser>> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            Page<ChatUser> pageParam = new Page<>(page, size);
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

            Page<ChatUser> result = chatUserMapper.selectPage(pageParam, query);

            // 清除密码字段
            result.getRecords().forEach(u -> u.setPassword(null));

            PageResult<ChatUser> pageResult = new PageResult<>();
            pageResult.setRecords(result.getRecords());
            pageResult.setTotal(result.getTotal());
            pageResult.setPage(result.getCurrent());
            pageResult.setSize(result.getSize());
            pageResult.setPages(result.getPages());
            pageResult.setHasMore(result.getCurrent() < result.getPages());

            return ApiResponse.success(pageResult);
        } catch (Exception e) {
            log.error("获取用户列表失败: ", e);
            return ApiResponse.fail("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户级别选项
     */
    @GetMapping("/levels")
    public ApiResponse<List<String>> listLevels() {
        return ApiResponse.success(List.of("admin", "vip", "normal"));
    }

    /**
     * 修改用户级别
     */
    @PutMapping("/{id}/level")
    public ApiResponse<Void> updateLevel(@PathVariable Long id, @RequestBody UpdateLevelRequest request) {
        try {
            ChatUser user = chatUserMapper.selectById(id);
            if (user == null) {
                return ApiResponse.fail("用户不存在");
            }
            user.setUserLevel(request.getLevel());
            chatUserMapper.updateById(user);
            log.info("修改用户级别: userId={}, level={}", user.getUserId(), request.getLevel());
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("修改用户级别失败: ", e);
            return ApiResponse.fail("修改用户级别失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        try {
            ChatUser user = chatUserMapper.selectById(id);
            if (user == null) {
                return ApiResponse.fail("用户不存在");
            }
            chatUserMapper.deleteById(id);
            log.info("删除用户: userId={}, username={}", user.getUserId(), user.getUsername());
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除用户失败: ", e);
            return ApiResponse.fail("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码（清空密码，用户需重新设置）
     */
    @PutMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        try {
            ChatUser user = chatUserMapper.selectById(id);
            if (user == null) {
                return ApiResponse.fail("用户不存在");
            }
            user.setPassword(null);
            chatUserMapper.updateById(user);
            log.info("重置用户密码: userId={}, username={}", user.getUserId(), user.getUsername());
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("重置用户密码失败: ", e);
            return ApiResponse.fail("重置用户密码失败: " + e.getMessage());
        }
    }

    @Data
    public static class UpdateLevelRequest {
        private String level;
    }
}
