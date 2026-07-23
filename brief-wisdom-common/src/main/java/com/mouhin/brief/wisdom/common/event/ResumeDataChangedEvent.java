package com.mouhin.brief.wisdom.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 简历数据变更事件
 * <p>
 * 当简历模块发生增删改操作后发布此事件，
 * 由 AI 模块监听并触发知识库同步。
 *
 * @author Brief-Wisdom
 * @date 2026-07-20
 */
public class ResumeDataChangedEvent extends ApplicationEvent {

    /**
     * 变更来源描述（用于日志）
     */
    private final String changeDescription;

    public ResumeDataChangedEvent(Object source, String changeDescription) {
        super(source);
        this.changeDescription = changeDescription;
    }

    public String getChangeDescription() {
        return changeDescription;
    }
}
