package com.mouhin.brief.wisdom.persistence.repository;

import com.mouhin.brief.wisdom.persistence.mapper.UserOauthMapper;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 用户 OAuth 绑定数据访问层
 */
@Repository
@RequiredArgsConstructor
public class UserOauthRepository {

    private final UserOauthMapper userOauthMapper;

    public UserOauth findByProviderAndOpenid(String provider, String openid) {
        return userOauthMapper.selectByProviderAndOpenid(provider, openid);
    }

    public void save(UserOauth oauth) {
        userOauthMapper.insert(oauth);
    }

    public void update(UserOauth oauth) {
        userOauthMapper.updateById(oauth);
    }
}
