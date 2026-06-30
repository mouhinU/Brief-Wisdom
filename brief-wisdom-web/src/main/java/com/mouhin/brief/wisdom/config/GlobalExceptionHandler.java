 package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.exception.ContentSecurityException;
import com.mouhin.brief.wisdom.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获 Controller 层抛出的异常，转换为标准 Result 格式返回，
 * Controller 无需再写 try-catch 块。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 清除原始 Handler 的 produces 约束，防止异常处理器返回 JSON 时
     * 因 MediaType 不匹配而抛出 HttpMediaTypeNotAcceptableException。
     * <p>
     * 例如 SSE 端点 produces="text/event-stream"，异常时 GlobalExceptionHandler
     * 需要返回 application/json，若不清除 producibleMediaTypes 会导致内容协商失败。
     */
    private void clearProducibleMediaTypes(HttpServletRequest request) {
        request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
    }

    /**
     * 内容安全拦截异常 —— 返回 400
     */
    @ExceptionHandler(ContentSecurityException.class)
    public ResponseEntity<Result<?>> handleContentSecurityException(ContentSecurityException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[内容安全] 请求被拦截: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(e.getMessage()));
    }

    /**
     * 限流异常 —— 返回 429
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Result<?>> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[限流] 请求被限流: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Result.fail(e.getMessage()));
    }

    /**
     * 参数校验异常（Spring Validation）—— 返回 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[参数校验] {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(message));
    }

    /**
     * 非法参数异常 —— 返回 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[参数错误] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(e.getMessage()));
    }

    /**
     * 静态资源未找到 —— 静默处理（Chrome DevTools 探测请求等）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<?>> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        // 不打印堆栈，仅记录简短 debug 日志
        log.debug("[资源未找到] {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.fail("资源不存在"));
    }

    /**
     * 其他未知异常 —— 返回 200 + 错误信息（兼容现有前端处理逻辑）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.error("未知异常: ", e);
        return ResponseEntity.ok(Result.fail("服务异常: " + e.getMessage()));
    }
}
