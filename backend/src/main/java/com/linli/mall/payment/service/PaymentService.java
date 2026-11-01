package com.linli.mall.payment.service;

import com.linli.mall.payment.dto.CreatePaymentRequest;
import com.linli.mall.payment.dto.PaymentCallbackRequest;
import com.linli.mall.payment.dto.PaymentView;

public interface PaymentService {

    PaymentView createPayment(Long userId, String idempotencyKey, CreatePaymentRequest request);

    PaymentView getPayment(Long userId, String payNo);

    void handleCallback(PaymentCallbackRequest request);

    /**
     * 沙箱延迟回调：由定时任务调用。
     */
    void processSandboxCallback(String payNo);
}
