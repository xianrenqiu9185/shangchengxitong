package com.linli.mall.order.dto;

public record OrderPreviewView(Long goodsAmount, Long deliveryFee, Long discountAmount,
                               Long payAmount) {
}
