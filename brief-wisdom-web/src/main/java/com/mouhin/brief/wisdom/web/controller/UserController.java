package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.web.req.UpdateLevelRequest;
import com.mouhin.brief.wisdom.web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller - 系统设置页面使用
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@RequiresPermission("user:manage")
public class UserController {

    private final UserService userService;

    /**
     * 分页获取用户列表
     */
    @GetMapping("/list")
    public PageResult<UserDTO> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return userService.listUsersPaged(page, size, level, keyword);
    }

    /**
     * 获取所有用户级别选项
     */
    @GetMapping("/levels")
    public List<String> listLevels() {
        return userService.listUserLevels();
    }

    /**
     * 修改用户级别
     */
    @PutMapping("/{id}/level")
    public Boolean updateLevel(@PathVariable Long id, @RequestBody UpdateLevelRequest request) {
        userService.updateLevel(id, request.getLevel());
        return true;
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Boolean deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return true;
    }

    /**
     * 重置用户密码（清空密码，用户需重新设置）
     */
    @PutMapping("/{id}/reset-password")
    public Boolean resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return true;
    }
}
