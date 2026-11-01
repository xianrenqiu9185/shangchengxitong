package com.linli.mall.cart.dto;

public record CartItemView(
        Long id,
        Long productId,
        Integer quantity,
        Boolean checked,
        CartProductView product,
        Long subtotal,
        Boolean available
) {
}
