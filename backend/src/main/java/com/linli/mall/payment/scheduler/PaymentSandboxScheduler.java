package com.linli.mall.payment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linli.mall.payment.entity.Payment;
import com.linli.mall.payment.mapper.PaymentMapper;
import com.linli.mall.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 沙箱支付回调模拟：支付单创建一段时间后自动触发一次成功回调。
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PaymentSandboxScheduler {

    private final PaymentMapper paymentMapper;
    private final PaymentService paymentService;

    @Value("${payment.sandbox.callback-delay-ms:2000}")
    private long callbackDelayMs;

    @Value("${payment.sandbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.sandbox.callback-interval-ms:1000}")
    public void processPendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minus(Duration.ofMillis(callbackDelayMs));
        int limit = Math.max(1, Math.min(batchSize, 200));
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getStatus, 0)
                .le(Payment::getCreateTime, threshold)
                .orderByAsc(Payment::getId)
                .last("LIMIT " + limit));
        for (Payment payment : payments) {
            try {
                paymentService.processSandboxCallback(payment.getPayNo());
            } catch (Exception e) {
                log.warn("沙箱支付回调失败，payNo={}", payment.getPayNo(), e);
            }
        }
    }
}
