package com.linli.mall.aftersale.integration;

public final class OrderStatus {

    public static final int PENDING_PAYMENT = 0;
    public static final int PENDING_SHIPMENT = 1;
    public static final int PENDING_RECEIPT = 2;
    public static final int COMPLETED = 3;
    public static final int CANCELLED = 4;
    public static final int AFTER_SALE = 5;
    public static final int REFUNDED = 6;

    private OrderStatus() {
    }

    public static boolean canApplyAfterSale(Integer status) {
        return status != null
                && (status == PENDING_SHIPMENT || status == PENDING_RECEIPT || status == COMPLETED);
    }
}
