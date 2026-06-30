package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.UserOauth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 OAuth 绑定 Mapper
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {
}
