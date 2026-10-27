package com.linli.mall.aftersale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AfterSaleAuditRequest(
        @NotNull(message = "审核结果不能为空")
        Boolean approved,

        @Size(max = 255, message = "审核意见不能超过 255 个字符")
        String remark
) {
}
