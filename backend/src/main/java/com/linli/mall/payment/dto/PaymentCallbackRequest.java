package com.linli.mall.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCallbackRequest {

    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    private String tradeNo;

    @NotNull(message = "金额不能为空")
    private Long amount;

    /**
     * 沙箱验签字段，本期只做非空/简单校验。
     */
    private String sign;
}
