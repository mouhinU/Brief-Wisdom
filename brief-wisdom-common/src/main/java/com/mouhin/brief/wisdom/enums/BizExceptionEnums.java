package com.mouhin.brief.wisdom.enums;

import lombok.Getter;

/**
 * 系统常见业务异常枚举
 * <p>
 * 统一定义常见异常的错误码和错误消息，便于全局管理和国际化扩展。
 * 使用方式：throw new BizException(BizExceptionEnums.PARAM_ERROR, "用户名不能为空");
 */
/**
 * BizExceptionEnums 枚举
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Getter
public enum BizExceptionEnums {

    // ==================== 通用异常 (1000-1099) ====================
    SUCCESS("0000", "操作成功"),
    SYSTEM_ERROR("1000", "系统异常，请稍后重试"),
    PARAM_ERROR("1001", "参数错误"),
    PARAM_MISSING("1002", "缺少必要参数"),
    DATA_NOT_FOUND("1003", "数据不存在"),
    DATA_ALREADY_EXISTS("1004", "数据已存在"),
    OPERATION_FAILED("1005", "操作失败"),
    OPERATION_NOT_ALLOWED("1006", "操作不被允许"),
    STATUS_ERROR("1007", "状态异常"),

    // ==================== 认证授权异常 (1100-1199) ====================
    UNAUTHORIZED("1100", "未登录或登录已过期"),
    FORBIDDEN("1101", "无权限访问"),
    TOKEN_INVALID("1102", "Token 无效"),
    TOKEN_EXPIRED("1103", "Token 已过期"),
    USER_DISABLED("1104", "账号已被禁用"),
    LOGIN_FAILED("1105", "登录失败，用户名或密码错误"),

    // ==================== 参数校验异常 (1200-1299) ====================
    PARAM_FORMAT_ERROR("1200", "参数格式错误"),
    PARAM_LENGTH_ERROR("1201", "参数长度超限"),
    PARAM_RANGE_ERROR("1202", "参数超出范围"),
    PARAM_TYPE_ERROR("1203", "参数类型错误"),

    // ==================== 数据相关异常 (1300-1399) ====================
    DUPLICATE_KEY("1300", "数据重复"),
    DATA_IN_USE("1301", "数据正在被使用，无法操作"),
    DATA_LOCKED("1302", "数据已被锁定"),
    DATA_VERSION_ERROR("1303", "数据版本冲突，请刷新后重试"),

    // ==================== 限流/安全异常 (1400-1499) ====================
    RATE_LIMIT_EXCEEDED("1400", "请求过于频繁，请稍后再试"),
    CONTENT_SECURITY_BLOCKED("1401", "内容包含敏感信息，请修改后重试"),
    REQUEST_INVALID("1402", "非法请求"),
    IP_BLOCKED("1403", "IP 已被限制访问"),

    // ==================== 外部服务异常 (1500-1599) ====================
    THIRD_PARTY_ERROR("1500", "第三方服务异常"),
    AI_SERVICE_ERROR("1501", "AI 服务异常，请稍后重试"),
    AI_RESPONSE_TIMEOUT("1502", "AI 响应超时"),
    AI_TOKEN_LIMIT("1503", "AI Token 用量已达上限"),
    SMS_SEND_FAILED("1504", "短信发送失败"),
    EMAIL_SEND_FAILED("1505", "邮件发送失败"),

    // ==================== 文件相关异常 (1600-1699) ====================
    FILE_NOT_FOUND("1600", "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED("1601", "文件类型不支持"),
    FILE_SIZE_EXCEEDED("1602", "文件大小超出限制"),
    FILE_UPLOAD_FAILED("1603", "文件上传失败"),
    FILE_DOWNLOAD_FAILED("1604", "文件下载失败"),

    // ==================== 简历模块异常 (1700-1799) ====================
    RESUME_NOT_FOUND("1700", "简历不存在"),
    RESUME_UPDATE_FAILED("1701", "简历更新失败"),
    RESUME_AI_POLISH_FAILED("1702", "AI 简历润色失败，请稍后重试"),
    RESUME_DATA_INVALID("1703", "简历数据格式不正确"),

    ;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 默认错误消息
     */
    private final String message;

    BizExceptionEnums(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static BizExceptionEnums getByCode(String code) {
        for (BizExceptionEnums e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
