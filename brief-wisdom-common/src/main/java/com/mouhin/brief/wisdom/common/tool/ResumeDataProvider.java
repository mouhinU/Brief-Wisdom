package com.mouhin.brief.wisdom.common.tool;

import java.util.List;
import java.util.Map;

/**
 * 简历数据提供者接口
 * <p>
 * 为 AI 工具提供简历数据访问能力，隔离 resume 模块的直接依赖。
 * 由 resume 模块实现，AI 工具模块通过此接口获取简历数据。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
public interface ResumeDataProvider {

    /**
     * 获取用户的所有工作经历（含项目、成果、技术栈）
     *
     * @return 工作经历列表，每条记录包含 title、job、description、projects、stacks 等字段
     */
    List<Map<String, Object>> listAllExperiences();
}
