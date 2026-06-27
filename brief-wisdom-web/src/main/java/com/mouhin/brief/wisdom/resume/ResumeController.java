package com.mouhin.brief.wisdom.resume;

import com.mouhin.brief.wisdom.resume.dto.WorkExperienceVO;
import com.mouhin.brief.wisdom.resume.service.ResumeService;
import com.mouhin.brief.wisdom.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历数据 REST 接口
 */
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 获取所有工作经历（含项目、成果、技术栈）
     * <p>
     * 返回结构与原 project.json 一致，前端无需修改渲染逻辑。
     */
    @GetMapping("/experiences")
    public ApiResponse<List<WorkExperienceVO>> listExperiences() {
        try {
            List<WorkExperienceVO> data = resumeService.listAllExperiences();
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("获取简历数据失败: ", e);
            return ApiResponse.fail("获取简历数据失败: " + e.getMessage());
        }
    }
}
