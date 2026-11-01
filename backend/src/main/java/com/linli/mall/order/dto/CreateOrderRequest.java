package com.linli.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    /**
     * 配送必填，自提可空。
     */
    private Long addressId;

    /**
     * 1 配送、2 自提。
     */
    @NotNull(message = "履约方式不能为空")
    private Integer fulfillType;

    /**
     * 用户优惠券 ID（本期先保留，优惠计算后续接入）。
     */
    private Long couponId;

    @Size(max = 255, message = "备注最长 255 字符")
    private String remark;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemRequest> items;
}
