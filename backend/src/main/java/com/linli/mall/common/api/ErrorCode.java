package com.linli.mall.common.api;

/**
 * 统一错误码（与需求文档 6.2.4 对齐，后续按模块补充）。
 */
public enum ErrorCode {

    SUCCESS(0, "OK"),

    BAD_REQUEST(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录或登录失效"),
    FORBIDDEN(10003, "无操作权限"),
    ACCOUNT_DISABLED(10005, "账号已被禁用"),

    INVENTORY_NOT_ENOUGH(20003, "库存不足"),

    ORDER_STATUS_INVALID(30002, "订单状态不允许该操作"),
    DUPLICATE_REQUEST(30004, "重复请求"),
    ORDER_CLOSED(30005, "订单已关闭"),

    NOT_FOUND(99001, "资源不存在或无权访问"),
    SYSTEM_ERROR(90001, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
