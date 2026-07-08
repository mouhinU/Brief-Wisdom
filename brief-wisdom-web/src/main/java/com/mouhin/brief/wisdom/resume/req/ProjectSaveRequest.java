package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

/**
 * 项目经历创建/更新请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class ProjectSaveRequest {

    private Long experienceId;

    private String name;

    private String lifecycle;

    private String background;

    private String duty;

    private Integer sortOrder;
}
