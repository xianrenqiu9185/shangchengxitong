package com.linli.mall.order.service;

import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;

public interface OrderService {

    /**
     * 下单：幂等校验 + 锁定库存 + 创建订单（同一本地事务）。
     *
     * @param userId   当前会员
     * @param idemKey  Idempotency-Key 请求头
     * @param request  下单请求
     */
    CreateOrderResponse createOrder(Long userId, String idemKey, CreateOrderRequest request);
}
