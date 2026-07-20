package com.mouhin.brief.wisdom.resume.dto;

import lombok.Data;

import java.util.List;

/**
 * 项目 VO
 */

/**
 * ProjectVO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class ProjectVO {

    private Long id;
    private String name;
    private String lifecycle;
    private String background;
    private String duty;
    private List<String> achievements;
}
