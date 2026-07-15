package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户提醒事项实体类
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_reminder")
public class ChatReminder extends BaseEntity {

    /**
     * 用户 ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 提醒内容
     */
    @TableField(value = "reminder_text")
    private String reminderText;

    /**
     * 提醒时间
     */
    @TableField(value = "remind_time")
    private LocalDateTime remindTime;

    /**
     * 状态: 0-待处理, 1-已完成, 2-已过期, 3-已取消
     */
    @TableField(value = "status")
    private Integer status;
}
