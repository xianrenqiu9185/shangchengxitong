package com.linli.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.cart.service.CartService;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.common.idempotency.IdempotencyClaim;
import com.linli.mall.common.idempotency.IdempotencyService;
import com.linli.mall.inventory.service.InventoryService;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.dto.OrderItemRequest;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.entity.OrderItem;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderItemMapper;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderService;
import com.linli.mall.order.support.OrderAmounts;
import com.linli.mall.product.entity.Product;
import com.linli.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String BIZ_TYPE_ORDER_CREATE = "ORDER_CREATE";

    @Value("${order.pay.timeout.minutes:30}")
    private int payTimeoutMinutes;

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final IdempotencyService idempotencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(Long userId, String idemKey, CreateOrderRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        validate(request);

        String orderNo = generateOrderNo();
        IdempotencyClaim claim = idempotencyService.claim(
                userId, BIZ_TYPE_ORDER_CREATE, idemKey, orderNo);
        if (!claim.acquired()) {
            Order existing = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, claim.bizNo()));
            if (existing == null) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "幂等订单不存在");
            }
            return toResponse(existing);
        }

        // 1. 校验商品并计算金额
        List<Product> products = new ArrayList<>(request.getItems().size());
        long totalAmount = 0L;
        for (OrderItemRequest item : request.getItems()) {
            Product product = productService.requireOnSale(item.getProductId());
            products.add(product);
            totalAmount = Math.addExact(totalAmount,
                    Math.multiplyExact(product.getPrice(), item.getQuantity().longValue()));
        }

        // 2. 锁定库存（与建单同一本地事务，失败整体回滚）
        for (OrderItemRequest item : request.getItems()) {
            inventoryService.lock(item.getProductId(), item.getQuantity());
        }

        // 3. 创建订单与订单项
        long deliveryFee = OrderAmounts.deliveryFee(request.getFulfillType(), totalAmount);
        long payAmount = totalAmount + deliveryFee;
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAddressId(request.getAddressId());
        order.setFulfillType(request.getFulfillType());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(0L);
        order.setPayAmount(payAmount);
        // TODO 优惠券核销接入后，discountAmount / payAmount 需按最终金额复算
        order.setCouponId(request.getCouponId());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setPayDeadline(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest req = request.getItems().get(i);
            Product product = products.get(i);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpec(product.getSpec());
            item.setPrice(product.getPrice());
            item.setQuantity(req.getQuantity());
            item.setAmount(Math.multiplyExact(product.getPrice(), req.getQuantity().longValue()));
            orderItemMapper.insert(item);
        }

        // 4. 清理购物车勾选项 + 幂等回写
        cartService.removeChecked(userId);
        idempotencyService.succeed(userId, BIZ_TYPE_ORDER_CREATE, idemKey, orderNo);
        return toResponse(order);
    }

    private CreateOrderResponse toResponse(Order order) {
        return new CreateOrderResponse(order.getOrderNo(), order.getPayAmount(),
                OrderStatus.of(order.getStatus()).name());
    }

    private void validate(CreateOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单商品不能为空");
        }
        if (request.getFulfillType() == null
                || (request.getFulfillType() != 1 && request.getFulfillType() != 2)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "履约方式非法");
        }
        if (request.getFulfillType() == 1 && request.getAddressId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "配送订单必须选择收货地址");
        }
        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "订单商品参数非法");
            }
        }
    }

    private String generateOrderNo() {
        String time = ORDER_NO_TIME_FORMAT.format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return "ORD" + time + String.format("%06d", random);
    }
}
