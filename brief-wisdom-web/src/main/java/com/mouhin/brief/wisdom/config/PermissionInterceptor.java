package com.mouhin.brief.wisdom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.common.security.RequiresPermission;
import com.mouhin.brief.wisdom.persistence.model.ChatUser;
import com.mouhin.brief.wisdom.web.controller.WechatAuthController;
import com.mouhin.brief.wisdom.web.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 基于 @RequiresPermission 注解的 API 权限校验拦截器
 * <p>
 * 校验逻辑：
 * <ol>
 *   <li>检查 Handler 方法或其所在类是否标注了 @RequiresPermission</li>
 *   <li>从 Session 获取当前用户</li>
 *   <li>super_admin 角色自动放行</li>
 *   <li>查询用户角色关联的权限标识，检查是否包含注解要求的权限</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final RoleService roleService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非 Controller 方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取权限注解：优先方法级别，其次类级别
        RequiresPermission methodAnnotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        RequiresPermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        RequiresPermission annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        // 没有注解，放行
        if (annotation == null) {
            return true;
        }

        String requiredPermission = annotation.value();

        // 获取当前用户
        HttpSession session = request.getSession(false);
        if (session == null) {
            writeForbiddenResponse(response, requiredPermission);
            return false;
        }

        ChatUser user = (ChatUser) session.getAttribute(WechatAuthController.SESSION_USER_KEY);
        if (user == null) {
            writeForbiddenResponse(response, requiredPermission);
            return false;
        }

        // 获取用户权限
        List<String> permissions = roleService.getUserPermissions(user.getUserId());

        // super_admin 拥有所有权限（getUserPermissions 返回 null）
        if (permissions == null) {
            log.debug("[权限校验] 用户 {} 拥有 super_admin 角色，放行: {}", user.getUserId(), requiredPermission);
            return true;
        }

        // 检查是否拥有要求的权限
        if (permissions.contains(requiredPermission)) {
            log.debug("[权限校验] 用户 {} 拥有权限 {}，放行", user.getUserId(), requiredPermission);
            return true;
        }

        // 权限不足
        log.warn("[权限校验] 用户 {} 缺少权限 {}，拒绝访问: {} {}",
                user.getUserId(), requiredPermission, request.getMethod(), request.getRequestURI());
        writeForbiddenResponse(response, requiredPermission);
        return false;
    }

    /**
     * 写入 403 权限不足响应
     */
    private void writeForbiddenResponse(HttpServletResponse response, String requiredPermission) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.fail("权限不足，需要权限: " + requiredPermission);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
