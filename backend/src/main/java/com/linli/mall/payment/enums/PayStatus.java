package com.linli.mall.payment.enums;

/**
 * 支付状态（API 使用字符串枚举）。
 */
public enum PayStatus {

    PENDING_PAYMENT(0, "待支付"),
    PAYING(1, "支付中"),
    PAID(2, "支付成功"),
    FAILED(3, "支付失败"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String label;

    PayStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PayStatus of(int code) {
        for (PayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown pay status: " + code);
    }
}
