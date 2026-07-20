package com.mouhin.brief.wisdom.common;

import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应封装
 * <p>
 * 所有 API 返回值统一使用此类包装，包含 success、code、msg、data 四个字段。
 * 前端通过 success 判断是否成功，通过 code 做精细化错误处理，通过 msg 展示提示信息。
 *
 * @author Brief-Wisdom
 * @date 2026-06-30
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误码（0000 表示成功，其他见 BizExceptionEnums）
     */
    private String code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 返回成功结果（携带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的 Result 对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(BizExceptionEnums.SUCCESS.getCode());
        result.setMsg(BizExceptionEnums.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 返回成功结果（无数据）
     *
     * @param <T> 数据类型
     * @return 成功的 Result 对象
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回失败结果（使用默认系统错误码）
     *
     * @param errMsg 错误提示信息
     * @param <T>    数据类型
     * @return 失败的 Result 对象
     */
    public static <T> Result<T> fail(String errMsg) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(BizExceptionEnums.SYSTEM_ERROR.getCode());
        result.setMsg(errMsg);
        return result;
    }

    /**
     * 返回失败结果（指定错误码和错误信息）
     *
     * @param code   错误码
     * @param errMsg 错误提示信息
     * @param <T>    数据类型
     * @return 失败的 Result 对象
     */
    public static <T> Result<T> fail(String code, String errMsg) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(errMsg);
        return result;
    }

    /**
     * 返回失败结果（使用异常枚举的默认消息）
     *
     * @param exceptionEnum 业务异常枚举
     * @param <T>           数据类型
     * @return 失败的 Result 对象
     */
    public static <T> Result<T> fail(BizExceptionEnums exceptionEnum) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(exceptionEnum.getCode());
        result.setMsg(exceptionEnum.getMessage());
        return result;
    }

    /**
     * 返回失败结果（使用异常枚举的错误码，自定义消息覆盖默认消息）
     *
     * @param exceptionEnum 业务异常枚举
     * @param detail        自定义错误详情
     * @param <T>           数据类型
     * @return 失败的 Result 对象
     */
    public static <T> Result<T> fail(BizExceptionEnums exceptionEnum, String detail) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(exceptionEnum.getCode());
        result.setMsg(detail);
        return result;
    }
}
