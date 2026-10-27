package com.linli.mall.order.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.entity.OrderItem;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderItemMapper;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.review.integration.ReviewOrderItem;
import com.linli.mall.review.integration.ReviewOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价模块到订单模块的 Port 适配器（由订单模块实现）。
 */
@Service
@RequiredArgsConstructor
public class ReviewOrderPortAdapter implements ReviewOrderPort {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public ReviewOrderItem findReviewableItem(Long userId, String orderNo, Long orderId,
                                              Long orderItemId, Long productId) {
        if (userId == null || (orderId == null && !hasText(orderNo))
                || (orderItemId == null && productId == null)) {
            return null;
        }
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(orderId != null, Order::getId, orderId)
                .eq(hasText(orderNo), Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(orderQuery);
        if (order == null || !Integer.valueOf(OrderStatus.COMPLETED.getCode()).equals(order.getStatus())) {
            return null;
        }

        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(orderItemId != null, OrderItem::getId, orderItemId)
                .eq(productId != null, OrderItem::getProductId, productId)
                .orderByAsc(OrderItem::getId);
        List<OrderItem> items = orderItemMapper.selectList(itemQuery);
        if (items.isEmpty()) {
            return null;
        }
        OrderItem item = items.get(0);
        return new ReviewOrderItem(order.getId(), item.getId(), item.getProductId(), item.getProductName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
