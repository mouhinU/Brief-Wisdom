package com.mouhin.brief.wisdom.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 * <p>
 * 所有需要分页的接口统一使用此类接收参数，避免每个 Controller 重复声明 page/size。
 * 使用方式：Controller 方法参数添加 {@code PageRequest pageRequest}
 * 或在 URL 中传 {@code ?page=1&size=20}。
 * <p>
 * 注意：page 和 size 的合法性由业务层校验，不在 DTO 层使用 JSR-303 注解，
 * 以避免 common 模块引入 validation 依赖。
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从 1 开始），默认 1
     */
    private int page = 1;

    /**
     * 每页条数，默认 20
     */
    private int size = 20;

    /**
     * 计算 MyBatis-Plus 的 offset 值
     *
     * @return (page - 1) * size
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /**
     * 校验并修正分页参数
     *
     * @param maxSize 最大每页条数
     * @return 修正后的自身
     */
    public PageRequest validate(int maxSize) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > maxSize) {
            size = maxSize;
        }
        return this;
    }
}
