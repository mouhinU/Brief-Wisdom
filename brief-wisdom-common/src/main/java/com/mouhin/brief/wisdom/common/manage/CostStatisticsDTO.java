package com.mouhin.brief.wisdom.common.manage;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 费用统计 DTO
 *
 * @author Brief-Wisdom
 * @date 2026-07-02
 */
@Data
public class CostStatisticsDTO implements Serializable {

    /**
     * 总费用（元）
     */
    private Double totalCost;

    /**
     * 总 token 数
     */
    private Long totalTokens;

    /**
     * 总消息数（assistant 消息）
     */
    private Long totalMessages;

    /**
     * 按模型分组统计
     */
    private List<ModelCostItem> byModel = new ArrayList<>();

    /**
     * 按用户分组统计
     */
    private List<UserCostItem> byUser = new ArrayList<>();

    /**
     * 按日期分组统计
     */
    private List<DateCostItem> byDate = new ArrayList<>();

    /**
     * 按模型+日期分组统计
     */
    private List<DateModelCostItem> byDateAndModel = new ArrayList<>();

    /**
     * 模型费用项
     */
    @Data
    public static class ModelCostItem implements Serializable {
        private String model;
        private Long messageCount;
        private Double totalCost;
        private Long totalTokens;
    }

    /**
     * 用户费用项
     */
    @Data
    public static class UserCostItem implements Serializable {
        private String userId;
        private String userName;
        private Long messageCount;
        private Double totalCost;
        private Long totalTokens;
    }

    /**
     * 日期费用项
     */
    @Data
    public static class DateCostItem implements Serializable {
        private String date;
        private Long messageCount;
        private Double totalCost;
        private Long totalTokens;
    }

    /**
     * 日期+模型费用项
     */
    @Data
    public static class DateModelCostItem implements Serializable {
        private String date;
        private String model;
        private Long messageCount;
        private Double totalCost;
    }
}
