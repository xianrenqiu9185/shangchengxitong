package com.linli.mall.order.controller;

import com.linli.mall.common.api.ErrorCode;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.exception.BizException;
import com.linli.mall.order.dto.OrderDetailView;
import com.linli.mall.order.service.OrderDetailService;
import com.linli.mall.order.service.OrderManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderManagementService orderManagementService;
    private final OrderDetailService orderDetailService;

    @GetMapping
    @RequirePermission("trade:order:query")
    public Result<PageResult<OrderDetailView>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return Result.ok(orderDetailService.listAdmin(status, keyword, pageNum, pageSize));
    }

    @GetMapping("/{orderNo}")
    @RequirePermission("trade:order:query")
    public Result<OrderDetailView> detail(@PathVariable String orderNo) {
        return Result.ok(orderDetailService.getDetailForAdmin(orderNo));
    }

    @PostMapping("/{orderNo}/ship")
    @RequirePermission("trade:order:ship")
    public Result<Void> ship(@PathVariable String orderNo) {
        orderManagementService.ship(orderNo);
        return Result.ok();
    }

    @PostMapping("/{orderNo}/close")
    @RequirePermission("trade:order:close")
    public Result<Void> close(@PathVariable String orderNo) {
        if (!orderManagementService.closeIfPending(orderNo)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return Result.ok();
    }
}
