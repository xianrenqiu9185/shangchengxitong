package com.linli.mall.aftersale.controller;

import com.linli.mall.aftersale.dto.AfterSaleAuditRequest;
import com.linli.mall.aftersale.dto.AfterSaleView;
import com.linli.mall.aftersale.service.AfterSaleService;
import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
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
@RequestMapping("/api/v1/admin/after-sales")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    @RequirePermission("after:sale:audit")
    public Result<PageResult<AfterSaleView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(afterSaleService.listAdmin(status, pageNum, pageSize));
    }

    @PostMapping("/{saleNo}/audit")
    @RequirePermission("after:sale:audit")
    public Result<AfterSaleView> audit(
            @PathVariable String saleNo,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AfterSaleAuditRequest request) {
        return Result.ok(afterSaleService.audit(UserContext.userId(), saleNo, idempotencyKey, request));
    }
}
