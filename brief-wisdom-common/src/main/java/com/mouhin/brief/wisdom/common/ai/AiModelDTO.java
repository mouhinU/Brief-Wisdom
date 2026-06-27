package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型 DTO
 */
@Data
public class AiModelDTO implements Serializable {
    private Long id;
    private String modelName;
    private String displayName;
    private String provider;
    private String description;
    private Integer isActive;
    private Integer isEnabled;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
