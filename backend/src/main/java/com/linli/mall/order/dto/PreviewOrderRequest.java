package com.linli.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PreviewOrderRequest {

    @NotNull(message = "履约方式不能为空")
    private Integer fulfillType;

    private Long couponId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemRequest> items;
}
