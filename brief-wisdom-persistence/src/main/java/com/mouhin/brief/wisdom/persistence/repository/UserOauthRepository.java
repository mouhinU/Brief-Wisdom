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

    public UserOauth findByProviderAndOpenid(String provider, String openid) {
        return userOauthMapper.selectOne(
                new LambdaQueryWrapper<UserOauth>()
                        .eq(UserOauth::getProvider, provider)
                        .eq(UserOauth::getOpenid, openid)
        );
    }

    public void save(UserOauth oauth) {
        userOauthMapper.insert(oauth);
    }

    public void update(UserOauth oauth) {
        userOauthMapper.updateById(oauth);
    }
}
