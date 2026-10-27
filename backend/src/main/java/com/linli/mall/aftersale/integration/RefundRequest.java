package com.linli.mall.aftersale.integration;

public record RefundRequest(String refundNo, String saleNo, Long orderId, Long amount) {
}
