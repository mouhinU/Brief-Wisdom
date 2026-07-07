package com.mouhin.brief.wisdom.web.controller;

import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.manage.UserDTO;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.web.req.UpdateLevelRequest;
import com.mouhin.brief.wisdom.web.req.UserListQueryRequest;
import com.mouhin.brief.wisdom.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller - 系统设置页面使用
 */
/**
 * UserController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@RequiresPermission("user:manage")
@Tag(name = "用户管理", description = "用户信息查询与管理相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 分页获取用户列表
     */
    @Operation(summary = "分页获取用户列表")
    @GetMapping("/list")
    public PageResult<UserDTO> listUsers(UserListQueryRequest request) {
        return userService.listUsersPaged(
                request.getPage(), request.getSize(), request.getLevel(), request.getKeyword());
    }

    /**
     * 获取所有用户级别选项
     */
    @Operation(summary = "获取用户级别选项")
    @GetMapping("/levels")
    public List<String> listLevels() {
        return userService.listUserLevels();
    }

    /**
     * 修改用户级别
     */
    @Operation(summary = "修改用户级别")
    @PutMapping("/{id}/level")
    public Boolean updateLevel(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @RequestBody UpdateLevelRequest request) {
        userService.updateLevel(id, request.getLevel());
        return true;
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Operation(summary = "删除用户", description = "逻辑删除用户")
    @DeleteMapping("/{id}")
    public Boolean deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        userService.deleteUser(id);
        return true;
    }

    /**
     * 重置用户密码（清空密码，用户需重新设置）
     */
    @Operation(summary = "重置用户密码", description = "清空密码，用户需重新设置")
    @PutMapping("/{id}/reset-password")
    public Boolean resetPassword(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        userService.resetPassword(id);
        return true;
    }
}
