package com.linli.mall.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AfterSaleCreateRequest(
        @NotBlank(message = "订单号不能为空")
        @Size(max = 32, message = "订单号格式不正确")
        String orderNo,

        @NotBlank(message = "售后类型不能为空")
        @Pattern(regexp = "REFUND|RETURN", message = "售后类型仅支持 REFUND 或 RETURN")
        String type,

        @NotBlank(message = "售后原因不能为空")
        @Size(max = 200, message = "售后原因不能超过 200 个字符")
        String reason,

        @Size(max = 200, message = "补充说明不能超过 200 个字符")
        String description
) {
}
