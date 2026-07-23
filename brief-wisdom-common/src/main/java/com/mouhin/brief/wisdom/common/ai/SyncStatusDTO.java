package com.mouhin.brief.wisdom.common.ai;

import java.io.Serializable;
import java.util.Map;

/**
 * 多端同步状态传输对象
 * <p>
 * 轻量级同步指纹，前端通过对比此对象判断是否需要刷新数据。
 *
 * @author Brief-Wisdom
 * @date 2026-07-22
 */
public record SyncStatusDTO(
        int sessionCount,
        Map<String, Integer> sessionMessageCounts,
        Map<String, Long> sessionLastMessageTimes,
        String fingerprint
) implements Serializable {

    public int getSessionCount() { return sessionCount; }

    public Map<String, Integer> getSessionMessageCounts() { return sessionMessageCounts; }

    public Map<String, Long> getSessionLastMessageTimes() { return sessionLastMessageTimes; }

    public String getFingerprint() { return fingerprint; }
}
