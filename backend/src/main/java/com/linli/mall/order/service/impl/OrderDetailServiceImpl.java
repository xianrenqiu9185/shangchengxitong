package com.linli.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.OrderAddressView;
import com.linli.mall.order.dto.OrderDetailView;
import com.linli.mall.order.dto.OrderItemView;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.entity.OrderItem;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderItemMapper;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderDetailService;
import com.linli.mall.order.service.OrderQueryService;
import com.linli.mall.user.entity.User;
import com.linli.mall.user.entity.UserAddress;
import com.linli.mall.user.mapper.UserAddressMapper;
import com.linli.mall.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDetailServiceImpl implements OrderDetailService {

    private final OrderQueryService orderQueryService;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final UserAddressMapper userAddressMapper;
    private final UserMapper userMapper;

    @Override
    public OrderDetailView getDetail(String orderNo, Long userId) {
        return toView(orderQueryService.getByOrderNo(orderNo, userId));
    }

    @Override
    public OrderDetailView getDetailForAdmin(String orderNo) {
        return toView(orderQueryService.getByOrderNo(orderNo));
    }

    @Override
    public PageResult<OrderDetailView> listUser(Long userId, String status, long pageNum, long pageSize) {
        Integer statusCode = parseStatus(status);
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<Order> page = orderMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(statusCode != null, Order::getStatus, statusCode)
                        .orderByDesc(Order::getCreateTime)
                        .orderByDesc(Order::getId));
        return PageResult.of(page.getRecords().stream().map(this::toView).toList(),
                page.getTotal(), safePageNum, safePageSize);
    }

    @Override
    public PageResult<OrderDetailView> listAdmin(String status, String keyword, long pageNum, long pageSize) {
        Integer statusCode = parseStatus(status);
        String safeKeyword = keyword == null ? null : keyword.trim();
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<Order> page = orderMapper.selectPage(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Order>()
                        .eq(statusCode != null, Order::getStatus, statusCode)
                        .like(safeKeyword != null && !safeKeyword.isBlank(), Order::getOrderNo, safeKeyword)
                        .orderByDesc(Order::getCreateTime)
                        .orderByDesc(Order::getId));
        return PageResult.of(page.getRecords().stream().map(this::toView).toList(),
                page.getTotal(), safePageNum, safePageSize);
    }

    private OrderDetailView toView(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        List<OrderItemView> itemViews = items.stream()
                .map(item -> new OrderItemView(item.getProductId(), item.getProductName(), null,
                        item.getPrice(), item.getSpec(), item.getQuantity()))
                .toList();

        OrderAddressView address = null;
        if (order.getAddressId() != null) {
            UserAddress userAddress = userAddressMapper.selectById(order.getAddressId());
            if (userAddress != null) {
                address = new OrderAddressView(userAddress.getReceiver(), userAddress.getPhone(),
                        "", "", "", userAddress.getDetail());
            }
        }

        User user = userMapper.selectById(order.getUserId());
        String customer = user == null ? null
                : (user.getNickname() == null ? user.getPhone() : user.getNickname());
        String phone = user == null ? null : user.getPhone();

        String deliveryType = Integer.valueOf(2).equals(order.getFulfillType()) ? "PICKUP" : "DELIVERY";
        String pickupCode = "PICKUP".equals(deliveryType)
                ? "A" + order.getOrderNo().substring(Math.max(0, order.getOrderNo().length() - 3))
                : null;

        return new OrderDetailView(order.getOrderNo(), OrderStatus.of(order.getStatus()).name(),
                deliveryType, order.getFulfillType(), order.getTotalAmount(), 0L,
                order.getDiscountAmount(), order.getPayAmount(), customer, phone,
                address, pickupCode, order.getRemark(), order.getCreateTime(), itemViews);
    }

    private Integer parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase()).getCode();
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单状态非法");
        }
    }
}
