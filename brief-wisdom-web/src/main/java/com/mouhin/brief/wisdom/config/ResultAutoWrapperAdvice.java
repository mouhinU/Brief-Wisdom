package com.mouhin.brief.wisdom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mouhin.brief.wisdom.common.Result;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应自动封装
 * <p>
 * Controller 返回值遵循以下规则：
 * <ul>
 *   <li>返回 {@code Result<T>} → 原样输出，不重复包装</li>
 *   <li>返回 {@code ResponseEntity<?>} → 原样输出，不重复包装</li>
 *   <li>返回 {@code void} → 不处理</li>
 *   <li>返回其他任意对象 → 自动包装为 {@code Result.success(body)}</li>
 * </ul>
 */
/**
 * ResultAutoWrapperAdvice
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@RestControllerAdvice
public class ResultAutoWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    /**
     * 构造响应自动封装器
     *
     * @param objectMapper JSON 序列化工具
     */
    public ResultAutoWrapperAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 判断是否需要自动包装响应
     *
     * @param returnType    返回值类型
     * @param converterType 消息转换器类型
     * @return true 表示需要包装
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // void 方法不处理
        if (returnType.getParameterType() == void.class) {
            return false;
        }
        // ResponseEntity 不处理（由 HttpEntityMethodProcessor 自行处理）
        return !ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
    }

    /**
     * 响应体写入前自动包装为 Result
     *
     * @param body            原始返回值
     * @param methodParameter 方法参数
     * @param mediaType       媒体类型
     * @param clazz           消息转换器类型
     * @param request         HTTP 请求
     * @param response        HTTP 响应
     * @return 包装后的响应对象
     */
    @Override
    @SneakyThrows
    public Object beforeBodyWrite(Object body, MethodParameter methodParameter,
                                  MediaType mediaType, Class<? extends HttpMessageConverter<?>> clazz,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 已经是 Result，直接返回
        if (body instanceof Result) {
            return body;
        }
        // 包装为 Result
        Result<Object> result = Result.success(body);

        // String 类型使用 StringHttpMessageConverter，需要手动序列化为 JSON
        if (clazz == StringHttpMessageConverter.class) {
            return objectMapper.writeValueAsString(result);
        }

        return result;
    }
}
