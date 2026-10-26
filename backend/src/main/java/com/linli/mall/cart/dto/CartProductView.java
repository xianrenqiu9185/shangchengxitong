package com.linli.mall.cart.dto;

public record CartProductView(
        Long id,
        String name,
        String image,
        String spec,
        String unit,
        Long price,
        Integer stock,
        Integer status,
        Boolean available,
        Integer sales
) {
}
