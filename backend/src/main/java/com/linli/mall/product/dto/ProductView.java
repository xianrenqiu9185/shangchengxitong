package com.linli.mall.product.dto;

import java.time.LocalDateTime;

public record ProductView(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String spec,
        String subtitle,
        String unit,
        String image,
        String description,
        Long price,
        Long originalPrice,
        Integer status,
        Integer stock,
        Integer sales,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime updatedAt
) {
}
