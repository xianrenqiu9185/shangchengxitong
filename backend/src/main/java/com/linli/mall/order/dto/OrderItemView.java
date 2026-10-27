package com.linli.mall.order.dto;

public record OrderItemView(Long productId, String name, String image, Long price,
                            String unit, Integer quantity) {
}
