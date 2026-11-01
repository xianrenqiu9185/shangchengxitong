package com.linli.mall.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailView(
        String orderNo,
        String status,
        String deliveryType,
        Integer fulfillType,
        Long goodsAmount,
        Long deliveryFee,
        Long discountAmount,
        Long payAmount,
        String customer,
        String phone,
        OrderAddressView address,
        String pickupCode,
        String remark,
        LocalDateTime createdAt,
        List<OrderItemView> items
) {
}
