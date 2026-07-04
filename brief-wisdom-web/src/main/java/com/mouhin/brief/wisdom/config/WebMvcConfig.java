package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置
 * <p>
 * 注册自定义拦截器：
 * <ul>
 *   <li>{@link PermissionInterceptor} - 基于 @RequiresPermission 注解的 API 权限校验</li>
 * </ul>
 */
/**
 * WebMvcConfig
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**"); // 只拦截 API 路径
    }
}
