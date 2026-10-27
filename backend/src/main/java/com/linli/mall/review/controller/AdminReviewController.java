package com.linli.mall.review.controller;

import com.linli.mall.common.api.PageResult;
import com.linli.mall.common.api.Result;
import com.linli.mall.common.auth.RequirePermission;
import com.linli.mall.review.dto.AdminReviewView;
import com.linli.mall.review.dto.ReviewReplyRequest;
import com.linli.mall.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @RequirePermission("review:goods:reply")
    public Result<PageResult<AdminReviewView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(reviewService.listAdmin(keyword, productId, score, status, pageNum, pageSize));
    }

    @PostMapping("/{id}/reply")
    @RequirePermission("review:goods:reply")
    public Result<AdminReviewView> reply(@PathVariable Long id,
                                         @Valid @RequestBody ReviewReplyRequest request) {
        return Result.ok(reviewService.reply(id, request));
    }
}
