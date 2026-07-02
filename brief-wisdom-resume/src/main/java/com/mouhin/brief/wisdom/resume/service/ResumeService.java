package com.mouhin.brief.wisdom.resume.service;

import com.mouhin.brief.wisdom.resume.dto.WorkExperienceVO;

import java.util.List;

/**
 * 简历数据服务接口
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface ResumeService {

    /**
     * 获取所有工作经历（含项目、成果、技术栈）
     *
     * @return 工作经历列表，按 sort_order 排序
     */
    List<WorkExperienceVO> listAllExperiences();
}
