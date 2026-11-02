package com.linli.mall.user.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.common.auth.UserContext;
import com.linli.mall.user.dto.AccountCreateRequest;
import com.linli.mall.user.dto.AccountStatusRequest;
import com.linli.mall.user.dto.AccountView;
import com.linli.mall.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService accountService;

    @GetMapping
    @RequirePermission("system:account:manage")
    public Result<PageResult<AccountView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(accountService.list(keyword, pageNum, pageSize));
    }

    @PostMapping
    @RequirePermission("system:account:manage")
    public Result<AccountView> create(@Valid @RequestBody AccountCreateRequest request) {
        return Result.ok(accountService.create(request));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("system:account:manage")
    public Result<AccountView> updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody AccountStatusRequest request) {
        return Result.ok(accountService.updateStatus(id, request.status(), UserContext.userId()));
    }
}
