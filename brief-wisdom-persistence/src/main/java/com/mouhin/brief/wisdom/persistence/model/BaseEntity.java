package com.mouhin.brief.wisdom.persistence.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 —— 抽取所有业务表的公共字段
 * <p>
 * 包含：自增主键 id、创建时间、更新时间、逻辑删除标记。
 * 子类只需声明业务字段，无需重复定义公共字段。
 * <p>
 * 使用方式：
 * <pre>
 * {@code @Data}
 * {@code @EqualsAndHashCode(callSuper = true)}
 * {@code @TableName("xxx_table")}
 * public class XxxEntity extends BaseEntity { }
 * </pre>
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间（插入时自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
