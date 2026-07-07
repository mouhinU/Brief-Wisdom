package com.mouhin.brief.wisdom.system.service;

/**
 * 短信验证码服务接口
 * <p>
 * 提供短信验证码的发送和校验能力，用于手机号登录和注册。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
public interface SmsService {

    /**
     * 发送验证码到指定手机号
     *
     * @param phone 手机号
     * @return 验证码（仅开发环境返回明文，生产环境由短信通道发送）
     */
    String sendVerificationCode(String phone);

    /**
     * 校验验证码是否正确
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    boolean verifyCode(String phone, String code);

    /**
     * 检查是否可以向该手机号发送验证码（防刷限制）
     *
     * @param phone 手机号
     * @return true-可以发送，false-频率超限
     */
    boolean canSend(String phone);
}
