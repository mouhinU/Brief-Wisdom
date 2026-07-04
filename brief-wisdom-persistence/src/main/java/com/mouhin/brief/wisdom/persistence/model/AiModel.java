package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI模型配置实体类
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model")
public class AiModel extends BaseEntity {

    @TableField(value = "model_name")
    private String modelName;

    @TableField(value = "display_name")
    private String displayName;

    @TableField(value = "provider")
    private String provider;

    @TableField(value = "description")
    private String description;

    @TableField(value = "is_active")
    private Integer isActive;

    @TableField(value = "is_enabled")
    private Integer isEnabled;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    @TableField(value = "input_price_per_million")
    private Double inputPricePerMillion;  // 每百万输入token价格(元)

    @TableField(value = "output_price_per_million")
    private Double outputPricePerMillion;  // 每百万输出token价格(元)
}
