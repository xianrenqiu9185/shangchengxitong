package com.linli.mall.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePaymentRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 支付渠道，本期沙箱固定 1。
     */
    private Integer channel;
}
