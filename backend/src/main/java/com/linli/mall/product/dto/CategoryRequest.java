package com.linli.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过 50 个字符")
        String name,

        @Min(value = 0, message = "排序值不能小于 0")
        Integer sort,

        @Min(value = 0, message = "状态非法")
        @Max(value = 1, message = "状态非法")
        Integer status
) {
}
