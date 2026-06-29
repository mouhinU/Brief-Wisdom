package com.mouhin.brief.wisdom.common;

import lombok.Data;

/**
 * 统一响应封装
 */
@Data
public class Result<T> {

    private boolean success;
    private T data;
    private String msg;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String errMsg) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMsg(errMsg);
        return result;
    }
}
