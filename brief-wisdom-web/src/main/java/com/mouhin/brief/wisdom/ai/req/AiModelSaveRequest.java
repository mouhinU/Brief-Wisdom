package com.mouhin.brief.wisdom.ai.req;

import lombok.Data;

/**
 * AI 模型创建请求
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@Data
public class AiModelSaveRequest {

    private String modelName;

    private String displayName;

    private String provider;

    private String description;

    private Integer sortOrder;

    private Double inputPricePerMillion;

    private Double outputPricePerMillion;

    private String thinkingMode;
}
