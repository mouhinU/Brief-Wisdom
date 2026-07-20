package com.mouhin.brief.wisdom.resume.controller;

import com.mouhin.brief.wisdom.resume.dto.WorkExperienceVO;
import com.mouhin.brief.wisdom.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历数据 REST 接口
 */

/**
 * ResumeController
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "简历展示", description = "简历公开展示接口")
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 获取所有工作经历（含项目、成果、技术栈）
     * <p>
     * 返回结构与原 project.json 一致，前端无需修改渲染逻辑。
     */
    @Operation(summary = "获取工作经历", description = "获取所有工作经历，含项目、成果、技术栈")
    @GetMapping("/experiences")
    public List<WorkExperienceVO> listExperiences() {
        return resumeService.listAllExperiences();
    }
}
