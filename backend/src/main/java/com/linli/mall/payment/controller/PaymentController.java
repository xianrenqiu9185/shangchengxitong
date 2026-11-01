package com.linli.mall.payment.controller;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.payment.dto.CreatePaymentRequest;
import com.linli.mall.payment.dto.PaymentCallbackRequest;
import com.linli.mall.payment.dto.PaymentView;
import com.linli.mall.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @RequirePermission("trade:payment:pay")
    public Result<PaymentView> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(paymentService.createPayment(userId, idempotencyKey, request));
    }

    @GetMapping("/{payNo}")
    public Result<PaymentView> get(@PathVariable String payNo) {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(paymentService.getPayment(userId, payNo));
    }

    @PostMapping("/callback")
    public Result<Void> callback(@Valid @RequestBody PaymentCallbackRequest request) {
        paymentService.handleCallback(request);
        return Result.ok();
    }
}
