package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.UserOauthMapper;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 用户 OAuth 绑定数据访问层
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Repository
@RequiredArgsConstructor
public class UserOauthRepository {

    private final UserOauthMapper userOauthMapper;

    /**
     * 根据 OAuth 提供商和 OpenID 查询绑定记录
     *
     * @param provider 提供商（如 alipay、wechat、dingtalk）
     * @param openid   第三方平台用户唯一标识
     * @return 匹配的 OAuth 绑定记录，不存在返回 null
     */
    public UserOauth findByProviderAndOpenid(String provider, String openid) {
        return userOauthMapper.selectOne(
                new LambdaQueryWrapper<UserOauth>()
                        .eq(UserOauth::getProvider, provider)
                        .eq(UserOauth::getOpenid, openid)
        );
    }

    /**
     * 保存 OAuth 绑定记录
     *
     * @param oauth OAuth 绑定实体
     */
    public void save(UserOauth oauth) {
        userOauthMapper.insert(oauth);
    }

    /**
     * 更新 OAuth 绑定记录
     *
     * @param oauth OAuth 绑定实体
     */
    public void update(UserOauth oauth) {
        userOauthMapper.updateById(oauth);
    }
}
