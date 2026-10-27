package com.linli.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.service.InventoryService;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.entity.OrderItem;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderItemMapper;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (Integer.valueOf(OrderStatus.PAID.getCode()).equals(order.getStatus())) {
            return true;
        }
        if (Integer.valueOf(OrderStatus.CANCELLED.getCode()).equals(order.getStatus())) {
            return false;
        }

        int affected = orderMapper.transition(orderNo,
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.PAID.getCode());
        if (affected == 0) {
            Order latest = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderNo));
            if (latest != null && Integer.valueOf(OrderStatus.PAID.getCode()).equals(latest.getStatus())) {
                return true;
            }
            if (latest != null && Integer.valueOf(OrderStatus.CANCELLED.getCode()).equals(latest.getStatus())) {
                return false;
            }
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            inventoryService.deduct(item.getProductId(), item.getQuantity());
        }
        return true;
    }
}
