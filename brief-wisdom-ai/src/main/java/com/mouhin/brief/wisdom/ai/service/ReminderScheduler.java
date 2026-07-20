package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.ChatReminder;
import com.mouhin.brief.wisdom.persistence.repository.ChatReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时提醒调度器
 * <p>
 * 每分钟检查一次到期的提醒事项，通过 SSE/WebSocket 推送通知给用户，
 * 并将已通知的提醒状态更新为已完成。
 *
 * @author Brief-Wisdom
 * @date 2026-07-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    /**
     * 提醒到期事件类型
     */
    private static final String REMINDER_DUE_EVENT = "reminder_due";
    /**
     * 提醒完成状态
     */
    private static final int STATUS_COMPLETED = 1;
    private final ChatReminderRepository chatReminderRepository;
    private final ChatSyncService chatSyncService;

    /**
     * 每分钟检查并处理到期的提醒
     * <p>
     * 查询所有 status=0 且 remindTime <= now 的提醒，
     * 逐个推送通知并标记为已完成。
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndNotifyDueReminders() {
        List<ChatReminder> dueReminders;
        try {
            dueReminders = chatReminderRepository.findDueReminders();
        } catch (Exception e) {
            log.error("[ReminderScheduler] 查询到期提醒失败: {}", e.getMessage(), e);
            return;
        }

        if (dueReminders.isEmpty()) {
            return;
        }

        log.info("[ReminderScheduler] 发现 {} 条到期提醒", dueReminders.size());

        for (ChatReminder reminder : dueReminders) {
            try {
                // 推送通知给用户
                chatSyncService.notifyUser(reminder.getUserId(), REMINDER_DUE_EVENT, null);

                // 标记为已完成
                reminder.setStatus(STATUS_COMPLETED);
                chatReminderRepository.update(reminder);

                log.info("[ReminderScheduler] 提醒已通知 - userId: {}, reminderId: {}, text: {}",
                        reminder.getUserId(), reminder.getId(),
                        reminder.getReminderText().length() > 30
                                ? reminder.getReminderText().substring(0, 30) + "..."
                                : reminder.getReminderText());
            } catch (Exception e) {
                log.error("[ReminderScheduler] 处理提醒失败 - reminderId: {}, error: {}",
                        reminder.getId(), e.getMessage(), e);
            }
        }
    }
}
