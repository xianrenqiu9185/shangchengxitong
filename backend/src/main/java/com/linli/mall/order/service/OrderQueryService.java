package com.linli.mall.order.service;

import com.linli.mall.order.entity.Order;

/**
 * 订单只读查询契约，供售后等模块使用。
 */
public interface OrderQueryService {

    /**
     * 按单号查询并校验归属。
     */
    Order getByOrderNo(String orderNo, Long userId);

    /**
     * 按单号查询（内部使用，不校验归属）。
     */
    Order getByOrderNo(String orderNo);
}
