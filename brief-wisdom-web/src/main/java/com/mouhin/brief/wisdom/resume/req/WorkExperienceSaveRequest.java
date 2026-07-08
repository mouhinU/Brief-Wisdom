package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

/**
 * 工作经历创建/更新请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class WorkExperienceSaveRequest {

    private String title;

    private String job;

    private String description;

    private Integer sortOrder;

    /**
     * 是否在简历页面显示: 1-显示, 0-隐藏
     */
    private Integer isVisible;
}
