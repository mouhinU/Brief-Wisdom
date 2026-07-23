package com.mouhin.brief.wisdom.common.ai;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型传输对象
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record AiModelDTO(
        Long id,
        String modelName,
        String displayName,
        String provider,
        String description,
        Integer isActive,
        Integer isEnabled,
        Integer sortOrder,
        Double inputPricePerMillion,
        Double outputPricePerMillion,
        String thinkingMode,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {

    public Long getId() { return id; }

    public String getModelName() { return modelName; }

    public String getDisplayName() { return displayName; }

    public String getProvider() { return provider; }

    public String getDescription() { return description; }

    public Integer getIsActive() { return isActive; }

    public Integer getIsEnabled() { return isEnabled; }

    public Integer getSortOrder() { return sortOrder; }

    public Double getInputPricePerMillion() { return inputPricePerMillion; }

    public Double getOutputPricePerMillion() { return outputPricePerMillion; }

    public String getThinkingMode() { return thinkingMode; }

    public LocalDateTime getCreateTime() { return createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
}
