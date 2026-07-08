package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

/**
 * 技术栈创建/更新请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class WorkExperienceStackSaveRequest {

    private Long experienceId;

    private String techName;

    private Integer sortOrder;
}
