package com.linli.mall.user.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.user.dto.MemberStatusRequest;
import com.linli.mall.user.dto.MemberView;
import com.linli.mall.user.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    @RequirePermission("user:member:manage")
    public Result<PageResult<MemberView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(memberService.list(keyword, status, pageNum, pageSize));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("user:member:manage")
    public Result<MemberView> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody MemberStatusRequest request) {
        return Result.ok(memberService.updateStatus(id, request.status()));
    }
}
