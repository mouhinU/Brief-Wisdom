package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.persistence.model.AiModel;
import com.mouhin.brief.wisdom.persistence.repository.AiModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AiAgentService 流式 Token 估算与费用计算测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiAgentService 流式 Token 估算测试")
class AiAgentServiceTest {

    @Mock
    private AiModelRepository aiModelRepository;

    @InjectMocks
    private AiAgentService aiAgentService;

    // ===== estimateStreamingTokens 测试 =====

    @Test
    @DisplayName("空内容返回 0 Token")
    void testEstimateTokens_nullContent() {
        assertEquals(0, aiAgentService.estimateStreamingTokens(null));
        assertEquals(0, aiAgentService.estimateStreamingTokens(""));
    }

    @Test
    @DisplayName("短内容至少返回 1 Token")
    void testEstimateTokens_shortContent() {
        assertEquals(1, aiAgentService.estimateStreamingTokens("Hi"));
    }

    @Test
    @DisplayName("中文内容按 3 字符 ≈ 1 Token 估算")
    void testEstimateTokens_chineseContent() {
        // 30 个中文字符 → 约 10 Token
        String content = "这是一段用于测试的中文内容，总共三十个字符长度用来验证估算逻辑";
        int tokens = aiAgentService.estimateStreamingTokens(content);
        assertTrue(tokens > 0, "Token 数量应大于 0");
        assertEquals(content.length() / 3, tokens);
    }

    @Test
    @DisplayName("英文内容按 3 字符 ≈ 1 Token 估算")
    void testEstimateTokens_englishContent() {
        String content = "This is a test response in English for token estimation verification";
        int tokens = aiAgentService.estimateStreamingTokens(content);
        assertTrue(tokens > 0);
        assertEquals(content.length() / 3, tokens);
    }

    // ===== estimateStreamingCost 测试 =====

    @Test
    @DisplayName("模型存在且有定价时正确计算费用")
    void testEstimateStreamingCost_withPricing() {
        AiModel model = new AiModel();
        model.setModelName("qwen-max");
        model.setOutputPricePerMillion(20.0);
        when(aiModelRepository.findByModelName("qwen-max")).thenReturn(model);

        // 1000 Token × 20元/百万 = 0.02 元
        double cost = aiAgentService.estimateStreamingCost("qwen-max", 1000);
        assertEquals(0.02, cost, 0.001);
    }

    @Test
    @DisplayName("模型不存在时返回 0")
    void testEstimateStreamingCost_modelNotFound() {
        when(aiModelRepository.findByModelName("unknown")).thenReturn(null);
        assertEquals(0.0, aiAgentService.estimateStreamingCost("unknown", 1000));
    }

    @Test
    @DisplayName("模型定价为 null 时返回 0")
    void testEstimateStreamingCost_nullPricing() {
        AiModel model = new AiModel();
        model.setModelName("free-model");
        model.setOutputPricePerMillion(null);
        when(aiModelRepository.findByModelName("free-model")).thenReturn(model);

        assertEquals(0.0, aiAgentService.estimateStreamingCost("free-model", 1000));
    }

    @Test
    @DisplayName("查询异常时返回 0 不中断")
    void testEstimateStreamingCost_exceptionHandled() {
        when(aiModelRepository.findByModelName(anyString())).thenThrow(new RuntimeException("DB error"));
        assertEquals(0.0, aiAgentService.estimateStreamingCost("qwen-max", 1000));
    }
}
