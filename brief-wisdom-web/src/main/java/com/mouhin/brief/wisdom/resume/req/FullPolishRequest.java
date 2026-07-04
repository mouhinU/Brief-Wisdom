package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

import java.util.List;

/**
 * 全文润色请求 —— 传入完整简历数据，AI 逐字段给出优化建议
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Data
public class FullPolishRequest {

    /** 个人描述/求职意向 */
    private String personalSummary;

    /** 工作经历列表（JSON 结构） */
    private List<ExperienceItem> experiences;

    /**
     * 工作经历项
     */
    @Data
    public static class ExperienceItem {

        /** 公司名称 */
        private String company;

        /** 职位 */
        private String position;

        /** 工作经历描述 */
        private String description;

        /** 项目列表 */
        private List<ProjectItem> projects;
    }

    /**
     * 项目项
     */
    @Data
    public static class ProjectItem {

        /** 项目名称 */
        private String name;

        /** 项目背景 */
        private String background;

        /** 个人职责 */
        private String duty;

        /** 项目成果列表 */
        private List<String> achievements;
    }
}
