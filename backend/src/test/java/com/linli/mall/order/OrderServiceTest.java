package com.linli.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.inventory.entity.Inventory;
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
import com.linli.mall.order.service.OrderStateMachine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServiceTest {

    private static final Long MEMBER_ID = 3L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderStateMachine orderStateMachine;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Test
    void createOrderShouldLockInventoryAndCreateOrder() {
        Inventory before = inventoryService.getByProductId(1L);
        String idemKey = UUID.randomUUID().toString();

        CreateOrderResponse response = orderService.createOrder(MEMBER_ID, idemKey, orderRequest(1L, 2));

        assertThat(response.orderNo()).isNotBlank();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
        assertThat(response.payAmount()).isEqualTo(2580L);

        Inventory after = inventoryService.getByProductId(1L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked() + 2);
        assertThat(after.available()).isEqualTo(before.available() - 2);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, response.orderNo()));
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(MEMBER_ID);

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getPrice()).isEqualTo(1290L);
        assertThat(items.get(0).getAmount()).isEqualTo(2580L);
        assertThat(items.get(0).getProductName()).isEqualTo("山东红富士苹果");
    }

    @Test
    void duplicateIdempotencyKeyShouldReturnSameOrderAndLockOnce() {
        Inventory before = inventoryService.getByProductId(1L);
        String idemKey = UUID.randomUUID().toString();

        CreateOrderResponse first = orderService.createOrder(MEMBER_ID, idemKey, orderRequest(1L, 1));
        CreateOrderResponse second = orderService.createOrder(MEMBER_ID, idemKey, orderRequest(1L, 1));

        assertThat(second.orderNo()).isEqualTo(first.orderNo());
        Inventory after = inventoryService.getByProductId(1L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked() + 1);
    }

    @Test
    void insufficientInventoryShouldNotCreateOrder() {
        Inventory before = inventoryService.getByProductId(6L);
        String idemKey = UUID.randomUUID().toString();

        assertThatThrownBy(() -> orderService.createOrder(MEMBER_ID, idemKey,
                orderRequest(6L, before.available() + 1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");

        Inventory after = inventoryService.getByProductId(6L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked());
    }

    @Test
    void invalidTransitionShouldFail() {
        CreateOrderResponse response = orderService.createOrder(MEMBER_ID, UUID.randomUUID().toString(),
                orderRequest(1L, 1));

        assertThatThrownBy(() -> orderStateMachine.transition(
                response.orderNo(), OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED))
                .isInstanceOf(BizException.class);
    }

    @Test
    void validTransitionShouldMoveStatus() {
        CreateOrderResponse response = orderService.createOrder(MEMBER_ID, UUID.randomUUID().toString(),
                orderRequest(1L, 1));

        orderStateMachine.transition(response.orderNo(), OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, response.orderNo()));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID.getCode());
    }

    private CreateOrderRequest orderRequest(Long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setFulfillType(2);
        request.setItems(List.of(item));
        return request;
    }
}
