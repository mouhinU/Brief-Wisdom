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
     *
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @param level   用户级别（可选）
     * @param keyword 搜索关键词（可选）
     * @return 分页用户列表
     */
    PageResult<UserDTO> listUsersPaged(int page, int size, String level, String keyword);

    /**
     * 获取所有用户级别选项
     *
     * @return 用户级别列表
     */
    java.util.List<String> listUserLevels();

    /**
     * 修改用户级别
     *
     * @param id    用户 ID
     * @param level 新级别
     */
    void updateLevel(Long id, String level);

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户 ID
     */
    void deleteUser(Long id);

    /**
     * 重置用户密码为 123456
     *
     * @param id 用户 ID
     */
    void resetPassword(Long id);
}
