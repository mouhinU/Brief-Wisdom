package com.mouhin.brief.wisdom.common.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 多端同步状态 DTO
 * <p>
 * 轻量级同步指纹，前端通过对比此对象判断是否需要刷新数据。
 */
/**
 * SyncStatusDTO
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class SyncStatusDTO implements Serializable {

    /**
     * 当前用户的会话总数
     */
    private int sessionCount;

    /**
     * 每个会话的消息数量
     */
    private Map<String, Integer> sessionMessageCounts;

    /**
     * 每个会话的最后一条消息时间戳（毫秒）
     */
    private Map<String, Long> sessionLastMessageTimes;

    /**
     * 同步指纹（基于以上数据计算的哈希值，前端直接对比即可）
     */
    private String fingerprint;
}
