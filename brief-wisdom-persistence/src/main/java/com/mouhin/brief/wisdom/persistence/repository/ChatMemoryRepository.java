package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatMemoryMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 对话记忆数据访问层
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Repository("userChatMemoryRepository")
@RequiredArgsConstructor
public class ChatMemoryRepository {

    private final ChatMemoryMapper chatMemoryMapper;

    /**
     * 查询用户的所有记忆（按访问次数降序）
     *
     * @param userId 用户 ID
     * @return 记忆列表
     */
    public List<ChatMemory> findByUserId(String userId) {
        return chatMemoryMapper.selectList(
                new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getUserId, userId)
                        .orderByDesc(ChatMemory::getAccessCount)
        );
    }

    /**
     * 按分类查询用户记忆
     *
     * @param userId   用户 ID
     * @param category 分类名称
     * @return 记忆列表
     */
    public List<ChatMemory> findByUserIdAndCategory(String userId, String category) {
        return chatMemoryMapper.selectList(
                new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getUserId, userId)
                        .eq(ChatMemory::getCategory, category)
                        .orderByDesc(ChatMemory::getAccessCount)
        );
    }

    /**
     * 按 key 查询单条记忆
     *
     * @param userId    用户 ID
     * @param memoryKey 记忆键
     * @return 匹配的记忆，不存在返回 null
     */
    public ChatMemory findByUserIdAndKey(String userId, String memoryKey) {
        return chatMemoryMapper.selectOne(
                new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getUserId, userId)
                        .eq(ChatMemory::getMemoryKey, memoryKey)
        );
    }

    /**
     * 保存记忆
     *
     * @param memory 记忆实体
     */
    public void save(ChatMemory memory) {
        chatMemoryMapper.insert(memory);
    }

    /**
     * 更新记忆
     *
     * @param memory 记忆实体
     */
    public void update(ChatMemory memory) {
        chatMemoryMapper.updateById(memory);
    }

    /**
     * 删除记忆
     *
     * @param id 记忆 ID
     */
    public void deleteById(Long id) {
        chatMemoryMapper.deleteById(id);
    }

    /**
     * 增加访问次数
     *
     * @param id 记忆 ID
     */
    public void incrementAccessCount(Long id) {
        ChatMemory memory = chatMemoryMapper.selectById(id);
        if (memory != null) {
            memory.setAccessCount(memory.getAccessCount() + 1);
            chatMemoryMapper.updateById(memory);
        }
    }

    /**
     * 删除用户的所有记忆
     *
     * @param userId 用户 ID
     */
    public void deleteByUserId(String userId) {
        chatMemoryMapper.delete(
                new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getUserId, userId)
        );
    }
}
