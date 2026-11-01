package com.linli.mall.order.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.aftersale.integration.OrderInfo;
import com.linli.mall.aftersale.integration.OrderPort;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 售后模块到订单模块的 Port 适配器（由 A 的订单模块实现）。
 */
@Service
@RequiredArgsConstructor
public class OrderPortAdapter implements OrderPort {

    private final OrderMapper orderMapper;

    @Override
    public OrderInfo findByOrderNoForUser(String orderNo, Long userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        return order == null ? null : toInfo(order);
    }

    @Override
    public Map<Long, OrderInfo> findByIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, this::toInfo));
    }

    @Override
    public void markAfterSale(Long orderId, int expectedStatus) {
        int affected = orderMapper.transitionById(orderId, expectedStatus,
                OrderStatus.AFTER_SALE.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    public void restoreStatus(Long orderId, int targetStatus) {
        orderMapper.transitionById(orderId, OrderStatus.AFTER_SALE.getCode(), targetStatus);
    }

    @Override
    public void markRefunded(Long orderId) {
        int affected = orderMapper.transitionById(orderId,
                OrderStatus.AFTER_SALE.getCode(), OrderStatus.REFUNDED.getCode());
        if (affected == 0) {
            Order order = orderMapper.selectById(orderId);
            if (order == null
                    || !Integer.valueOf(OrderStatus.REFUNDED.getCode()).equals(order.getStatus())) {
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
            }
        }
    }

    private OrderInfo toInfo(Order order) {
        return new OrderInfo(order.getId(), order.getOrderNo(), order.getUserId(),
                order.getStatus(), order.getPayAmount());
    }
}
