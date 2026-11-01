package com.linli.mall.order.service;

import com.linli.mall.order.enums.OrderStatus;

/**
 * 订单状态机契约，售后等模块统一通过此接口迁移订单状态。
 */
public interface OrderStateMachine {

    /**
     * 按合法流转表迁移；非法迁移或并发失败抛出 30002。
     */
    void transition(String orderNo, OrderStatus from, OrderStatus to);
}
