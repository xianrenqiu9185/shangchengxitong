package com.linli.mall.order.controller;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RequirePermission("trade:order:create")
    public Result<CreateOrderResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(orderService.createOrder(userId, idempotencyKey, request));
    }
}
