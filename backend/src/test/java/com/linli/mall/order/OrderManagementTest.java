package com.linli.mall.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.service.InventoryService;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.dto.OrderItemRequest;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.mapper.OrderMapper;
import com.linli.mall.order.service.OrderManagementService;
import com.linli.mall.order.service.OrderQueryService;
import com.linli.mall.order.service.OrderService;
import com.linli.mall.payment.dto.CreatePaymentRequest;
import com.linli.mall.payment.dto.PaymentCallbackRequest;
import com.linli.mall.payment.dto.PaymentView;
import com.linli.mall.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderManagementTest {

    private static final Long MEMBER_ID = 3L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderManagementService orderManagementService;

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void cancelShouldReleaseInventory() {
        Inventory before = inventoryService.getByProductId(1L);
        CreateOrderResponse response = createOrder();
        Inventory locked = inventoryService.getByProductId(1L);
        assertThat(locked.getLocked()).isEqualTo(before.getLocked() + 1);

        orderManagementService.cancel(response.orderNo(), MEMBER_ID);

        Order order = orderQueryService.getByOrderNo(response.orderNo());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        Inventory after = inventoryService.getByProductId(1L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked());
    }

    @Test
    void timeoutCloseShouldReleaseInventory() {
        Inventory before = inventoryService.getByProductId(1L);
        CreateOrderResponse response = createOrder();

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, response.orderNo())
                .set(Order::getPayDeadline, LocalDateTime.now().minusMinutes(1)));

        boolean closed = orderManagementService.closeIfPending(response.orderNo());

        assertThat(closed).isTrue();
        Order order = orderQueryService.getByOrderNo(response.orderNo());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        Inventory after = inventoryService.getByProductId(1L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked());
    }

    @Test
    void shipAndConfirmShouldCompleteOrder() {
        CreateOrderResponse response = createOrder();
        pay(response);

        orderManagementService.ship(response.orderNo());
        Order shipped = orderQueryService.getByOrderNo(response.orderNo());
        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.DELIVERING.getCode());

        orderManagementService.confirm(response.orderNo(), MEMBER_ID);
        Order completed = orderQueryService.getByOrderNo(response.orderNo());
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED.getCode());
    }

    @Test
    void pickupShouldCompleteSelfPickupOrder() {
        CreateOrderResponse response = createOrder();
        pay(response);

        orderManagementService.ship(response.orderNo());
        orderManagementService.pickup(response.orderNo(), MEMBER_ID);

        Order completed = orderQueryService.getByOrderNo(response.orderNo());
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED.getCode());
    }

    @Test
    void cancelPaidOrderShouldFail() {
        CreateOrderResponse response = createOrder();
        pay(response);

        assertThatThrownBy(() -> orderManagementService.cancel(response.orderNo(), MEMBER_ID))
                .isInstanceOf(BizException.class);
    }

    private CreateOrderResponse createOrder() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setFulfillType(2);
        request.setItems(List.of(item));
        return orderService.createOrder(MEMBER_ID, UUID.randomUUID().toString(), request);
    }

    private void pay(CreateOrderResponse order) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderNo(order.orderNo());
        request.setChannel(1);
        PaymentView payment = paymentService.createPayment(
                MEMBER_ID, UUID.randomUUID().toString(), request);

        PaymentCallbackRequest callback = new PaymentCallbackRequest();
        callback.setPayNo(payment.payNo());
        callback.setTradeNo("TRADE-" + payment.payNo());
        callback.setAmount(payment.amount());
        callback.setSign("sandbox");
        paymentService.handleCallback(callback);
    }
}
