package com.linli.mall.order.support;

/**
 * 订单金额规则（与前端 Mock 保持一致）。
 */
public final class OrderAmounts {

    public static final long FREE_DELIVERY_THRESHOLD = 3990L;
    public static final long DELIVERY_FEE = 300L;

    private OrderAmounts() {
    }

    public static long deliveryFee(int fulfillType, long goodsAmount) {
        if (fulfillType == 2) {
            return 0L;
        }
        return goodsAmount >= FREE_DELIVERY_THRESHOLD ? 0L : DELIVERY_FEE;
    }
}
