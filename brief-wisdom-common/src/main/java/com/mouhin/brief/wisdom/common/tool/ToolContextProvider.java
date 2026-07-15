package com.mouhin.brief.wisdom.common.tool;

/**
 * 工具上下文提供者接口
 * <p>
 * 为 AI 工具提供当前用户上下文信息，隔离 web 层的 Servlet/Security 依赖。
 * 由 web 模块实现（基于 UserContextHelper），AI 工具模块通过此接口获取用户信息。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
public interface ToolContextProvider {

    /**
     * 获取当前登录用户的 userId
     *
     * @return 用户ID，不为 null
     */
    String getCurrentUserId();

    /**
     * 判断当前用户是否为管理员（admin 或 super_admin）
     *
     * @return true 表示是管理员
     */
    boolean isAdmin();

    /**
     * 判断当前用户是否为超级管理员
     *
     * @return true 表示是超级管理员
     */
    boolean isSuperAdmin();
}
