package com.mouhin.brief.wisdom.common;

import lombok.Data;

/**
 * 统一 API 响应封装
 */
@Data
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String msg;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> fail(String errMsg) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMsg(errMsg);
        return response;
    }
}
