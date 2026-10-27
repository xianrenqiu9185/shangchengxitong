package com.linli.mall.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.common.idempotency.IdempotencyClaim;
import com.linli.mall.common.idempotency.IdempotencyService;
import com.linli.mall.order.entity.Order;
import com.linli.mall.order.enums.OrderStatus;
import com.linli.mall.order.service.OrderPaymentService;
import com.linli.mall.order.service.OrderQueryService;
import com.linli.mall.payment.dto.CreatePaymentRequest;
import com.linli.mall.payment.dto.PaymentCallbackRequest;
import com.linli.mall.payment.dto.PaymentView;
import com.linli.mall.payment.entity.Payment;
import com.linli.mall.payment.entity.Refund;
import com.linli.mall.payment.enums.PayStatus;
import com.linli.mall.payment.mapper.PaymentMapper;
import com.linli.mall.payment.mapper.RefundMapper;
import com.linli.mall.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String BIZ_TYPE_PAYMENT_CREATE = "PAYMENT_CREATE";
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderQueryService orderQueryService;
    private final OrderPaymentService orderPaymentService;
    private final IdempotencyService idempotencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentView createPayment(Long userId, String idempotencyKey, CreatePaymentRequest request) {
        Order order = orderQueryService.getByOrderNo(request.getOrderNo(), userId);
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getPayDeadline() != null && order.getPayDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_CLOSED);
        }

        Payment active = paymentMapper.findActiveByOrderId(order.getId());
        if (active != null) {
            return toView(active, order.getOrderNo());
        }
        Payment successful = paymentMapper.findSuccessfulByOrderId(order.getId());
        if (successful != null) {
            return toView(successful, order.getOrderNo());
        }

        String payNo = generateNo("PAY");
        IdempotencyClaim claim = idempotencyService.claim(
                userId, BIZ_TYPE_PAYMENT_CREATE, idempotencyKey, payNo);
        if (!claim.acquired()) {
            Payment existing = selectByPayNo(claim.bizNo());
            if (existing == null) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "幂等支付单不存在");
            }
            return toView(existing, order.getOrderNo());
        }

        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setChannel(request.getChannel() == null ? 1 : request.getChannel());
        payment.setStatus(PayStatus.PENDING_PAYMENT.getCode());
        paymentMapper.insert(payment);

        idempotencyService.succeed(userId, BIZ_TYPE_PAYMENT_CREATE, idempotencyKey, payNo);
        return toView(payment, order.getOrderNo());
    }

    @Override
    public PaymentView getPayment(Long userId, String payNo) {
        Payment payment = selectByPayNo(payNo);
        if (payment == null || !payment.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toView(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(PaymentCallbackRequest request) {
        Payment payment = selectByPayNo(request.getPayNo());
        if (payment == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (request.getAmount() == null || !request.getAmount().equals(payment.getAmount())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "支付金额不一致");
        }
        if (Integer.valueOf(PayStatus.PAID.getCode()).equals(payment.getStatus())) {
            return;
        }
        if (Integer.valueOf(PayStatus.CLOSED.getCode()).equals(payment.getStatus())) {
            return;
        }

        int claimed = paymentMapper.claimForCallback(request.getPayNo());
        if (claimed == 0) {
            Payment latest = selectByPayNo(request.getPayNo());
            if (latest != null && (Integer.valueOf(PayStatus.PAID.getCode()).equals(latest.getStatus())
                    || Integer.valueOf(PayStatus.PAYING.getCode()).equals(latest.getStatus()))) {
                return;
            }
            throw new BizException(ErrorCode.DUPLICATE_REQUEST);
        }

        Order order = orderQueryService.getById(payment.getOrderId());
        boolean paid = orderPaymentService.markPaid(order.getOrderNo());
        paymentMapper.markPaid(request.getPayNo());

        if (!paid) {
            // 支付成功 vs 超时关单竞态：订单已关闭，不复活订单，自动退款补偿
            createCompensationRefund(payment);
            log.warn("支付成功时订单已关闭，已发起退款补偿，orderNo={}, payNo={}",
                    order.getOrderNo(), request.getPayNo());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSandboxCallback(String payNo) {
        Payment payment = selectByPayNo(payNo);
        if (payment == null
                || !Integer.valueOf(PayStatus.PENDING_PAYMENT.getCode()).equals(payment.getStatus())) {
            return;
        }
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPayNo(payNo);
        request.setTradeNo("SANDBOX" + payNo);
        request.setAmount(payment.getAmount());
        request.setSign("sandbox");
        handleCallback(request);
    }

    private void createCompensationRefund(Payment payment) {
        Refund refund = new Refund();
        refund.setRefundNo(generateNo("RF"));
        refund.setPaymentId(payment.getId());
        refund.setOrderId(payment.getOrderId());
        refund.setAfterSaleId(null);
        refund.setAmount(payment.getAmount());
        refund.setStatus(1);
        refund.setRefundTime(LocalDateTime.now());
        refundMapper.insert(refund);
    }

    private Payment selectByPayNo(String payNo) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPayNo, payNo));
    }

    private PaymentView toView(Payment payment) {
        Order order = orderQueryService.getById(payment.getOrderId());
        return toView(payment, order.getOrderNo());
    }

    private PaymentView toView(Payment payment, String orderNo) {
        return new PaymentView(payment.getPayNo(), orderNo, payment.getAmount(),
                PayStatus.of(payment.getStatus()).name(), payment.getPayTime());
    }

    private String generateNo(String prefix) {
        String time = LocalDateTime.now().format(NO_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return prefix + time + String.format("%06d", random);
    }
}
