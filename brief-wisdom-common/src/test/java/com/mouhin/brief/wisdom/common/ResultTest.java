package com.mouhin.brief.wisdom.common;

import com.mouhin.brief.wisdom.enums.BizExceptionEnums;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一响应封装测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-03
 */
@DisplayName("Result 统一响应封装测试")
class ResultTest {

    @Test
    @DisplayName("success(data) 应返回成功结果，携带 code=0000")
    void testSuccessWithData() {
        Result<String> result = Result.success("hello");

        assertTrue(result.isSuccess());
        assertEquals("0000", result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    @DisplayName("success() 无参版本应返回成功结果，data 为 null")
    void testSuccessWithoutData() {
        Result<Void> result = Result.success();

        assertTrue(result.isSuccess());
        assertEquals("0000", result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail(errMsg) 应返回失败结果，code 默认为 SYSTEM_ERROR")
    void testFailWithMessage() {
        Result<Object> result = Result.fail("出错了");

        assertFalse(result.isSuccess());
        assertEquals(BizExceptionEnums.SYSTEM_ERROR.getCode(), result.getCode());
        assertEquals("出错了", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("fail(code, errMsg) 应返回带自定义错误码的失败结果")
    void testFailWithCodeAndMessage() {
        Result<Object> result = Result.fail("1001", "参数错误");

        assertFalse(result.isSuccess());
        assertEquals("1001", result.getCode());
        assertEquals("参数错误", result.getMsg());
    }

    @Test
    @DisplayName("fail(BizExceptionEnums) 应使用枚举的 code 和 message")
    void testFailWithEnum() {
        Result<Object> result = Result.fail(BizExceptionEnums.DATA_NOT_FOUND);

        assertFalse(result.isSuccess());
        assertEquals("1003", result.getCode());
        assertEquals("数据不存在", result.getMsg());
    }

    @Test
    @DisplayName("fail(BizExceptionEnums, detail) 应使用枚举的 code 但覆盖 message")
    void testFailWithEnumAndDetail() {
        Result<Object> result = Result.fail(BizExceptionEnums.PARAM_ERROR, "用户名不能为空");

        assertFalse(result.isSuccess());
        assertEquals("1001", result.getCode());
        assertEquals("用户名不能为空", result.getMsg());
    }
}
