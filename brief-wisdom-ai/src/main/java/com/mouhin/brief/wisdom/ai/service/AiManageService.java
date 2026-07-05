package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.manage.CostStatisticsDTO;
import com.mouhin.brief.wisdom.common.manage.MessageDTO;
import com.mouhin.brief.wisdom.common.manage.SessionDTO;
import com.mouhin.brief.wisdom.common.manage.UserDTO;

import java.util.List;

/**
 * AI助手管理服务接口 - 按用户级别查询会话历史
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
public interface AiManageService {

    /**
     * 获取所有用户（含级别信息）
     *
     * @return 用户 DTO 列表
     */
    List<UserDTO> listUsers();

    /**
     * 按用户级别查询用户列表
     *
     * @param userLevel 用户级别
     * @return 用户 DTO 列表
     */
    List<UserDTO> listUsersByLevel(String userLevel);

    /**
     * 获取所有可用的用户级别
     *
     * @return 用户级别列表
     */
    List<String> listUserLevels();

    /**
     * 查询指定用户的会话列表
     *
     * @param userId 用户 ID
     * @return 会话 DTO 列表
     */
    List<SessionDTO> listSessionsByUserId(String userId);

    /**
     * 按用户级别查询会话列表（查询该级别下所有用户的会话）
     *
     * @param userLevel 用户级别
     * @return 会话 DTO 列表
     */
    List<SessionDTO> listSessionsByUserLevel(String userLevel);

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话 ID
     * @return 消息 DTO 列表
     */
    List<MessageDTO> getSessionMessages(String sessionId);

    /**
     * 获取费用统计数据
     *
     * @param days 按日期统计的天数范围
     * @return 多维度费用统计
     */
    CostStatisticsDTO getCostStatistics(int days);
}
