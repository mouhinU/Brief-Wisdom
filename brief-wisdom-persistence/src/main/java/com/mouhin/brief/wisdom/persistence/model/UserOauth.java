package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 OAuth 绑定实体类
 * <p>
 * 存储用户与各平台（微信/钉钉/QQ/支付宝）的授权绑定关系。
 * 同一用户可绑定多个平台，每个平台只能绑定一次（uk_provider_openid 唯一约束）。
 */
@Data
@TableName("user_oauth")
public class UserOauth implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 chat_user.user_id
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 平台标识：wechat / dingtalk / qq / alipay
     */
    @TableField(value = "provider")
    private String provider;

    /**
     * 平台 OpenID（平台内唯一标识）
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 平台 UnionID（微信/钉钉跨应用唯一标识，部分平台无此字段）
     */
    @TableField(value = "unionid")
    private String unionid;

    /**
     * 该平台显示昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 该平台头像URL
     */
    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;  // 0-未删除, 1-已删除
}
