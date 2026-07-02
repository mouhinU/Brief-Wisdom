package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.common.manage.UserDTO;

/**
 * 用户名/密码 注册与登录服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param username 用户名（唯一）
     * @param password 明文密码
     * @param nickname 昵称（可选）
     * @return 注册后的用户信息
     */
    UserDTO register(String username, String password, String nickname);

    /**
     * 用户登录（验证用户名和密码）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功的用户信息
     */
    UserDTO login(String username, String password);
}
