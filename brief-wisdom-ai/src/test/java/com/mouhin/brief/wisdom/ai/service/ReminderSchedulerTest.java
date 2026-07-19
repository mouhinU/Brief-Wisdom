package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.ChatReminder;
import com.mouhin.brief.wisdom.persistence.repository.ChatReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * ReminderScheduler 定时提醒调度器测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReminderScheduler 定时提醒调度器测试")
class ReminderSchedulerTest {

    @Mock
    private ChatReminderRepository chatReminderRepository;

    @Mock
    private ChatSyncService chatSyncService;

    private ReminderScheduler reminderScheduler;

    @BeforeEach
    void setUp() {
        reminderScheduler = new ReminderScheduler(chatReminderRepository, chatSyncService);
    }

    @Test
    @DisplayName("无到期提醒时不推送通知")
    void testNoDueReminders() {
        when(chatReminderRepository.findDueReminders()).thenReturn(Collections.emptyList());

        reminderScheduler.checkAndNotifyDueReminders();

        verify(chatSyncService, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("有到期提醒时推送通知并标记完成")
    void testDueRemindersNotified() {
        ChatReminder reminder = new ChatReminder();
        reminder.setId(1L);
        reminder.setUserId("user-1");
        reminder.setReminderText("测试提醒");
        reminder.setRemindTime(LocalDateTime.now().minusMinutes(1));
        reminder.setStatus(0);

        when(chatReminderRepository.findDueReminders()).thenReturn(List.of(reminder));

        reminderScheduler.checkAndNotifyDueReminders();

        verify(chatSyncService).notifyUser("user-1", "reminder_due", null);
        verify(chatReminderRepository).update(argThat(r -> r.getStatus() == 1));
    }

    @Test
    @DisplayName("多条到期提醒逐条处理")
    void testMultipleReminders() {
        ChatReminder r1 = new ChatReminder();
        r1.setId(1L);
        r1.setUserId("user-1");
        r1.setReminderText("提醒1");
        r1.setRemindTime(LocalDateTime.now().minusMinutes(5));
        r1.setStatus(0);

        ChatReminder r2 = new ChatReminder();
        r2.setId(2L);
        r2.setUserId("user-2");
        r2.setReminderText("提醒2");
        r2.setRemindTime(LocalDateTime.now().minusMinutes(1));
        r2.setStatus(0);

        when(chatReminderRepository.findDueReminders()).thenReturn(List.of(r1, r2));

        reminderScheduler.checkAndNotifyDueReminders();

        verify(chatSyncService).notifyUser("user-1", "reminder_due", null);
        verify(chatSyncService).notifyUser("user-2", "reminder_due", null);
        verify(chatReminderRepository, times(2)).update(argThat(r -> r.getStatus() == 1));
    }

    @Test
    @DisplayName("查询异常时不中断调度")
    void testQueryExceptionHandled() {
        when(chatReminderRepository.findDueReminders()).thenThrow(new RuntimeException("DB error"));

        // 不应抛出异常
        reminderScheduler.checkAndNotifyDueReminders();

        verify(chatSyncService, never()).notifyUser(anyString(), anyString(), anyString());
    }
}
