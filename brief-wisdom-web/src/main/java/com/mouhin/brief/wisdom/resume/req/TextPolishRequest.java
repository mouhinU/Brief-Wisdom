package com.mouhin.brief.wisdom.resume.req;

import lombok.Data;

/**
 * 文本润色请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Data
public class TextPolishRequest {

    /**
     * 待润色的原始文本
     */
    private String text;

    /**
     * 字段类型：description-工作经历描述, background-项目背景, duty-个人职责, achievement-项目成果
     */
    private String fieldType;

    /**
     * 上下文信息（可选），如公司名称、项目名称等，帮助AI更好地理解语境
     */
    private String context;
}
