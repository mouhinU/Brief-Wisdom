package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {

    /**
     * 根据平台和 openid 查找绑定记录
     *
     * @param provider 平台标识（wechat/dingtalk/qq/alipay）
     * @param openid   平台 OpenID
     * @return 绑定记录，不存在返回 null
     */
    @Select("SELECT * FROM user_oauth WHERE provider = #{provider} AND openid = #{openid} AND is_deleted = 0")
    UserOauth selectByProviderAndOpenid(@Param("provider") String provider, @Param("openid") String openid);
}
