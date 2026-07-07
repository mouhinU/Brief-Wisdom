package com.mouhin.brief.wisdom.resume.dto;

import lombok.Data;

import java.util.List;

/**
 * 工作经历 VO（对应前端 JSON 数组的每个元素）
 */
/**
 * WorkExperienceVO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class WorkExperienceVO {

    private Long id;
    private String title;
    private String job;
    private String description;
    private List<ProjectVO> projects;
    private List<String> stacks;
}
