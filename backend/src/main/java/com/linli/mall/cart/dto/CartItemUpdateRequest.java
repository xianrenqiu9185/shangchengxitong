package com.linli.mall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartItemUpdateRequest(
        @Min(value = 1, message = "数量必须大于 0")
        @Max(value = 99, message = "单商品数量不能超过 99")
        Integer quantity,

        Boolean checked
) {
}
