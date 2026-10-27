package com.linli.mall.order.service;

public interface OrderManagementService {

    void cancel(String orderNo, Long userId);

    void confirm(String orderNo, Long userId);

    void pickup(String orderNo, Long userId);

    void ship(String orderNo);

    /**
     * 超时关单：仅当前为待支付时关闭并释放库存。
     */
    boolean closeIfPending(String orderNo);
}
