package com.mouhin.brief.wisdom.system.service;

import com.mouhin.brief.wisdom.common.manage.UserDTO;

/**
 * 用户名/密码 注册与登录服务接口
 * <p>
 * 支持传统用户名密码登录和手机号验证码登录。
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
     * 用户注册（带手机号）
     * <p>
     * 绑定手机号的用户默认级别为 vip，角色为 guest。
     *
     * @param username 用户名（唯一）
     * @param password 明文密码
     * @param nickname 昵称（可选）
     * @param phone    手机号（可选，绑定后用户级别为 vip）
     * @return 注册后的用户信息
     */
    UserDTO registerWithPhone(String username, String password, String nickname, String phone);

    /**
     * 用户登录（验证用户名和密码）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录成功的用户信息
     */
    UserDTO login(String username, String password);

    /**
     * 手机号验证码登录
     * <p>
     * 若手机号已绑定用户则直接登录，否则自动创建用户（级别为 vip，角色为 guest）。
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录成功的用户信息
     */
    UserDTO loginByPhone(String phone, String code);
}
