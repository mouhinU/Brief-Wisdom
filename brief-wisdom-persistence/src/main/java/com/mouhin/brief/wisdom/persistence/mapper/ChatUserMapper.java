package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatUserMapper extends BaseMapper<ChatUser> {
    
    // 根据用户名查找用户
    @Select("SELECT * FROM chat_user WHERE username = #{username} AND is_deleted = 0")
    ChatUser selectByUsername(String username);
}
