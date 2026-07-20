package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

import java.util.List;

/**
 * AI 简历建议请求 —— 传入完整简历数据，AI 给出整体优化建议
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Data
public class ResumeSuggestRequest {

    /**
     * 个人描述/求职意向
     */
    private String personalSummary;

    /**
     * 工作经历列表（JSON 结构）
     */
    private List<FullPolishRequest.ExperienceItem> experiences;

    /**
     * 期望分析的维度：overall-整体评估, layout-排版建议, content-内容优化, keywords-关键词优化
     */
    private List<String> dimensions;
}
