package com.mouhin.brief.wisdom.resume.dto;

import lombok.Data;

import java.util.List;

/**
 * 项目 VO
 */
@Data
public class ProjectVO {

    private String name;
    private String lifecycle;
    private String background;
    private String duty;
    private List<String> achievements;
}
