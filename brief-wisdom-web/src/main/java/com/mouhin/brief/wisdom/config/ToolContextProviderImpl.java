package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.common.tool.ToolContextProvider;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.system.service.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具上下文提供者实现
 * <p>
 * 基于 UserContextHelper 实现 ToolContextProvider 接口，
 * 为 AI 工具模块提供用户上下文信息。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Component
@RequiredArgsConstructor
public class ToolContextProviderImpl implements ToolContextProvider {

    private final UserContextHelper userContextHelper;

    @Override
    public String getCurrentUserId() {
        return userContextHelper.getCurrentUserId();
    }

    @Override
    public boolean isAdmin() {
        ChatUser user = userContextHelper.getCurrentUser();
        if (user == null) {
            return false;
        }
        String level = user.getUserLevel();
        return "super_admin".equals(level) || "admin".equals(level);
    }

    @Override
    public boolean isSuperAdmin() {
        ChatUser user = userContextHelper.getCurrentUser();
        if (user == null) {
            return false;
        }
        return "super_admin".equals(user.getUserLevel());
    }
}
