package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型 DTO
 */
/**
 * AiModelDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
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
    private Double inputPricePerMillion;  // 每百万输入token价格(元)
    private Double outputPricePerMillion;  // 每百万输出token价格(元)
    private String thinkingMode;  // 思考模式: normal-普通模式, thinking-思考模式
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
