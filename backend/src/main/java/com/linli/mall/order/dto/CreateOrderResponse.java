package com.linli.mall.order.dto;

/**
 * @param orderNo   订单号
 * @param payAmount 应付金额（分）
 * @param status    订单状态（字符串枚举，如 PENDING_PAYMENT）
 */
public record CreateOrderResponse(String orderNo, Long payAmount, String status) {
}
