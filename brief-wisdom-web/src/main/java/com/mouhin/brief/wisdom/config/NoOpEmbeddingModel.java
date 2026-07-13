package com.mouhin.brief.wisdom.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.Collections;

/**
 * 空操作的 EmbeddingModel 实现。
 * <p>
 * 当未配置默认 AI 提供商或 api-key 时作为降级 Bean 使用，
 * 避免应用启动失败。所有操作返回空结果并打印警告日志。
 *
 * @author Brief-Wisdom
 * @date 2026-07-13
 */
@Slf4j
public class NoOpEmbeddingModel implements EmbeddingModel {

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        log.warn("[NoOpEmbeddingModel] EmbeddingModel 未配置，忽略 embedding 请求");
        return new EmbeddingResponse(Collections.emptyList());
    }

    @Override
    public float[] embed(Document document) {
        log.warn("[NoOpEmbeddingModel] EmbeddingModel 未配置，返回空向量");
        return new float[0];
    }
}
