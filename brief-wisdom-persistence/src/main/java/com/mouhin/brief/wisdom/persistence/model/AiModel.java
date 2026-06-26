package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型配置实体类
 */
@Data
@TableName("ai_model")
public class AiModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
