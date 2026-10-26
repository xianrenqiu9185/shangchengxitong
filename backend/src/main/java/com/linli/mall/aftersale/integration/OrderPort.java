package com.linli.mall.aftersale.integration;

import java.util.Collection;
import java.util.Map;

/**
 * 售后到订单模块的单向 Port，由 A 的订单模块实现。
 */
public interface OrderPort {

    OrderInfo findByOrderNoForUser(String orderNo, Long userId);

    Map<Long, OrderInfo> findByIds(Collection<Long> orderIds);

    void markAfterSale(Long orderId, int expectedStatus);

    void restoreStatus(Long orderId, int targetStatus);

    void markRefunded(Long orderId);
}
