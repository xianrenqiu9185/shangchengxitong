package com.linli.mall.aftersale.integration;

/**
 * 售后到支付模块的退款 Port，由 A 的支付模块实现。refundNo 必须幂等。
 */
public interface RefundPort {

    RefundResult refund(RefundRequest request);
}
