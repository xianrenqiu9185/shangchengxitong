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
import com.linli.mall.order.service.OrderManagementService;
import com.linli.mall.order.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderManagementServiceImpl implements OrderManagementService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderQueryService orderQueryService;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, Long userId) {
        Order order = orderQueryService.getByOrderNo(orderNo, userId);
        if (Integer.valueOf(OrderStatus.CANCELLED.getCode()).equals(order.getStatus())) {
            return;
        }
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int affected = orderMapper.transition(orderNo,
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        releaseItems(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(String orderNo, Long userId) {
        Order order = orderQueryService.getByOrderNo(orderNo, userId);
        if (Integer.valueOf(OrderStatus.COMPLETED.getCode()).equals(order.getStatus())) {
            return;
        }
        if (!Integer.valueOf(OrderStatus.DELIVERING.getCode()).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int affected = orderMapper.transition(orderNo,
                OrderStatus.DELIVERING.getCode(), OrderStatus.COMPLETED.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(String orderNo, Long userId) {
        Order order = orderQueryService.getByOrderNo(orderNo, userId);
        if (!Integer.valueOf(2).equals(order.getFulfillType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅自提订单可核销");
        }
        if (Integer.valueOf(OrderStatus.COMPLETED.getCode()).equals(order.getStatus())) {
            return;
        }
        if (!Integer.valueOf(OrderStatus.DELIVERING.getCode()).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int affected = orderMapper.transition(orderNo,
                OrderStatus.DELIVERING.getCode(), OrderStatus.COMPLETED.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(String orderNo) {
        Order order = orderQueryService.getByOrderNo(orderNo);
        if (Integer.valueOf(OrderStatus.DELIVERING.getCode()).equals(order.getStatus())) {
            return;
        }
        if (!Integer.valueOf(OrderStatus.PAID.getCode()).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        int affected = orderMapper.transition(orderNo,
                OrderStatus.PAID.getCode(), OrderStatus.DELIVERING.getCode());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeIfPending(String orderNo) {
        int affected = orderMapper.transition(orderNo,
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode());
        if (affected == 0) {
            return false;
        }
        Order order = orderQueryService.getByOrderNo(orderNo);
        releaseItems(order.getId());
        return true;
    }

    private void releaseItems(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            inventoryService.release(item.getProductId(), item.getQuantity());
        }
    }
}
