package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatReminderMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatReminder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户提醒事项数据访问层
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Repository
@RequiredArgsConstructor
public class ChatReminderRepository {

    private final ChatReminderMapper chatReminderMapper;

    /**
     * 查询用户的待处理提醒（按提醒时间升序）
     *
     * @param userId 用户 ID
     * @return 待处理提醒列表
     */
    public List<ChatReminder> findPendingByUserId(String userId) {
        return chatReminderMapper.selectList(
                new LambdaQueryWrapper<ChatReminder>()
                        .eq(ChatReminder::getUserId, userId)
                        .eq(ChatReminder::getStatus, 0)
                        .orderByAsc(ChatReminder::getRemindTime)
        );
    }

    /**
     * 查询所有到期的待处理提醒
     *
     * @return 到期的提醒列表
     */
    public List<ChatReminder> findDueReminders() {
        return chatReminderMapper.selectList(
                new LambdaQueryWrapper<ChatReminder>()
                        .eq(ChatReminder::getStatus, 0)
                        .le(ChatReminder::getRemindTime, LocalDateTime.now())
                        .orderByAsc(ChatReminder::getRemindTime)
        );
    }

    /**
     * 保存提醒
     *
     * @param reminder 提醒实体
     */
    public void save(ChatReminder reminder) {
        chatReminderMapper.insert(reminder);
    }

    /**
     * 更新提醒状态
     *
     * @param reminder 提醒实体
     */
    public void update(ChatReminder reminder) {
        chatReminderMapper.updateById(reminder);
    }

    /**
     * 删除提醒
     *
     * @param id 提醒 ID
     */
    public void deleteById(Long id) {
        chatReminderMapper.deleteById(id);
    }
}
