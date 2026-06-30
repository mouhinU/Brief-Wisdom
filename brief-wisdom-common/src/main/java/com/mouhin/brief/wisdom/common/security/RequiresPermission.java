package com.mouhin.brief.wisdom.common.security;

import java.lang.annotation.*;

/**
 * API 权限校验注解
 * <p>
 * 标注在 Controller 方法或类上，表示访问该接口需要拥有对应的权限标识。
 * 权限标识与 sys_menu 表的 permission 字段对应。
 * <p>
 * 校验逻辑：
 * <ul>
 *   <li>super_admin 角色自动拥有所有权限</li>
 *   <li>其他角色通过 sys_role_menu 关联查询是否拥有对应 permission</li>
 * </ul>
 *
 * <pre>
 * // 示例：标注在方法上
 * {@code @RequiresPermission("user:list")}
 * public List<UserDTO> listUsers() { ... }
 *
 * // 示例：标注在类上（所有方法都需要该权限）
 * {@code @RequiresPermission("system:settings")}
 * public class SystemController { ... }
 * </pre>
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识，如 "user:list", "role:manage", "system:settings"
     */
    String value();
}
