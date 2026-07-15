package com.mouhin.brief.wisdom.ai.service.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间感知工具
 * <p>
 * 让 AI 助手能够获取当前精确的日期和时间信息。
 * 解决 AI 模型本身不知道当前时间的问题。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
public class DateTimeTool {

    private static final DateTimeFormatter FULL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 获取当前日期和时间
     *
     * @param timezone 时区 ID，默认 Asia/Shanghai
     * @return 格式化的日期时间字符串
     */
    @Tool(description = "获取当前日期和时间。当用户询问今天日期、当前时间、星期几、今年是哪一年时调用。")
    public String getCurrentDateTime(
            @ToolParam(description = "时区ID，如 Asia/Shanghai、America/New_York，默认 Asia/Shanghai", required = false)
            String timezone) {

        log.info("[Tool] getCurrentDateTime 被调用: timezone={}", timezone);

        try {
            ZoneId zoneId = parseTimezone(timezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);

            String[] weekDays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            int dayOfWeek = now.getDayOfWeek().getValue() - 1;
            String weekDay = (dayOfWeek >= 0 && dayOfWeek < weekDays.length) ? weekDays[dayOfWeek] : "未知";

            return String.format("当前时间信息：\n日期: %s\n时间: %s\n星期: %s\n时区: %s\n完整时间: %s",
                    now.format(DATE_FORMAT),
                    now.format(TIME_FORMAT),
                    weekDay,
                    zoneId.getId(),
                    now.format(FULL_FORMAT));
        } catch (Exception e) {
            log.error("[Tool] getCurrentDateTime 执行失败: {}", e.getMessage(), e);
            return "获取时间失败: " + e.getMessage();
        }
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param date1 第一个日期（yyyy-MM-dd 格式）
     * @param date2 第二个日期（yyyy-MM-dd 格式）
     * @return 天数差
     */
    @Tool(description = "计算两个日期之间的天数差。当用户问'距离某个日期还有多少天'、'两个日期相差多久'时调用。")
    public String daysBetween(
            @ToolParam(description = "第一个日期，格式 yyyy-MM-dd") String date1,
            @ToolParam(description = "第二个日期，格式 yyyy-MM-dd") String date2) {

        log.info("[Tool] daysBetween 被调用: date1={}, date2={}", date1, date2);

        try {
            java.time.LocalDate d1 = java.time.LocalDate.parse(date1);
            java.time.LocalDate d2 = java.time.LocalDate.parse(date2);
            long days = java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
            return date1 + " 到 " + date2 + " 相差 " + Math.abs(days) + " 天";
        } catch (Exception e) {
            return "日期计算失败，请确保日期格式为 yyyy-MM-dd: " + e.getMessage();
        }
    }

    /**
     * 解析时区字符串
     */
    private ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("Asia/Shanghai");
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("无效时区: {}，使用默认 Asia/Shanghai", timezone);
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
