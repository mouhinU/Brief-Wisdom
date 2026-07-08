package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

/**
 * 项目成果创建/更新请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class ProjectAchievementSaveRequest {

    private Long projectId;

    private String content;

    private Integer sortOrder;
}
