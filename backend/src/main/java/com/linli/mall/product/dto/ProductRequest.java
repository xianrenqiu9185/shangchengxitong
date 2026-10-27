package com.linli.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotNull(message = "分类不能为空")
        Long categoryId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称不能超过 128 个字符")
        String name,

        @Size(max = 128, message = "规格不能超过 128 个字符")
        String spec,

        @Size(max = 255, message = "图片地址不能超过 255 个字符")
        String image,

        String description,

        @NotNull(message = "售价不能为空")
        @Min(value = 0, message = "售价不能为负数")
        Long price,

        @Min(value = 0, message = "状态非法")
        @Max(value = 1, message = "状态非法")
        Integer status
) {
}
