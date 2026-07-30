package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 配置
 * <p>
 * 注册自定义拦截器：
 * <ul>
 *   <li>{@link PermissionInterceptor} - 基于 @RequiresPermission 注解的 API 权限校验</li>
 * </ul>
 * 同时为本地存储的多模态上传文件提供静态资源映射。
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

    /**
     * 本地存储根目录（与 app.storage.local.base-dir 保持一致）
     */
    @Value("${app.storage.local.base-dir:./uploads}")
    private String uploadBaseDir;

    /**
     * 本地存储静态资源访问前缀（与 app.storage.local.url-prefix 保持一致）
     */
    @Value("${app.storage.local.url-prefix:/uploads}")
    private String uploadUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**"); // 只拦截 API 路径
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将本地存储目录映射为静态资源，供上传文件的 URL 直接访问
        String absolutePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize().toString();
        String location = "file:" + absolutePath + "/";
        registry.addResourceHandler(uploadUrlPrefix + "/**")
                .addResourceLocations(location);
    }
}
