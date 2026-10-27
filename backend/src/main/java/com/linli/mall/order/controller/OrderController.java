package com.linli.mall.order.controller;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.CreateOrderRequest;
import com.linli.mall.order.dto.CreateOrderResponse;
import com.linli.mall.order.dto.OrderDetailView;
import com.linli.mall.order.dto.OrderPreviewView;
import com.linli.mall.order.dto.PreviewOrderRequest;
import com.linli.mall.order.service.OrderDetailService;
import com.linli.mall.order.service.OrderManagementService;
import com.linli.mall.order.service.OrderPreviewService;
import com.linli.mall.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final OrderManagementService orderManagementService;
    private final OrderPreviewService orderPreviewService;

    @PostMapping("/preview")
    @RequirePermission("trade:order:create")
    public Result<OrderPreviewView> preview(@Valid @RequestBody PreviewOrderRequest request) {
        return Result.ok(orderPreviewService.preview(request));
    }

    @GetMapping
    public Result<PageResult<OrderDetailView>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return Result.ok(orderDetailService.listUser(currentUserId(), status, pageNum, pageSize));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderManagementService.cancel(orderNo, currentUserId());
        return Result.ok();
    }

    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo) {
        orderManagementService.confirm(orderNo, currentUserId());
        return Result.ok();
    }

    @PostMapping("/{orderNo}/pickup")
    public Result<Void> pickup(@PathVariable String orderNo) {
        orderManagementService.pickup(orderNo, currentUserId());
        return Result.ok();
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailView> detail(@PathVariable String orderNo) {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Result.ok(orderDetailService.getDetail(orderNo, userId));
    }

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

    private Long currentUserId() {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
