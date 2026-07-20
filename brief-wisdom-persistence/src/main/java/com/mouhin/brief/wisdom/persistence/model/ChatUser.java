package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_user")
public class ChatUser extends BaseEntity {

    @TableField(value = "user_id")
    private String userId;  // 用户ID (UUID)

    @TableField(value = "username")
    private String username;

    @TableField(value = "nickname")
    private String nickname;

    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "password")
    private String password;  // BCrypt加密后的密码，null表示未设置密码（仅第三方登录）

    @TableField(value = "phone")
    private String phone;  // 手机号（脱敏存储，用于短信验证码登录）

    @TableField(value = "user_level")
    private String userLevel;  // 用户级别: admin/vip/normal
}
