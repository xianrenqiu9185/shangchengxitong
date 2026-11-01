package com.linli.mall.common.api;

import org.slf4j.MDC;

/**
 * 统一返回体。
 *
 * @param code      业务码，0 表示成功
 * @param message   提示信息
 * @param data      业务数据
 * @param traceId   链路追踪 ID
 * @param timestamp 服务器时间戳（毫秒）
 */
public record Result<T>(int code, String message, T data, String traceId, long timestamp) {

    private static final String TRACE_ID_KEY = "traceId";

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "OK", data, MDC.get(TRACE_ID_KEY), System.currentTimeMillis());
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, MDC.get(TRACE_ID_KEY), System.currentTimeMillis());
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
