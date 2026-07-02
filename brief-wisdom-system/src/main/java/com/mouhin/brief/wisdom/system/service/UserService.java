package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.common.PageResult;
import com.mouhin.brief.wisdom.common.manage.UserDTO;

/**
 * 用户管理服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface UserService {

    /**
     * 分页获取用户列表（支持按级别、关键词筛选）
     */
    PageResult<UserDTO> listUsersPaged(int page, int size, String level, String keyword);

    /**
     * 获取所有用户级别选项
     */
    java.util.List<String> listUserLevels();

    /**
     * 修改用户级别
     */
    void updateLevel(Long id, String level);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 重置用户密码为 123456
     */
    void resetPassword(Long id);
}
