package com.linli.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态非法")
        @Max(value = 1, message = "状态非法")
        Integer status
) {
}
