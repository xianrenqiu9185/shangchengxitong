package com.linli.mall.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.aftersale.integration.RefundPort;
import com.linli.mall.aftersale.integration.RefundRequest;
import com.linli.mall.aftersale.integration.RefundResult;
import com.linli.mall.aftersale.integration.RefundStatus;
import com.linli.mall.inventory.entity.Inventory;
import com.linli.mall.inventory.service.InventoryService;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.dto.OrderItemRequest;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.service.OrderQueryService;
import com.linli.mall.order.service.OrderService;
import com.linli.mall.order.service.OrderStateMachine;
import com.linli.mall.payment.dto.CreatePaymentRequest;
import com.linli.mall.payment.dto.PaymentCallbackRequest;
import com.linli.mall.payment.dto.PaymentView;
import com.linli.mall.payment.entity.Refund;
import com.linli.mall.payment.mapper.RefundMapper;
import com.linli.mall.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    private static final Long MEMBER_ID = 3L;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundPort refundPort;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderStateMachine orderStateMachine;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RefundMapper refundMapper;

    @Test
    void paymentCallbackShouldMarkOrderPaidAndDeductStock() {
        Inventory before = inventoryService.getByProductId(1L);
        CreateOrderResponse order = createOrder(2);

        PaymentView payment = createPayment(order.orderNo());
        assertThat(payment.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(payment.amount()).isEqualTo(2580L);

        callback(payment.payNo(), payment.amount());

        PaymentView paid = paymentService.getPayment(MEMBER_ID, payment.payNo());
        assertThat(paid.status()).isEqualTo("PAID");

        Inventory after = inventoryService.getByProductId(1L);
        assertThat(after.getLocked()).isEqualTo(before.getLocked());
        assertThat(after.getSold()).isEqualTo(before.getSold() + 2);

        Order paidOrder = orderQueryService.getByOrderNo(order.orderNo());
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID.getCode());
        assertThat(paidOrder.getStatus()).isNotEqualTo(OrderStatus.PENDING_PAYMENT.getCode());
    }

    @Test
    void duplicateCallbackShouldNotDeductTwice() {
        CreateOrderResponse order = createOrder(1);
        PaymentView payment = createPayment(order.orderNo());

        callback(payment.payNo(), payment.amount());
        Inventory afterFirst = inventoryService.getByProductId(1L);
        callback(payment.payNo(), payment.amount());
        Inventory afterSecond = inventoryService.getByProductId(1L);

        assertThat(afterSecond.getSold()).isEqualTo(afterFirst.getSold());
        assertThat(afterSecond.getLocked()).isEqualTo(afterFirst.getLocked());
    }

    @Test
    void paymentAfterOrderClosedShouldCompensateRefund() {
        CreateOrderResponse order = createOrder(1);
        PaymentView payment = createPayment(order.orderNo());

        // 模拟超时关单
        orderStateMachine.transition(order.orderNo(), OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED);

        callback(payment.payNo(), payment.amount());

        PaymentView paid = paymentService.getPayment(MEMBER_ID, payment.payNo());
        assertThat(paid.status()).isEqualTo("PAID");
        Order closed = orderQueryService.getByOrderNo(order.orderNo());
        assertThat(closed.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());

        Long refunds = refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, closed.getId()));
        assertThat(refunds).isEqualTo(1L);
    }

    @Test
    void refundPortShouldCreateRefundRecordIdempotently() {
        CreateOrderResponse order = createOrder(1);
        PaymentView payment = createPayment(order.orderNo());
        callback(payment.payNo(), payment.amount());

        Order paidOrder = orderQueryService.getByOrderNo(order.orderNo());
        String refundNo = "RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        RefundResult first = refundPort.refund(new RefundRequest(
                refundNo, "AS-" + refundNo, paidOrder.getId(), paidOrder.getPayAmount()));
        RefundResult second = refundPort.refund(new RefundRequest(
                refundNo, "AS-" + refundNo, paidOrder.getId(), paidOrder.getPayAmount()));

        assertThat(first.status()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(second.status()).isEqualTo(RefundStatus.SUCCESS);
        Long refunds = refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo));
        assertThat(refunds).isEqualTo(1L);
    }

    @Test
    void createPaymentShouldReuseActivePayment() {
        CreateOrderResponse order = createOrder(1);
        PaymentView first = createPayment(order.orderNo());
        PaymentView second = createPayment(order.orderNo());
        assertThat(second.payNo()).isEqualTo(first.payNo());
    }

    private CreateOrderResponse createOrder(int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(quantity);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setFulfillType(2);
        request.setItems(List.of(item));
        return orderService.createOrder(MEMBER_ID, UUID.randomUUID().toString(), request);
    }

    private PaymentView createPayment(String orderNo) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderNo(orderNo);
        request.setChannel(1);
        return paymentService.createPayment(MEMBER_ID, UUID.randomUUID().toString(), request);
    }

    private void callback(String payNo, long amount) {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPayNo(payNo);
        request.setTradeNo("TRADE-" + payNo);
        request.setAmount(amount);
        request.setSign("sandbox");
        paymentService.handleCallback(request);
    }
}
