package com.linli.mall.aftersale.integration;

public record OrderInfo(Long orderId, String orderNo, Long userId, Integer status, Long payAmount) {
}
