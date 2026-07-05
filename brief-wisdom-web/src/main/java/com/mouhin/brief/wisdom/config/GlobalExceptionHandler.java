package com.mouhin.brief.wisdom.config;

import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import com.mouhin.brief.wisdom.exception.AuthException;
import com.mouhin.brief.wisdom.exception.BizException;
import com.mouhin.brief.wisdom.exception.ContentSecurityException;
import com.mouhin.brief.wisdom.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获 Controller 层抛出的异常，转换为标准 Result 格式返回，
 * 所有响应均携带错误码（code），便于前端做精细化错误处理。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
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
     * 业务异常 —— 携带错误码，返回 200（兼容前端现有逻辑）
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<?>> handleBizException(BizException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String code = e.getCode() != null ? e.getCode() : BizExceptionEnums.SYSTEM_ERROR.getCode();
        log.warn("[业务异常] code={}, message={}", code, e.getMessage());
        return ResponseEntity.ok(Result.fail(code, e.getMessage()));
    }

    /**
     * 内容安全拦截异常 —— 返回 400
     */
    @ExceptionHandler(ContentSecurityException.class)
    public ResponseEntity<Result<?>> handleContentSecurityException(ContentSecurityException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[内容安全] 请求被拦截: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.CONTENT_SECURITY_BLOCKED, e.getMessage()));
    }

    /**
     * 限流异常 —— 返回 429
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Result<?>> handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[限流] 请求被限流: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Result.fail(BizExceptionEnums.RATE_LIMIT_EXCEEDED, e.getMessage()));
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.PARAM_FORMAT_ERROR, message));
    }

    /**
     * 缺少必要请求参数 —— 返回 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<?>> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[缺少参数] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.PARAM_MISSING,
                        "缺少必要参数: " + e.getParameterName()));
    }

    /**
     * 参数类型不匹配 —— 返回 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[参数类型错误] {}: {}", e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.PARAM_TYPE_ERROR,
                        "参数 " + e.getName() + " 类型错误"));
    }

    /**
     * 请求体 JSON 解析失败 —— 返回 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[JSON解析失败] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.PARAM_FORMAT_ERROR, "请求体格式错误，请检查JSON格式"));
    }

    /**
     * 请求方法不支持 —— 返回 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[方法不支持] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(BizExceptionEnums.PARAM_ERROR,
                        "不支持 " + e.getMethod() + " 请求方法"));
    }

    /**
     * 非法参数异常 —— 返回 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.warn("[参数错误] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(BizExceptionEnums.PARAM_ERROR, e.getMessage()));
    }

    /**
     * 静态资源未找到 —— 静默处理（Chrome DevTools 探测请求等）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<?>> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.debug("[资源未找到] {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(BizExceptionEnums.DATA_NOT_FOUND, "资源不存在"));
    }

    /**
     * 认证授权异常 —— 返回 401
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Result<?>> handleAuthException(AuthException e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        String code = e.getCode() != null ? e.getCode() : BizExceptionEnums.UNAUTHORIZED.getCode();
        log.warn("[认证异常] code={}, message={}", code, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(code, e.getMessage()));
    }

    /**
     * 其他未知异常 —— 返回 200 + 错误信息（兼容现有前端处理逻辑）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e, HttpServletRequest request) {
        clearProducibleMediaTypes(request);
        log.error("未知异常: ", e);
        return ResponseEntity.ok(Result.fail(BizExceptionEnums.SYSTEM_ERROR, "服务异常，请稍后重试"));
    }
}
