package com.mouhin.brief.wisdom.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 工具注册配置
 * <p>
 * 将所有 @Tool 注解标注的工具方法注册为 Spring AI ToolCallbackProvider，
 * 供 ChatModel 在对话中按需调用。
 * <p>
 * 工具分三个梯队：
 * - 第一梯队（核心）：知识库搜索、代码搜索、记忆管理、网页抓取
 * - 第二梯队（增强）：日期时间、数学计算、简历分析、系统状态
 * - 第三梯队（扩展）：文本翻译、知识文档管理、定时提醒、SQL 查询
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ToolConfig {

    /**
     * 注册所有 AI 工具
     * <p>
     * 通过 MethodToolCallbackProvider 扫描各工具类中的 @Tool 注解方法，
     * 自动生成 ToolCallback 供 ChatModel 使用。
     *
     * @param knowledgeSearchTool     知识库搜索工具
     * @param codeSearchTool          代码搜索工具
     * @param memoryManagementTool    记忆管理工具
     * @param webFetchTool            网页抓取工具
     * @param dateTimeTool            日期时间工具
     * @param calculatorTool          数学计算工具
     * @param resumeAnalysisTool      简历分析工具
     * @param systemStatusTool        系统状态工具
     * @param translationTool         文本翻译工具
     * @param knowledgeDocMgmtTool    知识文档管理工具
     * @param reminderTool            定时提醒工具
     * @param databaseQueryTool       SQL 查询工具
     * @return ToolCallbackProvider 实例
     */
    @Bean
    public ToolCallbackProvider briefWisdomToolCallbackProvider(
            KnowledgeSearchTool knowledgeSearchTool,
            CodeSearchTool codeSearchTool,
            MemoryManagementTool memoryManagementTool,
            WebFetchTool webFetchTool,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            ResumeAnalysisTool resumeAnalysisTool,
            SystemStatusTool systemStatusTool,
            TranslationTool translationTool,
            KnowledgeDocManagementTool knowledgeDocMgmtTool,
            ReminderTool reminderTool,
            DatabaseQueryTool databaseQueryTool) {

        log.info("[ToolConfig] 注册 AI 工具集：共 12 个工具类");

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        // 第一梯队：核心工具
                        knowledgeSearchTool,
                        codeSearchTool,
                        memoryManagementTool,
                        webFetchTool,
                        // 第二梯队：增强工具
                        dateTimeTool,
                        calculatorTool,
                        resumeAnalysisTool,
                        systemStatusTool,
                        // 第三梯队：扩展工具
                        translationTool,
                        knowledgeDocMgmtTool,
                        reminderTool,
                        databaseQueryTool
                )
                .build();
    }
}
