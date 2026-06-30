package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
/**
 * ChatUser
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@TableName("chat_user")
public class ChatUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;  // 自增主键

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

    @TableField(value = "user_level")
    private String userLevel;  // 用户级别: admin/vip/normal

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;  // 0-未删除, 1-已删除
}
