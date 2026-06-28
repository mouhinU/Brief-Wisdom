package com.mouhin.brief.wisdom.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatUserMapper extends BaseMapper<ChatUser> {
    
    // 根据用户名查找用户
    @Select("SELECT * FROM chat_user WHERE username = #{username} AND is_deleted = 0")
    ChatUser selectByUsername(String username);

    // 根据 userId 查找用户（包含已逻辑删除的记录，绕过 @TableLogic）
    @Select("SELECT * FROM chat_user WHERE user_id = #{userId}")
    ChatUser selectByUserIdIncludeDeleted(String userId);

    // 恢复已逻辑删除的用户
    @Update("UPDATE chat_user SET is_deleted = 0 WHERE user_id = #{userId} AND is_deleted = 1")
    int restoreByUserId(String userId);

    // 硬删除用户（绕过 @TableLogic，物理删除记录及其级联数据）
    @Delete("DELETE FROM chat_user WHERE user_id = #{userId}")
    int hardDeleteByUserId(String userId);
}
