package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.persistence.model.ChatReminder;
import com.mouhin.brief.wisdom.persistence.repository.ChatReminderRepository;
import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时提醒工具
 * <p>
 * 允许用户通过 AI 对话创建提醒事项。
 * 支持自然语言时间表达（如"30分钟后"、"明天下午3点"）和 ISO-8601 格式。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTool {

    private final ChatReminderRepository chatReminderRepository;
    private final ToolContextProvider toolContextProvider;

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Pattern RELATIVE_TIME_PATTERN =
            Pattern.compile("(\\d+)\\s*(分钟|小时|天|秒)后");

    /**
     * 创建提醒事项
     *
     * @param reminderText 提醒内容
     * @param remindAt     提醒时间
     * @return 操作结果
     */
    @Tool(description = "为用户创建提醒事项。当用户说'提醒我XX'、'X分钟后记得XX'、'明天几点提醒我XX'时调用。")
    public String createReminder(
            @ToolParam(description = "提醒内容，描述需要提醒的事情") String reminderText,
            @ToolParam(description = "提醒时间，支持 ISO-8601 格式(如 2026-07-15T15:00:00)或相对时间(如 30分钟后、2小时后、1天后)") String remindAt) {

        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] createReminder 被调用: userId={}, reminderText={}, remindAt={}",
                userId, reminderText, remindAt);

        if (reminderText == null || reminderText.isBlank()) {
            return "提醒内容不能为空。";
        }
        if (remindAt == null || remindAt.isBlank()) {
            return "提醒时间不能为空。";
        }

        try {
            LocalDateTime remindTime = parseReminderTime(remindAt);
            if (remindTime == null) {
                return "无法解析提醒时间: " + remindAt + "。请使用 '30分钟后'、'2小时后' 或 ISO 格式 (2026-07-15T15:00:00)。";
            }

            if (remindTime.isBefore(LocalDateTime.now())) {
                return "提醒时间不能早于当前时间。";
            }

            ChatReminder reminder = new ChatReminder();
            reminder.setUserId(userId);
            reminder.setReminderText(reminderText);
            reminder.setRemindTime(remindTime);
            reminder.setStatus(0);
            chatReminderRepository.save(reminder);

            return "提醒已创建：\n- 内容: " + reminderText
                    + "\n- 时间: " + remindTime.format(DISPLAY_FORMAT)
                    + "\n- 距离现在: " + ChronoUnit.MINUTES.between(LocalDateTime.now(), remindTime) + " 分钟";
        } catch (Exception e) {
            log.error("[Tool] createReminder 执行失败: {}", e.getMessage(), e);
            return "创建提醒失败: " + e.getMessage();
        }
    }

    /**
     * 查看待处理的提醒
     *
     * @return 提醒列表
     */
    @Tool(description = "查看当前用户的所有待处理提醒事项。当用户问'我有哪些提醒'、'列出提醒'时调用。")
    public String listReminders() {
        String userId = toolContextProvider.getCurrentUserId();
        log.info("[Tool] listReminders 被调用: userId={}", userId);

        try {
            List<ChatReminder> reminders = chatReminderRepository.findPendingByUserId(userId);
            if (reminders.isEmpty()) {
                return "当前没有待处理的提醒事项。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("待处理提醒（共 ").append(reminders.size()).append(" 条）：\n\n");
            for (ChatReminder reminder : reminders) {
                sb.append("- [").append(reminder.getRemindTime().format(DISPLAY_FORMAT)).append("] ")
                        .append(reminder.getReminderText()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] listReminders 执行失败: {}", e.getMessage(), e);
            return "查看提醒失败: " + e.getMessage();
        }
    }

    /**
     * 解析提醒时间
     */
    private LocalDateTime parseReminderTime(String timeStr) {
        // 尝试相对时间解析（如"30分钟后"、"2小时后"）
        Matcher matcher = RELATIVE_TIME_PATTERN.matcher(timeStr);
        if (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            LocalDateTime now = LocalDateTime.now();
            return switch (unit) {
                case "秒" -> now.plusSeconds(amount);
                case "分钟" -> now.plusMinutes(amount);
                case "小时" -> now.plusHours(amount);
                case "天" -> now.plusDays(amount);
                default -> null;
            };
        }

        // 尝试 ISO-8601 格式
        try {
            return LocalDateTime.parse(timeStr);
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }

        // 尝试 yyyy-MM-dd HH:mm 格式
        try {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException ignored) {
            // 无法解析
        }

        return null;
    }
}
