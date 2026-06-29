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
@RestControllerAdvice
public class ResultAutoWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResultAutoWrapperAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // void 方法不处理
        if (returnType.getParameterType() == void.class) {
            return false;
        }
        // ResponseEntity 不处理（由 HttpEntityMethodProcessor 自行处理）
        return !ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
    }

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
