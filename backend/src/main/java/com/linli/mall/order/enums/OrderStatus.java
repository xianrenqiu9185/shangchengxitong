package com.linli.mall.order.enums;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态与合法流转。
 */
public enum OrderStatus {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "待发货"),
    DELIVERING(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    AFTER_SALE(5, "售后中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(DELIVERING, AFTER_SALE),
            DELIVERING, Set.of(COMPLETED, AFTER_SALE),
            COMPLETED, Set.of(AFTER_SALE),
            AFTER_SALE, Set.of(PAID, DELIVERING, COMPLETED, REFUNDED),
            CANCELLED, Set.of(),
            REFUNDED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + code);
    }
}
