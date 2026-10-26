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
    PHONE_REGISTERED(10006, "手机号已注册"),
    INVALID_CREDENTIALS(10007, "手机号或密码错误"),
    LOGIN_LOCKED(10008, "登录失败次数过多，请稍后再试"),

    PRODUCT_UNAVAILABLE(20002, "商品不存在或已下架"),
    INVENTORY_NOT_ENOUGH(20003, "库存不足"),
    PRODUCT_NOT_FOUND(20004, "商品不存在"),
    CATEGORY_IN_USE(20005, "分类下存在商品，无法删除"),
    CATEGORY_NAME_DUPLICATE(20006, "分类名称重复"),
    PRODUCT_INVALID(20007, "商品参数非法"),
    CART_QUANTITY_INVALID(20010, "数量非法"),
    CART_FULL(20011, "购物车已满"),

    ORDER_STATUS_INVALID(30002, "订单状态不允许该操作"),
    DUPLICATE_REQUEST(30004, "重复请求"),
    ORDER_CLOSED(30005, "订单已关闭"),

    AFTER_SALE_STATUS_INVALID(40002, "售后状态不允许该操作"),
    AFTER_SALE_NOT_ALLOWED(40003, "不满足售后条件"),
    DUPLICATE_REFUND(40004, "重复退款"),
    REFUND_FAILED(40005, "退款失败"),

    REVIEW_NOT_FOUND(60001, "评价不存在或无权访问"),
    REVIEW_DUPLICATE(60002, "重复评价"),
    REVIEW_NOT_ALLOWED(60003, "订单或订单项不可评价"),

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
